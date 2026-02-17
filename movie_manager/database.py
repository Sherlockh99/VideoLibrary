"""Модуль работы с базой данных SQLite."""
import sqlite3
from pathlib import Path
from typing import Optional
from dataclasses import dataclass
from config import DB_PATH


@dataclass
class Movie:
    """Модель фильма/сериала в базе данных."""
    id: int
    tmdb_id: int
    media_type: str  # "movie" или "tv"
    title: str
    original_title: str
    genres: str
    rating: float
    overview: str
    release_date: str
    poster_path: Optional[str]
    personal_rating: Optional[int]
    created_at: str


@dataclass
class Storage:
    """Модель хранилища (диск, NAS и т.д.)."""
    id: int
    name: str
    is_available: bool


@dataclass
class File:
    """Файл фильма (имя, размер, привязка к фильму)."""
    id: int
    name: str
    size: int  # байты
    movie_id: int


def init_db():
    """Инициализация базы данных."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS movies (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            tmdb_id INTEGER NOT NULL,
            media_type TEXT NOT NULL DEFAULT 'movie',
            title TEXT NOT NULL,
            original_title TEXT,
            genres TEXT,
            rating REAL,
            overview TEXT,
            release_date TEXT,
            poster_path TEXT,
            personal_rating INTEGER CHECK(personal_rating >= 1 AND personal_rating <= 5),
            created_at TEXT DEFAULT CURRENT_TIMESTAMP
        )
    """)

    # Индексы для быстрого поиска
    cursor.execute(
        "CREATE INDEX IF NOT EXISTS idx_title ON movies(title)"
    )
    cursor.execute(
        "CREATE INDEX IF NOT EXISTS idx_genres ON movies(genres)"
    )
    cursor.execute(
        "CREATE INDEX IF NOT EXISTS idx_overview ON movies(overview)"
    )
    cursor.execute(
        "CREATE INDEX IF NOT EXISTS idx_rating ON movies(rating)"
    )
    cursor.execute(
        "CREATE INDEX IF NOT EXISTS idx_personal_rating ON movies(personal_rating)"
    )

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS storages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            is_available INTEGER NOT NULL DEFAULT 1
        )
    """)

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS files (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            size INTEGER NOT NULL DEFAULT 0,
            movie_id INTEGER NOT NULL,
            FOREIGN KEY (movie_id) REFERENCES movies(id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_files_movie ON files(movie_id)")

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS storage_files (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            storage_id INTEGER NOT NULL,
            file_id INTEGER NOT NULL,
            FOREIGN KEY (storage_id) REFERENCES storages(id),
            FOREIGN KEY (file_id) REFERENCES files(id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_storage_files_storage ON storage_files(storage_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_storage_files_file ON storage_files(file_id)")

    # Миграция: удалить старую таблицу movie_files если была
    cursor.execute("DROP TABLE IF EXISTS movie_files")

    # Миграция: добавить media_type если его нет (существующие записи = фильмы)
    cursor.execute("PRAGMA table_info(movies)")
    columns = [row[1] for row in cursor.fetchall()]
    if "media_type" not in columns:
        cursor.execute("ALTER TABLE movies ADD COLUMN media_type TEXT NOT NULL DEFAULT 'movie'")
        # Удаляем старый UNIQUE на tmdb_id если был, добавляем UNIQUE(tmdb_id, media_type)
        # SQLite не поддерживает DROP CONSTRAINT, пересоздавать таблицу сложно — оставляем как есть
        # (tmdb_id для movie и tv в разных пространствах ID в TMDB)

    conn.commit()
    conn.close()


def add_movie(
    tmdb_id: int,
    title: str,
    original_title: str,
    genres: str,
    rating: float,
    overview: str,
    release_date: str,
    poster_path: Optional[str] = None,
    personal_rating: Optional[int] = None,
    media_type: str = "movie",
) -> int:
    """Добавляет фильм или сериал в базу данных."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute(
        """
        INSERT INTO movies (tmdb_id, media_type, title, original_title, genres, rating, overview, release_date, poster_path, personal_rating)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (tmdb_id, media_type, title, original_title, genres, rating, overview or "", release_date or "", poster_path, personal_rating),
    )
    movie_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return movie_id


def movie_exists(tmdb_id: int, media_type: str = "movie") -> bool:
    """Проверяет, есть ли фильм/сериал уже в базе."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT 1 FROM movies WHERE tmdb_id = ? AND media_type = ?", (tmdb_id, media_type))
    exists = cursor.fetchone() is not None
    conn.close()
    return exists


