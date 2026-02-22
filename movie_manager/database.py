"""Модуль работы с базой данных SQLite."""
import sqlite3
from pathlib import Path
from typing import Optional
from dataclasses import dataclass
from config import DB_PATH


@dataclass
class Category:
    """Модель категории (плейлист фильмов)."""
    id: int
    name: str


@dataclass
class Actor:
    """Модель актёра."""
    id: int
    tmdb_person_id: int
    name: str


@dataclass
class Genre:
    """Модель жанра."""
    id: int
    tmdb_genre_id: Optional[int]
    name: str


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

    # Категории (плейлисты)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE
        )
    """)

    # Связь фильмов и категорий
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS movie_categories (
            movie_id INTEGER NOT NULL,
            category_id INTEGER NOT NULL,
            PRIMARY KEY (movie_id, category_id),
            FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
            FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_movie_categories_movie ON movie_categories(movie_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_movie_categories_category ON movie_categories(category_id)")

    # Актёры
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS actors (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            tmdb_person_id INTEGER NOT NULL UNIQUE,
            name TEXT NOT NULL
        )
    """)

    # Связь фильмов и актёров (с порядком в титрах)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS movie_actors (
            movie_id INTEGER NOT NULL,
            actor_id INTEGER NOT NULL,
            credit_order INTEGER,
            PRIMARY KEY (movie_id, actor_id),
            FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
            FOREIGN KEY (actor_id) REFERENCES actors(id) ON DELETE CASCADE
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_movie_actors_movie ON movie_actors(movie_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_movie_actors_actor ON movie_actors(actor_id)")

    # Жанры (таблица, не строка)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS genres (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            tmdb_genre_id INTEGER,
            name TEXT NOT NULL
        )
    """)

    # Связь фильмов и жанров
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS movie_genres (
            movie_id INTEGER NOT NULL,
            genre_id INTEGER NOT NULL,
            PRIMARY KEY (movie_id, genre_id),
            FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
            FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_movie_genres_movie ON movie_genres(movie_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_movie_genres_genre ON movie_genres(genre_id)")

    # Миграция: для фильмов с genres но без записей в movie_genres — заполнить из строки
    cursor.execute(
        """
        SELECT m.id, m.genres FROM movies m
        WHERE m.genres IS NOT NULL AND m.genres != ''
        AND NOT EXISTS (SELECT 1 FROM movie_genres mg WHERE mg.movie_id = m.id)
        """
    )
    for mid, gstr in cursor.fetchall():
        if gstr:
            for name in (n.strip() for n in gstr.split(",") if n.strip()):
                cursor.execute("SELECT id FROM genres WHERE name = ?", (name,))
                row = cursor.fetchone()
                if row:
                    gid = row[0]
                else:
                    cursor.execute("INSERT INTO genres (tmdb_genre_id, name) VALUES (NULL, ?)", (name,))
                    gid = cursor.lastrowid
                cursor.execute("INSERT OR IGNORE INTO movie_genres (movie_id, genre_id) VALUES (?, ?)", (mid, gid))

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


def set_movie_genres_from_tmdb(movie_id: int, tmdb_genres: list[dict]) -> None:
    """Связывает фильм с жанрами по данным TMDb. tmdb_genres: [{"id": N, "name": "..."}, ...]"""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("DELETE FROM movie_genres WHERE movie_id = ?", (movie_id,))
    for g in tmdb_genres:
        tg_id = g.get("id")
        name = g.get("name", "")
        if not name:
            continue
        cursor.execute("SELECT id FROM genres WHERE tmdb_genre_id = ?", (tg_id,))
        row = cursor.fetchone()
        if row:
            genre_id = row[0]
        else:
            cursor.execute("INSERT INTO genres (tmdb_genre_id, name) VALUES (?, ?)", (tg_id, name))
            genre_id = cursor.lastrowid
        cursor.execute("INSERT OR IGNORE INTO movie_genres (movie_id, genre_id) VALUES (?, ?)", (movie_id, genre_id))
    conn.commit()
    conn.close()


def set_movie_genres_from_string(movie_id: int, genres_str: str) -> None:
    """Связывает фильм с жанрами по строке имён (для импорта)."""
    if not genres_str or not genres_str.strip():
        return
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("DELETE FROM movie_genres WHERE movie_id = ?", (movie_id,))
    for name in (n.strip() for n in genres_str.split(",") if n.strip()):
        cursor.execute("SELECT id FROM genres WHERE name = ?", (name,))
        row = cursor.fetchone()
        if row:
            genre_id = row[0]
        else:
            cursor.execute("INSERT INTO genres (tmdb_genre_id, name) VALUES (NULL, ?)", (name,))
            genre_id = cursor.lastrowid
        cursor.execute("INSERT OR IGNORE INTO movie_genres (movie_id, genre_id) VALUES (?, ?)", (movie_id, genre_id))
    conn.commit()
    conn.close()


def set_movie_actors(movie_id: int, actors_data: list[tuple[int, str, int]]) -> None:
    """Связывает фильм с актёрами. actors_data: [(tmdb_person_id, name, credit_order), ...]"""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("DELETE FROM movie_actors WHERE movie_id = ?", (movie_id,))
    for tmdb_pid, name, order_val in actors_data:
        cursor.execute("SELECT id FROM actors WHERE tmdb_person_id = ?", (tmdb_pid,))
        row = cursor.fetchone()
        if row:
            actor_id = row[0]
        else:
            cursor.execute("INSERT INTO actors (tmdb_person_id, name) VALUES (?, ?)", (tmdb_pid, name))
            actor_id = cursor.lastrowid
        cursor.execute(
            "INSERT OR IGNORE INTO movie_actors (movie_id, actor_id, credit_order) VALUES (?, ?, ?)",
            (movie_id, actor_id, order_val),
        )
    conn.commit()
    conn.close()


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
    category_id: Optional[int] = None,
    actor_id: Optional[int] = None,
    genre_id: Optional[int] = None,
) -> list[Movie]:
    """
    Поиск фильмов в базе по различным критериям.
    Все параметры опциональны и комбинируются через AND.
    """
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()

    joins = []
    if storage_name:
        joins.append("JOIN files f ON f.movie_id = m.id")
        joins.append("JOIN storage_files sf ON sf.file_id = f.id")
        joins.append("JOIN storages s ON s.id = sf.storage_id AND s.name = ?")
    if category_id is not None:
        joins.append("JOIN movie_categories mc ON mc.movie_id = m.id AND mc.category_id = ?")
    if actor_id is not None:
        joins.append("JOIN movie_actors ma ON ma.movie_id = m.id AND ma.actor_id = ?")
    if genre_id is not None:
        joins.append("JOIN movie_genres mg ON mg.movie_id = m.id AND mg.genre_id = ?")

    if joins:
        join_str = " " + " ".join(joins)
        query = f"SELECT DISTINCT m.* FROM movies m{join_str} WHERE 1=1"
        params = []
        if storage_name:
            params.append(storage_name)
        if category_id is not None:
            params.append(category_id)
        if actor_id is not None:
            params.append(actor_id)
        if genre_id is not None:
            params.append(genre_id)
    else:
        query = "SELECT * FROM movies WHERE 1=1"
        params = []

    tbl = "m" if joins else ""
    col_prefix = f"{tbl}." if tbl else ""
    if title:
        query += f" AND ({col_prefix}title LIKE ? OR {col_prefix}original_title LIKE ?)"
        pattern = f"%{title}%"
        params.extend([pattern, pattern])

    if genre:
        query += f" AND {col_prefix}genres LIKE ?"
        params.append(f"%{genre}%")

    if min_rating is not None:
        query += f" AND {col_prefix}rating >= ?"
        params.append(min_rating)

    if max_rating is not None:
        query += f" AND {col_prefix}rating <= ?"
        params.append(max_rating)

    if description:
        query += f" AND {col_prefix}overview LIKE ?"
        params.append(f"%{description}%")

    if personal_rating is not None:
        query += f" AND {col_prefix}personal_rating = ?"
        params.append(personal_rating)

    query += f" ORDER BY {col_prefix}title"
    cursor.execute(query, params)
    rows = cursor.fetchall()
    conn.close()

    return [_row_to_movie(row) for row in rows]


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
    return _row_to_movie(row) if row else None


def get_movie_by_id(movie_id: int) -> Optional[Movie]:
    """Получает фильм по ID в базе."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM movies WHERE id = ?", (movie_id,))
    row = cursor.fetchone()
    conn.close()
    return _row_to_movie(row) if row else None


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


