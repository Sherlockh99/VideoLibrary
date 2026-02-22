"""Клиент для работы с API The Movie Database (TMDb)."""
import requests
from typing import Optional
from config import TMDB_API_KEY, TMDB_BASE_URL


class TMDbClient:
    """Клиент для получения данных о фильмах с TMDb."""

    def __init__(self, api_key: Optional[str] = None):
        self.api_key = api_key or TMDB_API_KEY
        if not self.api_key or self.api_key == "your_api_key_here":
            raise ValueError(
                "API ключ TMDb не найден. Создайте файл .env с TMDB_API_KEY. "
                "Получить ключ: https://www.themoviedb.org/settings/api"
            )

    def _request(self, endpoint: str, params: dict | None = None) -> dict:
        """Выполняет GET-запрос к API TMDb."""
        url = f"{TMDB_BASE_URL}{endpoint}"
        request_params = {"api_key": self.api_key, "language": "ru-RU"}
        if params:
            request_params.update(params)

        response = requests.get(url, params=request_params)
        response.raise_for_status()
        return response.json()

    def search_movies(self, query: str, page: int = 1) -> dict:
        """Поиск фильмов по названию."""
        return self._request(
            "/search/movie",
            {"query": query, "page": page},
        )

    def get_movie_details(self, movie_id: int) -> dict:
        """Получение полной информации о фильме."""
        return self._request(f"/movie/{movie_id}")

    def search_tv(self, query: str, page: int = 1) -> dict:
        """Поиск сериалов по названию."""
        return self._request(
            "/search/tv",
            {"query": query, "page": page},
        )

    def get_tv_details(self, tv_id: int) -> dict:
        """Получение полной информации о сериале."""
        return self._request(f"/tv/{tv_id}")

    def get_movie_credits(self, movie_id: int) -> dict:
        """Получение актёров фильма. cast содержит order (billing order)."""
        return self._request(f"/movie/{movie_id}/credits")

    def get_tv_credits(self, tv_id: int) -> dict:
        """Получение актёров сериала."""
        return self._request(f"/tv/{tv_id}/credits")

    def get_genres(self) -> list[dict]:
        """Получение списка жанров."""
        data = self._request("/genre/movie/list", {"language": "ru-RU"})
        return data.get("genres", [])