def update_movie_from_import(
    tmdb_id: int,
    title: str,
    original_title: str,
    genres: str,
    rating: float,
    overview: str,
    release_date: str,
    poster_path: Optional[str],
    personal_rating: Optional[int],
    media_type: str = "movie",
) -> bool:
    """Обновляет фильм по tmdb_id (для импорта с заменой)."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute(
        """
        UPDATE movies SET
            title = ?, original_title = ?, genres = ?, rating = ?,
            overview = ?, release_date = ?, poster_path = ?, personal_rating = ?, media_type = ?
        WHERE tmdb_id = ? AND media_type = ?
        """,
        (title, original_title, genres, rating, overview, release_date, poster_path, personal_rating, media_type, tmdb_id, media_type),
    )
    updated = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return updated


def update_personal_rating(movie_id: int, rating: int) -> bool:
    """Обновляет личную оценку фильма (1-5)."""
    if not 1 <= rating <= 5:
        raise ValueError("Оценка должна быть от 1 до 5")
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("UPDATE movies SET personal_rating = ? WHERE id = ?", (rating, movie_id))
    updated = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return updated


def search_movies(
    title: Optional[str] = None,
    genre: Optional[str] = None,
    min_rating: Optional[float] = None,
    max_rating: Optional[float] = None,
    description: Optional[str] = None,
    personal_rating: Optional[int] = None,
    storage_name: Optional[str] = None,
) -> list[Movie]:
    """
    Поиск фильмов в базе по различным критериям.
    Все параметры опциональны и комбинируются через AND.
    """
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()

    if storage_name:
        query = """
            SELECT DISTINCT m.* FROM movies m
            JOIN files f ON f.movie_id = m.id
            JOIN storage_files sf ON sf.file_id = f.id
            JOIN storages s ON s.id = sf.storage_id AND s.name = ?
            WHERE 1=1
        """
        params = [storage_name]
    else:
        query = "SELECT * FROM movies WHERE 1=1"
        params = []

    if title:
        query += " AND (m.title LIKE ? OR m.original_title LIKE ?)" if storage_name else " AND (title LIKE ? OR original_title LIKE ?)"
        pattern = f"%{title}%"
        params.extend([pattern, pattern])

    if genre:
        col = "m.genres" if storage_name else "genres"
        query += f" AND {col} LIKE ?"
        params.append(f"%{genre}%")

    if min_rating is not None:
        col = "m.rating" if storage_name else "rating"
        query += f" AND {col} >= ?"
        params.append(min_rating)

    if max_rating is not None:
        col = "m.rating" if storage_name else "rating"
        query += f" AND {col} <= ?"
        params.append(max_rating)

    if description:
        col = "m.overview" if storage_name else "overview"
        query += f" AND {col} LIKE ?"
        params.append(f"%{description}%")

    if personal_rating is not None:
        col = "m.personal_rating" if storage_name else "personal_rating"
        query += f" AND {col} = ?"
        params.append(personal_rating)

    query += " ORDER BY m.title" if storage_name else " ORDER BY title"
    cursor.execute(query, params)
    rows = cursor.fetchall()
    conn.close()

    return [
        Movie(
            id=row["id"],
            tmdb_id=row["tmdb_id"],
            media_type=row.get("media_type", "movie") or "movie",
            title=row["title"],
            original_title=row["original_title"] or "",
            genres=row["genres"] or "",
            rating=row["rating"] or 0,
            overview=row["overview"] or "",
            release_date=row["release_date"] or "",
            poster_path=row["poster_path"],
            personal_rating=row["personal_rating"],
            created_at=row["created_at"],
        )
        for row in rows
    ]


def get_all_movies() -> list[Movie]:
    """Возвращает все фильмы из базы."""
    return search_movies()


def get_movie_by_tmdb_id(tmdb_id: int, media_type: str = "movie") -> Optional[Movie]:
    """Получает фильм/сериал по tmdb_id и media_type."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM movies WHERE tmdb_id = ? AND media_type = ?", (tmdb_id, media_type))
    row = cursor.fetchone()
    conn.close()
    if not row:
        return None
    return Movie(
        id=row["id"],
        tmdb_id=row["tmdb_id"],
        media_type=row.get("media_type", "movie") or "movie",
        title=row["title"],
        original_title=row["original_title"] or "",
        genres=row["genres"] or "",
        rating=row["rating"] or 0,
        overview=row["overview"] or "",
        release_date=row["release_date"] or "",
        poster_path=row["poster_path"],
        personal_rating=row["personal_rating"],
        created_at=row["created_at"],
    )


def get_movie_by_id(movie_id: int) -> Optional[Movie]:
    """Получает фильм по ID в базе."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM movies WHERE id = ?", (movie_id,))
    row = cursor.fetchone()
    conn.close()
    if not row:
        return None
    return Movie(
        id=row["id"],
        tmdb_id=row["tmdb_id"],
        media_type=row.get("media_type", "movie") or "movie",
        title=row["title"],
        original_title=row["original_title"] or "",
        genres=row["genres"] or "",
        rating=row["rating"] or 0,
        overview=row["overview"] or "",
        release_date=row["release_date"] or "",
        poster_path=row["poster_path"],
        personal_rating=row["personal_rating"],
        created_at=row["created_at"],
    )


# --- Storages ---


def add_storage(name: str) -> int:
    """Добавляет хранилище."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("INSERT INTO storages (name, is_available) VALUES (?, 1)", (name,))
    storage_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return storage_id


