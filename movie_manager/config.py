"""Конфигурация приложения."""
import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

TMDB_API_KEY = os.getenv("TMDB_API_KEY")
TMDB_BASE_URL = "https://api.themoviedb.org/3"
TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w500"

# Путь к базе данных
DB_PATH = Path(__file__).parent / "movies.db"
