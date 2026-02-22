# VideoLibrary

> **RU:** Android-приложение для управления видеоколлекцией. Поиск фильмов через TMDb, личная оценка, категории, актёры, жанры и хранение медиафайлов. Совместимо с десктопной версией (movie_manager) через общий формат .vlp.
>
> **EN:** Android app for managing your video collection. Movie search via TMDb, personal ratings, categories, actors, genres, and media file storage. Compatible with the desktop version (movie_manager) via shared .vlp format.

---

## Русский

### Возможности

| Функция | Описание |
|---------|----------|
| **Поиск** | Поиск фильмов и сериалов в TMDb |
| **Коллекция** | Библиотека с постерами (Coil — кеш в памяти и на диске) |
| **Оценки** | Личная оценка от 1 до 5 звёзд |
| **Категории** | Собственные подборки фильмов |
| **Актёры и жанры** | Просмотр по актёрам и жанрам |
| **Хранилища** | Привязка медиафайлов к фильмам |
| **Экспорт/импорт** | Формат .vlp — универсальный обмен с десктопом |

### Технологии

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Room** — локальная БД
- **Retrofit** + **OkHttp** — TMDb API
- **Coil** — загрузка постеров
- **Navigation Compose** — навигация

### Требования

- Android 7.0+ (API 24)
- API-ключ [TMDb](https://www.themoviedb.org/settings/api)

### Установка и сборка

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/ваш-username/VideoLibrary.git
   cd VideoLibrary
   ```

2. Соберите debug-версию:
   ```bash
   ./gradlew assembleDebug
   ```
   
   Или release:
   ```bash
   ./gradlew assembleRelease
   ```

3. APK будет в `app/build/outputs/apk/debug/` (или `release/`).

### Настройка

После запуска откройте **Настройки** → **TMDb API key** и введите ваш ключ. Без ключа поиск фильмов работать не будет.

### Формат .vlp

Файлы `.vlp` (Video Library Package) — JSON-формат для обмена данными между мобильным и десктопным приложениями. Экспорт из Android можно импортировать в Python-программу и наоборот.

Спецификация: [export_format.md](export_format.md)

### Структура проекта

```
VideoLibrary/
├── app/                    # Мобильное приложение (Android)
│   └── src/main/java/.../
│       ├── data/           # Repository, Room, API
│       ├── ui/             # Compose UI, ViewModel
│       └── util/
├── movie_manager/          # Десктоп (Python)
├── export_format.md        # Спецификация .vlp
└── README.md
```

---

## English

### Features

| Feature | Description |
|---------|-------------|
| **Search** | Search movies and TV shows on TMDb |
| **Collection** | Library with posters (Coil — in-memory and disk cache) |
| **Ratings** | Personal rating from 1 to 5 stars |
| **Categories** | Custom movie playlists |
| **Actors & Genres** | Browse by actors and genres |
| **Storages** | Link media files to movies |
| **Export/Import** | .vlp format — cross-platform exchange with desktop |

### Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Room** — local database
- **Retrofit** + **OkHttp** — TMDb API
- **Coil** — poster loading
- **Navigation Compose** — navigation

### Requirements

- Android 7.0+ (API 24)
- [TMDb](https://www.themoviedb.org/settings/api) API key

### Installation & Build

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/VideoLibrary.git
   cd VideoLibrary
   ```

2. Build debug version:
   ```bash
   ./gradlew assembleDebug
   ```
   
   Or release:
   ```bash
   ./gradlew assembleRelease
   ```

3. APK will be in `app/build/outputs/apk/debug/` (or `release/`).

### Configuration

After launching, go to **Settings** → **TMDb API key** and enter your key. Search won't work without it.

### .vlp Format

`.vlp` (Video Library Package) files are JSON-based for data exchange between mobile and desktop apps. Exports from Android can be imported into the Python app and vice versa.

Specification: [export_format.md](export_format.md)

### Project Structure

```
VideoLibrary/
├── app/                    # Mobile app (Android)
│   └── src/main/java/.../
│       ├── data/           # Repository, Room, API
│       ├── ui/             # Compose UI, ViewModel
│       └── util/
├── movie_manager/          # Desktop (Python)
├── export_format.md        # .vlp specification
└── README.md
```

---

## License

MIT (or specify your own)
