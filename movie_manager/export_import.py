"""
Экспорт и импорт коллекции в универсальном формате .vlp (Video Library Package).

Формат совместим с мобильным приложением — см. export_format.md
"""
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

from database import (
    add_movie,
    movie_exists,
    get_all_movies,
    get_all_storages,
    get_movie_by_tmdb_id,
    get_storage_by_name,
    get_files_by_movie_id,
    add_storage,
    add_file,
    add_file_to_storage,
    remove_files_by_movie_id,
    update_movie_from_import,
)

FORMAT_ID = "videolibrary"
FORMAT_VERSION = 1


def export_to_file(path: Path | str, source: str = "desktop") -> int:
    """
    Экспортирует коллекцию в файл .vlp (JSON).
    Возвращает количество экспортированных фильмов.
    """
    path = Path(path)
    if path.suffix.lower() != ".vlp":
        path = path.with_suffix(".vlp")

    movies = get_all_movies()
    storages = get_all_storages()

    data = {
        "format": FORMAT_ID,
        "version": FORMAT_VERSION,
        "exported_at": datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"),
        "source": source,
        "storages": [{"id": s.id, "name": s.name} for s in storages],
        "movies": [
            {
                "tmdb_id": m.tmdb_id,
                "title": m.title,
                "original_title": m.original_title,
                "genres": m.genres,
                "rating": m.rating,
                "overview": m.overview,
                "release_date": m.release_date,
                "poster_path": m.poster_path,
                "personal_rating": m.personal_rating,
                "files": [
                    {"name": f.name, "size": f.size, "storage_names": [s.name for s in storages]}
                    for f, storages in get_files_by_movie_id(m.id)
                ],
            }
            for m in movies
        ],
    }

    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    return len(movies)


def import_from_file(path: Path | str, replace_duplicates: bool = False) -> tuple[int, int]:
    """
    Импортирует коллекцию из файла .vlp.
    Возвращает (добавлено, пропущено_дубликатов).
    """
    path = Path(path)
    if not path.exists():
        raise FileNotFoundError(f"Файл не найден: {path}")

    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    if data.get("format") != FORMAT_ID:
        raise ValueError(f"Неверный формат файла. Ожидается format='{FORMAT_ID}'")

    version = data.get("version", 0)
    if version > FORMAT_VERSION:
        raise ValueError(f"Версия формата {version} не поддерживается. Макс: {FORMAT_VERSION}")

    # Создаём хранилища, если есть в файле
    storages_data = data.get("storages", [])
    for s in storages_data:
        name = s.get("name")
        if name and get_storage_by_name(name) is None:
            add_storage(name)

    movies_data = data.get("movies", [])
    added = 0
    skipped = 0

    for m in movies_data:
        tmdb_id = m.get("tmdb_id")
        if tmdb_id is None:
            continue

        movie_data = {
            "tmdb_id": int(tmdb_id),
            "title": m.get("title", ""),
            "original_title": m.get("original_title", ""),
            "genres": m.get("genres", ""),
            "rating": float(m.get("rating") or 0),
            "overview": m.get("overview", ""),
            "release_date": m.get("release_date", ""),
            "poster_path": m.get("poster_path"),
            "personal_rating": m.get("personal_rating"),
        }

        files_data = m.get("files", [])

        if movie_exists(tmdb_id):
            if replace_duplicates:
                update_movie_from_import(
                    tmdb_id=movie_data["tmdb_id"],
                    title=movie_data["title"],
                    original_title=movie_data["original_title"],
                    genres=movie_data["genres"],
                    rating=movie_data["rating"],
                    overview=movie_data["overview"],
                    release_date=movie_data["release_date"],
                    poster_path=movie_data["poster_path"],
                    personal_rating=movie_data["personal_rating"],
                )
                movie = get_movie_by_tmdb_id(tmdb_id)
                if movie:
                    remove_files_by_movie_id(movie.id)
                    for file_item in files_data:
                        name = file_item.get("name") or "—"
                        size = int(file_item.get("size", 0))
                        storage_names = file_item.get("storage_names", [])
                        file_id = add_file(movie.id, name, size)
                        for sn in storage_names:
                            storage = get_storage_by_name(sn)
                            if storage:
                                add_file_to_storage(file_id, storage.id)
                added += 1
            else:
                skipped += 1
            continue

        movie_id = add_movie(
            tmdb_id=movie_data["tmdb_id"],
            title=movie_data["title"],
            original_title=movie_data["original_title"],
            genres=movie_data["genres"],
            rating=movie_data["rating"],
            overview=movie_data["overview"],
            release_date=movie_data["release_date"],
            poster_path=movie_data["poster_path"],
            personal_rating=movie_data["personal_rating"],
        )
        for file_item in files_data:
            name = file_item.get("name") or "—"
            size = int(file_item.get("size", 0))
            storage_names = file_item.get("storage_names", [])
            file_id = add_file(movie_id, name, size)
            for sn in storage_names:
                storage = get_storage_by_name(sn)
                if storage:
                    add_file_to_storage(file_id, storage.id)
        added += 1

    return added, skipped
