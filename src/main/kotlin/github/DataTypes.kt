package github

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import java.io.IOException

/**
 * Represents a file in a repository on Github
 */
data class File(val path: String, @Json(name = "url") val contentUrl: String) {
    @Json(ignored = true) val isJava: Boolean get() = path.endsWith(".java")
}

/**
 * Represents a repository hosted on Github
 */
data class Repository(
    val id: Int,
    @Json(name="full_name") val name: String,
    val languages_url: String,
    val trees_url: String) {
    var isJava: Boolean by LazyWithSetter{
        val topLanguagesCount = 2
        try {
            val conn = GithubConnector.requestUrl(languages_url)
            val parsed = Klaxon().parseJsonObject(conn.reader)
            val topLanguages = parsed.keys.sortedByDescending { key -> parsed.long(key) }.take(topLanguagesCount)
            topLanguages.map { it.toLowerCase() }.contains("java")
        } catch (e: IOException) {
            false
        }
    }
}
