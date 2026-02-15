# Movie Manager — менеджер видеобиблиотеки

Программа для поиска фильмов на TMDb, добавления в локальную базу и поиска в коллекции.

## Возможности

- **Поиск на TMDb** — вводите название и выбираете фильм из результатов
- **Добавление в базу** — сохраняются название, жанры, рейтинг TMDb, описание
- **Личная оценка** — от 1 до 5 звёзд
- **Хранилища** — указывайте, на каких дисках/NAS лежат файлы фильмов
- **Поиск в коллекции** по:
  - названию
  - жанру
  - рейтингу TMDb
  - слову в описании
  - вашей оценке (1–5)
  - хранилищу (--storage "имя")

## Установка

1. Установите зависимости:
   ```
   cd movie_manager
   pip install -r requirements.txt
   ```

2. Получите API ключ TMDb:
   - Зарегистрируйтесь на [themoviedb.org](https://www.themoviedb.org)
   - Перейдите в [настройки API](https://www.themoviedb.org/settings/api)
   - Скопируйте API Key (v3 auth)

3. Создайте файл `.env`:
   ```
   cp .env.example .env
   ```
   Вставьте ваш ключ в `.env`:
   ```
   TMDB_API_KEY=ваш_ключ_здесь
   ```

## Использование

```
python main.py add      # Поиск на TMDb и добавление фильма
python main.py search   # Поиск в своей коллекции
python main.py search --storage "HDD"  # Поиск с фильтром по хранилищу
python main.py list     # Показать все фильмы
python main.py rate <id> <1-5>  # Поставить свою оценку
python main.py storage add <имя>      # Добавить хранилище
python main.py storage list           # Список хранилищ
python main.py storage remove <id>    # Удалить хранилище
python main.py file add <movie_id> <storage_id|имя>  # Привязать файл к фильму
python main.py file remove <movie_file_id>           # Удалить привязку
python main.py file list <movie_id>  # Файлы фильма
python main.py export [путь]    # Выгрузить в .vlp (совместимо с Android)
python main.py import <путь> [--replace]  # Загрузить из .vlp
```

### Пример

```bash
# Добавить фильм
python main.py add
# Введите: Матрица
# Выберите номер из списка

# Поиск в коллекции по жанру "фантастика"
python main.py search
# Название: [пусто]
# Жанр: фантастика
# ... остальные поля по желанию

# Поставить оценку 5 фильму с ID 1
python main.py rate 1 5
```

## База данных

SQLite база `movies.db` создаётся автоматически в папке `movie_manager`.
