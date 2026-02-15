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
) -> list[Movie]:
    """
    Поиск фильмов в базе по различным критериям.
    Все параметры опциональны и комбинируются через AND.
    """
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()

    query = "SELECT * FROM movies WHERE 1=1"
    params = []

    if title:
        query += " AND (title LIKE ? OR original_title LIKE ?)"
        pattern = f"%{title}%"
        params.extend([pattern, pattern])

    if genre:
        query += " AND genres LIKE ?"
        params.append(f"%{genre}%")

    if min_rating is not None:
        query += " AND rating >= ?"
        params.append(min_rating)

    if max_rating is not None:
        query += " AND rating <= ?"
        params.append(max_rating)

    if description:
        query += " AND overview LIKE ?"
        params.append(f"%{description}%")

    if personal_rating is not None:
        query += " AND personal_rating = ?"
        params.append(personal_rating)

    query += " ORDER BY title"
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
