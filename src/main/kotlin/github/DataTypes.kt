package github

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import java.io.IOException

class File {
    val path: String = ""
    val content: String = ""
}

data class Repository(
    val id: Int,
    @Json(name="full_name") val name: String,
    val languages_url: String) {
    var isJava: Boolean by LazyWithSetter{
        try {
            val conn = GithubConnector.requestUrl(languages_url)
            Klaxon().parseJsonObject(conn.reader).containsKey("Java")
        } catch (e: IOException) {
            false
        }
    }
//    val files: Sequence<File>
}
