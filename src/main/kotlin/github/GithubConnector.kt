package github

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * This function sleeps until the given [Instant] is reached
 */
fun waitUntil(instant: Instant) {
    Thread.sleep(max(0, Instant.now().until(instant, ChronoUnit.MILLIS)))
}

/**
 * Object used to make requests to the Github API
 *
 * This object handles the authentication to the API, the possible errors returned and similar connection-related stuff
 */
object GithubConnector {
    private const val BASE_URL = "https://api.github.com"
    private const val AUTH = "0691a72ef6a0fe2d3400b905eb8b94b14a028286" // TODO

    /**
     * Creates a URL to Github API given the path
     */
    fun createUrl(path: String) = "$BASE_URL/$path"

    /**
     * Requests a URL from Github API given only the path
     */
    fun request(path: String) = requestUrl(createUrl(path))

    /**
     * Requests the given URL from Github API.
     *
     * @throws IOException if something goes wrong while connecting
     * @return a connected [HttpURLConnection]
     */
    fun requestUrl(url: String): HttpURLConnection {
        val retrySeconds = 5
        var retryConnection = false
        var retryCount = 5

        var conn: HttpURLConnection
        do {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "token $AUTH")
            if (retryConnection) {
                println("Retrying in $retrySeconds seconds...")
                Thread.sleep(retrySeconds * 1000L)
            }
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
                    else -> {
                        val msg = "URL $url returned response ${conn.responseCode} ${conn.responseMessage}. Body is ${conn.reader.readText()}"
                        if (retryCount <= 0) {
                            throw IOException(msg)
                        } else {
                            println(msg)
                            retryConnection = true
                        }
                    }
                }
            } catch (error: IOException) {
                val body = conn.errorStream?.bufferedReader()?.readText() ?: ""
                println("${error.message}. $body")
                throw error // Rethrow the exception to be handled by the calling method
            }
            retryCount--
        } while(retryConnection)
        return conn
    }
}
