package com.sh.video.videolibrary.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

/**
 * Результат парсинга статьи: названия фильмов и заголовок страницы для категории.
 */
data class ArticleParseResult(
    val movieTitles: List<String>,
    val pageTitle: String
)

/**
 * Парсер статей с подборками фильмов/сериалов.
 * Поддерживает форматы iXBT Live (заголовки h2/h3 вида "Название (год)").
 */
object ArticleParser {

    private val YEAR_PATTERN = Regex("""\((\d{4})\)""")
    private val TITLE_WITH_YEAR = Regex("""^(.+?)\s*\((\d{4})\)\s*$""")

    /**
     * Загружает страницу по URL и извлекает названия фильмов/сериалов из заголовков,
     * а также заголовок страницы для использования в качестве названия категории.
     */
    fun parseArticle(url: String): Result<ArticleParseResult> = runCatching {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .timeout(15000)
            .get()

        val titles = extractTitlesFromDocument(doc)
        val pageTitle = extractPageTitle(doc)
        ArticleParseResult(movieTitles = titles, pageTitle = pageTitle)
    }

    private fun extractPageTitle(doc: Document): String {
        // Сначала пробуем h1 (основной заголовок статьи)
        val h1 = doc.select("h1").first()?.text()?.trim()
        if (!h1.isNullOrBlank()) return cleanPageTitle(h1)
        // Иначе берём из <title>, часто формат "Заголовок / Раздел / Сайт"
        val title = doc.title().trim()
        return cleanPageTitle(title)
    }

    private fun cleanPageTitle(raw: String): String {
        return raw
            .substringBefore(" / ")
            .substringBefore(" | ")
            .trim()
            .take(150)
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
