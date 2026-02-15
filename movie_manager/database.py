"""Модуль работы с базой данных SQLite."""
import sqlite3
from pathlib import Path
from typing import Optional
from dataclasses import dataclass
from config import DB_PATH


@dataclass
class Movie:
    """Модель фильма в базе данных."""
    id: int
    tmdb_id: int
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
class MovieFile:
    """Привязка фильма к хранилищу (файл фильма на этом хранилище)."""
    id: int
    movie_id: int
    storage_id: int
    created_at: str


def init_db():
    """Инициализация базы данных."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS movies (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            tmdb_id INTEGER UNIQUE NOT NULL,
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
        CREATE TABLE IF NOT EXISTS movie_files (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            movie_id INTEGER NOT NULL,
            storage_id INTEGER NOT NULL,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (movie_id) REFERENCES movies(id),
            FOREIGN KEY (storage_id) REFERENCES storages(id)
        )
    """)

    cursor.execute("CREATE INDEX IF NOT EXISTS idx_movie_files_movie ON movie_files(movie_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_movie_files_storage ON movie_files(storage_id)")

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
) -> int:
    """Добавляет фильм в базу данных."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute(
        """
        INSERT INTO movies (tmdb_id, title, original_title, genres, rating, overview, release_date, poster_path, personal_rating)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (tmdb_id, title, original_title, genres, rating, overview or "", release_date or "", poster_path, personal_rating),
    )
    movie_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return movie_id


def movie_exists(tmdb_id: int) -> bool:
    """Проверяет, есть ли фильм уже в базе."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT 1 FROM movies WHERE tmdb_id = ?", (tmdb_id,))
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
) -> bool:
    """Обновляет фильм по tmdb_id (для импорта с заменой)."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute(
        """
        UPDATE movies SET
            title = ?, original_title = ?, genres = ?, rating = ?,
            overview = ?, release_date = ?, poster_path = ?, personal_rating = ?
        WHERE tmdb_id = ?
        """,
        (title, original_title, genres, rating, overview, release_date, poster_path, personal_rating, tmdb_id),
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
            JOIN movie_files mf ON mf.movie_id = m.id
            JOIN storages s ON s.id = mf.storage_id AND s.name = ?
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


def get_movie_by_tmdb_id(tmdb_id: int) -> Optional[Movie]:
    """Получает фильм по tmdb_id."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM movies WHERE tmdb_id = ?", (tmdb_id,))
    row = cursor.fetchone()
    conn.close()
    if not row:
        return None
    return Movie(
        id=row["id"],
        tmdb_id=row["tmdb_id"],
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
    """Удаляет хранилище и все привязки film->storage."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("DELETE FROM movie_files WHERE storage_id = ?", (storage_id,))
    cursor.execute("DELETE FROM storages WHERE id = ?", (storage_id,))
    deleted = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return deleted


# --- Movie files (привязки фильм -> хранилище) ---


def add_movie_file(movie_id: int, storage_id: int) -> Optional[int]:
    """Добавляет привязку: у фильма есть файл на этом хранилище."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute(
        "SELECT 1 FROM movie_files WHERE movie_id = ? AND storage_id = ?",
        (movie_id, storage_id),
    )
    if cursor.fetchone():
        conn.close()
        return None
    cursor.execute(
        "INSERT INTO movie_files (movie_id, storage_id) VALUES (?, ?)",
        (movie_id, storage_id),
    )
    mf_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return mf_id


def remove_movie_files_by_movie_id(movie_id: int) -> None:
    """Удаляет все привязки фильма к хранилищам."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("DELETE FROM movie_files WHERE movie_id = ?", (movie_id,))
    conn.commit()
    conn.close()


def remove_movie_file(movie_file_id: int) -> bool:
    """Удаляет привязку по ID movie_files."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("DELETE FROM movie_files WHERE id = ?", (movie_file_id,))
    deleted = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return deleted


def get_movie_files_by_movie_id(movie_id: int) -> list[tuple[MovieFile, Storage]]:
    """Возвращает привязки фильма к хранилищам (MovieFile + Storage)."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute(
        """
        SELECT mf.id, mf.movie_id, mf.storage_id, mf.created_at,
               s.name as storage_name
        FROM movie_files mf
        JOIN storages s ON s.id = mf.storage_id
        WHERE mf.movie_id = ?
        ORDER BY s.name
        """,
        (movie_id,),
    )
    rows = cursor.fetchall()
    conn.close()
    return [
        (
            MovieFile(
                id=row["id"],
                movie_id=row["movie_id"],
                storage_id=row["storage_id"],
                created_at=row["created_at"] or "",
            ),
            Storage(id=row["storage_id"], name=row["storage_name"], is_available=True),
        )
        for row in rows
    ]


def get_storage_names_for_movie(movie_id: int) -> list[str]:
    """Возвращает список имён хранилищ, на которых есть файл фильма."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute(
        """
        SELECT s.name FROM movie_files mf
        JOIN storages s ON s.id = mf.storage_id
        WHERE mf.movie_id = ?
        ORDER BY s.name
        """,
        (movie_id,),
    )
    names = [r[0] for r in cursor.fetchall()]
    conn.close()
    return names
