"""
Программа для поиска фильмов на TMDb, добавления в базу и поиска в локальной коллекции.

Использование:
    python main.py add          - поиск и добавление фильма
    python main.py search       - поиск в своей коллекции
    python main.py list         - показать все фильмы
    python main.py rate <id> <1-5> - поставить свою оценку
    python main.py export [путь] - выгрузить коллекцию в .vlp
    python main.py import <путь> - загрузить коллекцию из .vlp
"""
import sys
from pathlib import Path

from tmdb_client import TMDbClient
from database import (
    init_db,
    add_movie,
    movie_exists,
    update_personal_rating,
    search_movies,
    get_all_movies,
    get_movie_by_id,
    Movie,
)
from export_import import export_to_file, import_from_file


def format_movie_short(idx: int, m: dict) -> str:
    """Краткое представление фильма из результата поиска TMDb."""
    title = m.get("title", "Без названия")
    year = (m.get("release_date") or "")[:4] or "?"
    rating = m.get("vote_average") or 0
    return f"  {idx}. {title} ({year}) — рейтинг: {rating:.1f}"


def format_movie_db(m: Movie) -> str:
    """Представление фильма из базы данных."""
    lines = [
        f"  [{m.id}] {m.title} ({m.release_date or '?'})",
        f"      Жанры: {m.genres or '-'}",
        f"      TMDb рейтинг: {m.rating:.1f}",
    ]
    if m.personal_rating:
        lines.append(f"      Ваша оценка: {m.personal_rating}/5")
    if m.overview:
        overview = m.overview[:150] + "..." if len(m.overview) > 150 else m.overview
        lines.append(f"      Описание: {overview}")
    return "\n".join(lines)


def cmd_add():
    """Поиск фильма на TMDb и добавление в базу."""
    try:
        client = TMDbClient()
    except ValueError as e:
        print(f"Ошибка: {e}")
        return 1

    query = input("Введите название фильма для поиска: ").strip()
    if not query:
        print("Название не может быть пустым.")
        return 1

    print("\nПоиск на TMDb...")
    data = client.search_movies(query)
    results = data.get("results", [])

    if not results:
        print("Ничего не найдено.")
        return 0

    print("\nНайдено фильмов:", len(results))
    for i, m in enumerate(results[:15], 1):
        print(format_movie_short(i, m))

    try:
        choice = input("\nВведите номер фильма для добавления (или 0 для отмены): ").strip()
        idx = int(choice)
        if idx == 0:
            return 0
        if idx < 1 or idx > len(results):
            print("Неверный номер.")
            return 1
    except ValueError:
        print("Введите число.")
        return 1

    selected = results[idx - 1]
    tmdb_id = selected["id"]

    if movie_exists(tmdb_id):
        print("Этот фильм уже есть в вашей коллекции.")
        return 0

    details = client.get_movie_details(tmdb_id)
    genres = ", ".join(g["name"] for g in details.get("genres", []))

    add_movie(
        tmdb_id=tmdb_id,
        title=details.get("title", selected.get("title", "")),
        original_title=details.get("original_title", "") or "",
        genres=genres,
        rating=float(details.get("vote_average") or 0),
        overview=details.get("overview") or "",
        release_date=details.get("release_date") or "",
        poster_path=details.get("poster_path"),
    )

    print(f'\nФильм "{details.get("title")}" добавлен в коллекцию.')
    return 0


def cmd_search():
    """Поиск фильмов в своей коллекции."""
    print("Поиск в коллекции (пустое поле = не учитывать)")
    title = input("Название: ").strip() or None
    genre = input("Жанр: ").strip() or None

    min_rating = None
    max_rating = None
    try:
        r = input("Мин. рейтинг TMDb (1-10, пусто = любой): ").strip()
        if r:
            min_rating = float(r)
        r = input("Макс. рейтинг TMDb (пусто = любой): ").strip()
        if r:
            max_rating = float(r)
    except ValueError:
        print("Ошибка в рейтинге.")
        return 1

    description = input("Слово в описании: ").strip() or None

    personal = None
    try:
        p = input("Ваша оценка (1-5, пусто = любая): ").strip()
        if p:
            personal = int(p)
            if not 1 <= personal <= 5:
                print("Оценка должна быть от 1 до 5.")
                return 1
    except ValueError:
        print("Ошибка в оценке.")
        return 1

    movies = search_movies(
        title=title,
        genre=genre,
        min_rating=min_rating,
        max_rating=max_rating,
        description=description,
        personal_rating=personal,
    )

    print(f"\nНайдено: {len(movies)} фильм(ов)")
    for m in movies:
        print(format_movie_db(m))
    return 0


def cmd_list():
    """Показать все фильмы в коллекции."""
    movies = get_all_movies()
    if not movies:
        print("Коллекция пуста. Добавьте фильмы командой: python main.py add")
        return 0

    print(f"\nВсего фильмов: {len(movies)}\n")
    for m in movies:
        print(format_movie_db(m))
        print()
    return 0


def cmd_rate():
    """Поставить свою оценку фильму."""
    if len(sys.argv) < 4:
        print("Использование: python main.py rate <id> <1-5>")
        return 1

    try:
        movie_id = int(sys.argv[2])
        rating = int(sys.argv[3])
    except ValueError:
        print("ID и оценка должны быть числами.")
        return 1

    if not 1 <= rating <= 5:
        print("Оценка должна быть от 1 до 5.")
        return 1

    if update_personal_rating(movie_id, rating):
        print("Оценка обновлена.")
        movie = get_movie_by_id(movie_id)
        if movie:
            print(format_movie_db(movie))
    else:
        print("Фильм с таким ID не найден.")
    return 0


def cmd_export() -> int:
    """Выгрузка коллекции в .vlp файл."""
    path = sys.argv[2] if len(sys.argv) > 2 else "videolibrary_export.vlp"
    path = Path(path).resolve()
    try:
        count = export_to_file(path, source="desktop")
        print(f"Экспортировано {count} фильм(ов) в {path}")
    except Exception as e:
        print(f"Ошибка экспорта: {e}")
        return 1
    return 0


def cmd_import() -> int:
    """Загрузка коллекции из .vlp файла."""
    if len(sys.argv) < 3:
        print("Использование: python main.py import <путь_к_файлу.vlp> [--replace]")
        return 1
    path = Path(sys.argv[2])
    replace = "--replace" in sys.argv
    try:
        added, skipped = import_from_file(path, replace_duplicates=replace)
        print(f"Добавлено: {added}, пропущено дубликатов: {skipped}")
    except FileNotFoundError as e:
        print(f"Ошибка: {e}")
        return 1
    except ValueError as e:
        print(f"Ошибка формата: {e}")
        return 1
    return 0


def main():
    init_db()

    if len(sys.argv) < 2:
        print(__doc__)
        return 0

    cmd = sys.argv[1].lower()

    if cmd == "add":
        return cmd_add()
    elif cmd == "search":
        return cmd_search()
    elif cmd == "list":
        return cmd_list()
    elif cmd == "rate":
        return cmd_rate()
    elif cmd == "export":
        return cmd_export()
    elif cmd == "import":
        return cmd_import()
    else:
        print(f"Неизвестная команда: {cmd}")
        print(__doc__)
        return 1


if __name__ == "__main__":
    sys.exit(main())