def remove_movie(movie_id: int) -> bool:
    """Удаляет фильм из коллекции. Только если у него нет файлов."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT 1 FROM files WHERE movie_id = ?", (movie_id,))
    if cursor.fetchone():
        conn.close()
        return False
    cursor.execute("DELETE FROM movie_categories WHERE movie_id = ?", (movie_id,))
    cursor.execute("DELETE FROM movie_actors WHERE movie_id = ?", (movie_id,))
    cursor.execute("DELETE FROM movie_genres WHERE movie_id = ?", (movie_id,))
    cursor.execute("DELETE FROM movies WHERE id = ?", (movie_id,))
    deleted = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return deleted


# --- Categories ---


def add_category(name: str) -> int:
    """Добавляет категорию. Если уже есть с таким именем — возвращает её id."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT id FROM categories WHERE name = ?", (name.strip(),))
    row = cursor.fetchone()
    if row:
        conn.close()
        return row[0]
    cursor.execute("INSERT INTO categories (name) VALUES (?)", (name.strip(),))
    cid = cursor.lastrowid
    conn.commit()
    conn.close()
    return cid


def get_all_categories() -> list[Category]:
    """Возвращает все категории."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM categories ORDER BY name")
    rows = cursor.fetchall()
    conn.close()
    return [Category(id=row["id"], name=row["name"]) for row in rows]


def get_category_by_id(category_id: int) -> Optional[Category]:
    """Возвращает категорию по id."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM categories WHERE id = ?", (category_id,))
    row = cursor.fetchone()
    conn.close()
    return Category(id=row["id"], name=row["name"]) if row is not None else None


