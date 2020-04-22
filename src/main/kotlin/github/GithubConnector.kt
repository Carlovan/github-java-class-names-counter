package github

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max

fun waitUntil(instant: Instant) {
    Thread.sleep(max(0, Instant.now().until(instant, ChronoUnit.MILLIS) + 1000))
}

object GithubConnector {
    private const val BASE_URL = "https://api.github.com"
    private const val AUTH = "0691a72ef6a0fe2d3400b905eb8b94b14a028286"

    fun createUrl(path: String) = "$BASE_URL/$path"
    fun requestUrl(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "token $AUTH")
        var retryConnection: Boolean
        do {
            retryConnection = false
            try {
                conn.connect()
                when (conn.responseCode) {
                    HttpURLConnection.HTTP_FORBIDDEN -> {
                        val remainingRequests = conn.getHeaderFieldInt("X-Ratelimit-Remaining", -1)
                        if (remainingRequests == 0) {
                            retryConnection = true
                            val resetInstant = Instant.ofEpochSecond(conn.getHeaderFieldLong("X-RateLimit-Reset", 0))
                            println("Waiting until ${resetInstant.atZone(ZoneId.systemDefault())} for rate limit")
                            waitUntil(resetInstant)
                        }
                    }
                    HttpURLConnection.HTTP_OK -> {}
                    else -> throw IOException("URL $url returned response ${conn.responseCode} ${conn.responseMessage}. Body is ${conn.reader.readText()}")
                }
            } catch (error: IOException) {
                val body = conn.errorStream?.bufferedReader()?.readText() ?: ""
                println("${error.message}. $body")
                throw error // Rethrow the exception to be handled by the calling method
            }
        } while(retryConnection)
        return conn
    }
    fun request(path: String) = requestUrl(createUrl(path))
}