package github

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import java.io.Reader
import java.net.HttpURLConnection

val HttpURLConnection.reader: Reader
    get() = inputStream.bufferedReader()

class File {
    val path: String = ""
    val content: String = ""
}

data class Repository(
    val id: Int,
    @Json(name="full_name") val name: String,
    val languages_url: String) {
    val isJava: Boolean by lazy {
        val conn = GithubConnector.requestUrl(languages_url)
        Klaxon().parseJsonObject(conn.reader).containsKey("Java")
    }
//    val files: Sequence<File>
}

class PaginatedRequest(path: String) {
    var nextLink: String? = GithubConnector.createUrl(path)

    fun next(): Reader? {
        if (nextLink == null)
            return null
        val connection =  GithubConnector.requestUrl(nextLink!!)
        nextLink = Regex("""\s*<(.*?)>;\s*rel="next"""")
            .find(connection.getHeaderField("Link"))
            ?.groupValues?.getOrNull(1)
        return connection.reader
    }
}

class GithubApi {
    fun getPublicRepositories(): Sequence<Repository> {
        val pagination = PaginatedRequest("repositories")
        return generateSequence { pagination.next() }
            .flatMap { Klaxon().parseArray<Repository>(it)?.asSequence() ?: sequenceOf() }
    }
}