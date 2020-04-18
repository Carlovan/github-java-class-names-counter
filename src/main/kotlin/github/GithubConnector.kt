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
        conn.connect()
        try {
            val remainingRequests = conn.getHeaderFieldInt("X-Ratelimit-Remaining", 0)
            if (remainingRequests <= 0) {
                val resetInstant = Instant.ofEpochSecond(conn.getHeaderFieldLong("X-RateLimit-Reset", 0))
                println("Waiting until ${resetInstant.atZone(ZoneId.systemDefault())} for rate limit")
                waitUntil(resetInstant)
            }
            println("Remaining $remainingRequests requests")
            if (conn.responseCode != 200) {
                println("There was an error on URL $url: ${conn.responseMessage} ${conn.reader.readText()}")
            }
        } catch (e: IOException) {
            println(e.message)
            println(conn.errorStream?.bufferedReader()?.readText())
            throw e
        }
        return conn
    }
    fun request(path: String) = requestUrl(createUrl(path))
}