def get_all_storages() -> list[Storage]:
    """Возвращает все хранилища."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM storages ORDER BY name")
    rows = cursor.fetchall()
    conn.close()
    return [
        Storage(id=row["id"], name=row["name"], is_available=bool(row["is_available"]))
        for row in rows
    ]


def get_storage_by_id(storage_id: int) -> Optional[Storage]:
    """Получает хранилище по ID."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM storages WHERE id = ?", (storage_id,))
    row = cursor.fetchone()
    conn.close()
    if not row:
        return None
    return Storage(id=row["id"], name=row["name"], is_available=bool(row["is_available"]))


def get_storage_by_name(name: str) -> Optional[Storage]:
    """Получает хранилище по имени."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM storages WHERE name = ?", (name,))
    row = cursor.fetchone()
    conn.close()
    if not row:
        return None
    return Storage(id=row["id"], name=row["name"], is_available=bool(row["is_available"]))


def remove_storage(storage_id: int) -> bool:
    """Удаляет хранилище и все привязки файлов к нему."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("DELETE FROM storage_files WHERE storage_id = ?", (storage_id,))
    cursor.execute("DELETE FROM storages WHERE id = ?", (storage_id,))
    deleted = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return deleted


# --- Files (файлы с привязкой к фильму) ---


def add_file(movie_id: int, name: str, size: int = 0) -> int:
    """Добавляет файл, привязанный к фильму. Возвращает file_id."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute(
        "INSERT INTO files (name, size, movie_id) VALUES (?, ?, ?)",
        (name, size, movie_id),
    )
    file_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return file_id


def add_file_to_storage(file_id: int, storage_id: int) -> bool:
    """Добавляет файл на хранилище. Возвращает True если добавлено."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute(
        "SELECT 1 FROM storage_files WHERE file_id = ? AND storage_id = ?",
        (file_id, storage_id),
    )
    if cursor.fetchone():
        conn.close()
        return False
    cursor.execute(
        "INSERT INTO storage_files (file_id, storage_id) VALUES (?, ?)",
        (file_id, storage_id),
    )
    conn.commit()
    conn.close()
    return True


def remove_files_by_movie_id(movie_id: int) -> None:
    """Удаляет все файлы фильма и их привязки к хранилищам."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT id FROM files WHERE movie_id = ?", (movie_id,))
    file_ids = [r[0] for r in cursor.fetchall()]
    for fid in file_ids:
        cursor.execute("DELETE FROM storage_files WHERE file_id = ?", (fid,))
    cursor.execute("DELETE FROM files WHERE movie_id = ?", (movie_id,))
    conn.commit()
    conn.close()


def remove_file(file_id: int) -> bool:
    """Удаляет файл и все его привязки к хранилищам."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("DELETE FROM storage_files WHERE file_id = ?", (file_id,))
    cursor.execute("DELETE FROM files WHERE id = ?", (file_id,))
    deleted = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return deleted


def get_files_by_movie_id(movie_id: int) -> list[tuple[File, list[Storage]]]:
    """Возвращает файлы фильма с их хранилищами: (File, [Storage, ...])."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute(
        "SELECT id, name, size, movie_id FROM files WHERE movie_id = ? ORDER BY name",
        (movie_id,),
    )
    file_rows = cursor.fetchall()
    result = []
    for row in file_rows:
        file_id = row["id"]
        cursor.execute(
            """
            SELECT s.id, s.name FROM storage_files sf
            JOIN storages s ON s.id = sf.storage_id
            WHERE sf.file_id = ?
            ORDER BY s.name
            """,
            (file_id,),
        )
        storages = [
            Storage(id=r["id"], name=r["name"], is_available=True)
            for r in cursor.fetchall()
        ]
        result.append(
            (
                File(
                    id=file_id,
                    name=row["name"],
                    size=row["size"],
                    movie_id=row["movie_id"],
                ),
                storages,
            )
        )
    conn.close()
    return result


def get_storage_names_for_movie(movie_id: int) -> list[str]:
    """Возвращает уникальные имена хранилищ, на которых есть файлы фильма."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute(
        """
        SELECT DISTINCT s.name FROM files f
        JOIN storage_files sf ON sf.file_id = f.id
        JOIN storages s ON s.id = sf.storage_id
        WHERE f.movie_id = ?
        ORDER BY s.name
        """,
        (movie_id,),
    )
    names = [r[0] for r in cursor.fetchall()]
    conn.close()
    return names
