package com.sh.video.videolibrary.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

/**
 * Парсер статей с подборками фильмов/сериалов.
 * Поддерживает форматы iXBT Live (заголовки h2/h3 вида "Название (год)").
 */
object ArticleParser {

    private val YEAR_PATTERN = Regex("""\((\d{4})\)""")
    private val TITLE_WITH_YEAR = Regex("""^(.+?)\s*\((\d{4})\)\s*$""")

    /**
     * Загружает страницу по URL и извлекает названия фильмов/сериалов из заголовков.
     * Ищет заголовки h2, h3 с паттерном "Название (год)".
     * @return список строк вида "Название (год)" или пустой список при ошибке
     */
    fun parseMovieTitles(url: String): Result<List<String>> = runCatching {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .timeout(15000)
            .get()

        extractTitlesFromDocument(doc)
    }

    /**
     * Извлекает названия из уже загруженного документа (для тестов).
     */
    fun extractTitlesFromDocument(doc: Document): List<String> {
        val titles = mutableSetOf<String>()
        // iXBT Live: заголовки подборок в h2/h3
        val headers: Elements = doc.select("h2, h3")
        for (el in headers) {
            val text = el.text().trim()
            if (text.isBlank()) continue
            // Паттерн: "Название (год)" или "Название: подзаголовок (год)"
            val match = TITLE_WITH_YEAR.find(text)
            if (match != null) {
                val fullMatch = match.groupValues[0].trim()
                if (fullMatch.length in 3..200) {
                    titles.add(fullMatch)
                }
            } else if (YEAR_PATTERN.containsMatchIn(text)) {
                // Вариант без полного матча — берём всё до "КП:" или подобного
                val cleaned = text
                    .substringBefore("КП:")
                    .substringBefore("—")
                    .trim()
                if (cleaned.length in 3..200) {
                    titles.add(cleaned)
                }
            }
        }
        return titles.toList()
    }

    /**
     * Убирает год из строки для поиска на TMDB.
     * "Она (2013)" -> "Она"
     */
    fun stripYearForSearch(fullTitle: String): String {
        return TITLE_WITH_YEAR.replace(fullTitle) { it.groupValues[1].trim() }
            .ifBlank { fullTitle }
    }
}