def get_category_by_name(name: str) -> Optional[Category]:
    """Возвращает категорию по имени."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM categories WHERE name = ?", (name.strip(),))
    row = cursor.fetchone()
    conn.close()
    return Category(id=row["id"], name=row["name"]) if row is not None else None


def remove_category(category_id: int) -> bool:
    """Удаляет категорию. Только если к ней не привязаны фильмы."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT 1 FROM movie_categories WHERE category_id = ?", (category_id,))
    if cursor.fetchone():
        conn.close()
        return False
    cursor.execute("DELETE FROM categories WHERE id = ?", (category_id,))
    deleted = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return deleted


def get_categories_by_movie_id(movie_id: int) -> list[Category]:
    """Возвращает категории фильма."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute(
        "SELECT c.id, c.name FROM categories c JOIN movie_categories mc ON c.id = mc.category_id WHERE mc.movie_id = ? ORDER BY c.name",
        (movie_id,),
    )
    rows = cursor.fetchall()
    conn.close()
    return [Category(id=row["id"], name=row["name"]) for row in rows]


def get_category_ids_by_movie_id(movie_id: int) -> list[int]:
    """Возвращает id категорий фильма."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT category_id FROM movie_categories WHERE movie_id = ?", (movie_id,))
    ids = [r[0] for r in cursor.fetchall()]
    conn.close()
    return ids


def set_movie_categories(movie_id: int, category_ids: list[int]) -> None:
    """Устанавливает категории фильма (заменяет текущие)."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("DELETE FROM movie_categories WHERE movie_id = ?", (movie_id,))
    for cid in category_ids:
        cursor.execute("INSERT OR IGNORE INTO movie_categories (movie_id, category_id) VALUES (?, ?)", (movie_id, cid))
    conn.commit()
    conn.close()


def get_movie_count_by_category_id(category_id: int) -> int:
    """Количество фильмов в категории."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT COUNT(*) FROM movie_categories WHERE category_id = ?", (category_id,))
    count = cursor.fetchone()[0]
    conn.close()
    return count


def get_movies_by_category_id(category_id: int) -> list[Movie]:
    """Возвращает фильмы категории."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute(
        "SELECT m.* FROM movies m JOIN movie_categories mc ON m.id = mc.movie_id WHERE mc.category_id = ? ORDER BY m.title",
        (category_id,),
    )
    rows = cursor.fetchall()
    conn.close()
    return [_row_to_movie(row) for row in rows]


# --- Actors ---


def get_all_actors_with_counts() -> list[tuple[Actor, int]]:
    """Возвращает всех актёров с количеством фильмов."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute(
        """
        SELECT a.id, a.tmdb_person_id, a.name, COUNT(ma.movie_id) as cnt
        FROM actors a
        LEFT JOIN movie_actors ma ON a.id = ma.actor_id
        GROUP BY a.id
        ORDER BY cnt DESC, a.name
        """
    )
    rows = cursor.fetchall()
    conn.close()
    return [(Actor(id=r["id"], tmdb_person_id=r["tmdb_person_id"], name=r["name"]), r["cnt"]) for r in rows]


