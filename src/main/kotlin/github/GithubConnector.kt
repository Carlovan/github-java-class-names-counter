package github

import java.net.HttpURLConnection
import java.net.URL

object GithubConnector {
    private const val BASE_URL = "https://api.github.com"
    private const val AUTH = "0691a72ef6a0fe2d3400b905eb8b94b14a028286"

    fun createUrl(path: String) = "$BASE_URL/$path"
    fun requestUrl(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "token $AUTH")
        conn.connect()
        try {
            if (conn.responseCode != 200) {
                println("There was an error on URL $url: ${conn.responseMessage} ${conn.reader.readText()}")
            }
        } catch (e: Exception) {
            println(conn.errorStream.bufferedReader().readText())
        }
        return conn
    }
    fun request(path: String) = requestUrl(createUrl(path))
}