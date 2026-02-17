"""
Программа для поиска фильмов на TMDb, добавления в базу и поиска в локальной коллекции.

Использование:
    python main.py add [--tv]   - поиск и добавление фильма (--tv для сериала)
    python main.py search       - поиск в своей коллекции
    python main.py list         - показать все фильмы
    python main.py rate <id> <1-5> - поставить свою оценку
    python main.py storage add <имя> - добавить хранилище
    python main.py storage list     - список хранилищ
    python main.py storage remove <id> - удалить хранилище
    python main.py file add <movie_id> <имя> [размер] [storage_id/имя...] - добавить файл
    python main.py file remove <file_id> - удалить файл
    python main.py file list <movie_id> - файлы фильма
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
    get_all_storages,
    get_storage_by_id,
    get_storage_by_name,
    add_storage,
    remove_storage,
    add_file,
    add_file_to_storage,
    remove_file,
    get_files_by_movie_id,
    get_storage_names_for_movie,
    Movie,
    File,
)
from export_import import export_to_file, import_from_file


def format_movie_short(idx: int, m: dict) -> str:
    """Краткое представление фильма из результата поиска TMDb."""
    title = m.get("title") or m.get("name") or "Без названия"
    year = (m.get("release_date") or m.get("first_air_date") or "")[:4] or "?"
    rating = m.get("vote_average") or 0
    return f"  {idx}. {title} ({year}) — рейтинг: {rating:.1f}"


def format_movie_db(m: Movie, storage_names: list[str] | None = None) -> str:
    """Представление фильма/сериала из базы данных."""
    if storage_names is None:
        storage_names = get_storage_names_for_movie(m.id)
    type_label = " (сериал)" if m.media_type == "tv" else ""
    lines = [
        f"  [{m.id}] {m.title}{type_label} ({m.release_date or '?'})",
        f"      Жанры: {m.genres or '-'}",
        f"      TMDb рейтинг: {m.rating:.1f}",
    ]
    if m.personal_rating:
        lines.append(f"      Ваша оценка: {m.personal_rating}/5")
    if storage_names:
        lines.append(f"      Файлы на: {', '.join(storage_names)}")
    if m.overview:
        overview = m.overview[:150] + "..." if len(m.overview) > 150 else m.overview
        lines.append(f"      Описание: {overview}")
    return "\n".join(lines)


def cmd_add():
    """Поиск фильма или сериала на TMDb и добавление в базу."""
    add_tv = "--tv" in sys.argv
    if add_tv:
        sys.argv = [a for a in sys.argv if a != "--tv"]

    try:
        client = TMDbClient()
    except ValueError as e:
        print(f"Ошибка: {e}")
        return 1

    label = "сериала" if add_tv else "фильма"
    query = input(f"Введите название {label} для поиска: ").strip()
    if not query:
        print("Название не может быть пустым.")
        return 1

    print("\nПоиск на TMDb...")
    if add_tv:
        data = client.search_tv(query)
    else:
        data = client.search_movies(query)
    results = data.get("results", [])

    if not results:
        print("Ничего не найдено.")
        return 0

    print(f"\nНайдено: {len(results)}")
    for i, m in enumerate(results[:15], 1):
        print(format_movie_short(i, m))

    try:
        choice = input(f"\nВведите номер для добавления (или 0 для отмены): ").strip()
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
    media_type = "tv" if add_tv else "movie"

    if movie_exists(tmdb_id, media_type):
        print(f"Этот {label[:-1]} уже есть в вашей коллекции.")
        return 0

    if add_tv:
        details = client.get_tv_details(tmdb_id)
        genres = ", ".join(g["name"] for g in details.get("genres", []))
        title = details.get("name", selected.get("name", ""))
        original = details.get("original_name", "") or ""
        release = details.get("first_air_date") or ""
    else:
        details = client.get_movie_details(tmdb_id)
        genres = ", ".join(g["name"] for g in details.get("genres", []))
        title = details.get("title", selected.get("title", ""))
        original = details.get("original_title", "") or ""
        release = details.get("release_date") or ""

    add_movie(
        tmdb_id=tmdb_id,
        media_type=media_type,
        title=title,
        original_title=original,
        genres=genres,
        rating=float(details.get("vote_average") or 0),
        overview=details.get("overview") or "",
        release_date=release,
        poster_path=details.get("poster_path"),
    )

    print(f'\n{label.capitalize()} "{title}" добавлен в коллекцию.')
    return 0


def _parse_storage_arg() -> str | None:
    """Парсит --storage <имя> из sys.argv."""
    if "--storage" not in sys.argv:
        return None
    idx = sys.argv.index("--storage")
    if idx + 1 >= len(sys.argv):
        return None
    return sys.argv[idx + 1].strip() or None


def cmd_search():
    """Поиск фильмов в своей коллекции."""
    storage_name = _parse_storage_arg()
    if storage_name:
        print(f"Фильтр по хранилищу: {storage_name}")

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
        storage_name=storage_name,
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


def cmd_storage() -> int:
    """Управление хранилищами: add, list, remove."""
    if len(sys.argv) < 3:
        print("Использование: python main.py storage <add|list|remove> [аргументы]")
        return 1

    sub = sys.argv[2].lower()
    if sub == "add":
        if len(sys.argv) < 4:
            print("Использование: python main.py storage add <имя>")
            return 1
        name = sys.argv[3].strip()
        if not name:
            print("Имя хранилища не может быть пустым.")
            return 1
        sid = add_storage(name)
        print(f'Хранилище "{name}" добавлено (id={sid}).')
        return 0

    if sub == "list":
        storages = get_all_storages()
        if not storages:
            print("Нет хранилищ. Добавьте: python main.py storage add <имя>")
            return 0
        for s in storages:
            status = "доступно" if s.is_available else "недоступно"
            print(f"  [{s.id}] {s.name} ({status})")
        return 0

    if sub == "remove":
        if len(sys.argv) < 4:
            print("Использование: python main.py storage remove <id>")
            return 1
        try:
            sid = int(sys.argv[3])
        except ValueError:
            print("ID должен быть числом.")
            return 1
        if remove_storage(sid):
            print("Хранилище удалено.")
        else:
            print("Хранилище с таким ID не найдено.")
        return 0

    print(f"Неизвестная подкоманда: {sub}")
    return 1


def _format_size(size: int) -> str:
    """Форматирует размер в байтах."""
    for u, suffix in [(10**9, "ГБ"), (10**6, "МБ"), (10**3, "КБ")]:
        if size >= u:
            return f"{size / u:.1f} {suffix}"
    return f"{size} Б"


def cmd_file() -> int:
    """Управление файлами: add, remove, list."""
    if len(sys.argv) < 3:
        print("Использование: python main.py file <add|remove|list> [аргументы]")
        return 1

    sub = sys.argv[2].lower()
    if sub == "add":
        if len(sys.argv) < 5:
            print("Использование: python main.py file add <movie_id> <имя> [размер] [storage_id|имя...]")
            return 1
        try:
            movie_id = int(sys.argv[3])
        except ValueError:
            print("movie_id должен быть числом.")
            return 1
        name = sys.argv[4].strip()
        if not name:
            print("Имя файла не может быть пустым.")
            return 1
        size = 0
        storage_args = []
        for arg in sys.argv[5:]:
            try:
                size = int(arg)
            except ValueError:
                storage_args.append(arg)
        file_id = add_file(movie_id, name, size)
        for arg in storage_args:
            arg = arg.strip()
            try:
                sid = int(arg)
                storage = get_storage_by_id(sid)
            except ValueError:
                storage = get_storage_by_name(arg)
            if storage:
                add_file_to_storage(file_id, storage.id)
        print(f'Файл "{name}" добавлен (id={file_id}).')
        return 0

    if sub == "remove":
        if len(sys.argv) < 4:
            print("Использование: python main.py file remove <file_id>")
            return 1
        try:
            file_id = int(sys.argv[3])
        except ValueError:
            print("file_id должен быть числом.")
            return 1
        if remove_file(file_id):
            print("Файл удалён.")
        else:
            print("Файл с таким ID не найден.")
        return 0

    if sub == "list":
        if len(sys.argv) < 4:
            print("Использование: python main.py file list <movie_id>")
            return 1
        try:
            movie_id = int(sys.argv[3])
        except ValueError:
            print("movie_id должен быть числом.")
            return 1
        movie = get_movie_by_id(movie_id)
        if not movie:
            print("Фильм не найден.")
            return 1
        files_data = get_files_by_movie_id(movie_id)
        if not files_data:
            print(f"У фильма [{movie_id}] {movie.title} нет файлов.")
            return 0
        print(f"Файлы фильма [{movie_id}] {movie.title}:")
        for f, storages in files_data:
            stor_str = ", ".join(s.name for s in storages) if storages else "—"
            print(f"  [id={f.id}] {f.name} ({_format_size(f.size)}) — {stor_str}")
        return 0

    print(f"Неизвестная подкоманда: {sub}")
    return 1


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
    elif cmd == "storage":
        return cmd_storage()
    elif cmd == "file":
        return cmd_file()
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