def get_actor_by_id(actor_id: int) -> Optional[Actor]:
    """Возвращает актёра по id."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM actors WHERE id = ?", (actor_id,))
    row = cursor.fetchone()
    conn.close()
    return Actor(id=row["id"], tmdb_person_id=row["tmdb_person_id"], name=row["name"]) if row else None


def get_movies_by_actor_id(actor_id: int) -> list[Movie]:
    """Возвращает фильмы актёра."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute(
        """
        SELECT m.* FROM movies m
        JOIN movie_actors ma ON m.id = ma.movie_id
        WHERE ma.actor_id = ?
        ORDER BY ma.credit_order IS NULL, ma.credit_order ASC, m.title
        """,
        (actor_id,),
    )
    rows = cursor.fetchall()
    conn.close()
    return [_row_to_movie(row) for row in rows]


def get_actors_by_movie_id(movie_id: int) -> list[Actor]:
    """Возвращает актёров фильма (по порядку в титрах)."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute(
        """
        SELECT a.id, a.tmdb_person_id, a.name FROM actors a
        JOIN movie_actors ma ON a.id = ma.actor_id
        WHERE ma.movie_id = ?
        ORDER BY ma.credit_order IS NULL, ma.credit_order ASC, a.name
        """,
        (movie_id,),
    )
    rows = cursor.fetchall()
    conn.close()
    return [Actor(id=r["id"], tmdb_person_id=r["tmdb_person_id"], name=r["name"]) for r in rows]


# --- Genres ---


def get_all_genres_with_counts() -> list[tuple[Genre, int]]:
    """Возвращает все жанры с количеством фильмов."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute(
        """
        SELECT g.id, g.tmdb_genre_id, g.name, COUNT(mg.movie_id) as cnt
        FROM genres g
        LEFT JOIN movie_genres mg ON g.id = mg.genre_id
        GROUP BY g.id
        ORDER BY cnt DESC, g.name
        """
    )
    rows = cursor.fetchall()
    conn.close()
    return [(Genre(id=r["id"], tmdb_genre_id=r["tmdb_genre_id"], name=r["name"]), r["cnt"]) for r in rows]


def get_genre_by_id(genre_id: int) -> Optional[Genre]:
    """Возвращает жанр по id."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM genres WHERE id = ?", (genre_id,))
    row = cursor.fetchone()
    conn.close()
    return Genre(id=row["id"], tmdb_genre_id=row["tmdb_genre_id"], name=row["name"]) if row else None


def get_movies_by_genre_id(genre_id: int) -> list[Movie]:
    """Возвращает фильмы жанра."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute(
        "SELECT m.* FROM movies m JOIN movie_genres mg ON m.id = mg.movie_id WHERE mg.genre_id = ? ORDER BY m.title",
        (genre_id,),
    )
    rows = cursor.fetchall()
    conn.close()
    return [_row_to_movie(row) for row in rows]


def get_genres_by_movie_id(movie_id: int) -> list[Genre]:
    """Возвращает жанры фильма (из таблицы genres)."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute(
        "SELECT g.id, g.tmdb_genre_id, g.name FROM genres g JOIN movie_genres mg ON g.id = mg.genre_id WHERE mg.movie_id = ? ORDER BY g.name",
        (movie_id,),
    )
    rows = cursor.fetchall()
    conn.close()
    return [Genre(id=r["id"], tmdb_genre_id=r["tmdb_genre_id"], name=r["name"]) for r in rows]


def _row_to_movie(row) -> Movie:
    """Преобразует sqlite3.Row в Movie."""
    r = {k: row[k] for k in row.keys()} if hasattr(row, "keys") else row
    return Movie(
        id=r["id"],
        tmdb_id=r["tmdb_id"],
        media_type=r.get("media_type") or "movie",
        title=r["title"],
        original_title=r.get("original_title") or "",
        genres=r.get("genres") or "",
        rating=r.get("rating") or 0,
        overview=r.get("overview") or "",
        release_date=r.get("release_date") or "",
        poster_path=r.get("poster_path"),
        personal_rating=r.get("personal_rating"),
        created_at=r["created_at"],
    )


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
