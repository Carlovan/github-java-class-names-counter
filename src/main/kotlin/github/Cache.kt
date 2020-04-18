package github

import com.beust.klaxon.Klaxon
import java.io.Closeable
import java.time.Duration
import java.time.Instant

class RepositoriesValue(var expiration: Instant, var content: MutableList<Repository>)

data class CacheContainer(
    var repositories: RepositoriesValue? = null) {
}

class GithubCache : Closeable {
    private val cacheFilename = "cache.json"
    private val expirationTime: Long = 1_000_000 // Expiration time in seconds
    private val cacheFile = java.io.File(cacheFilename)

    init {
        val created = cacheFile.createNewFile() // Only if it does not exist
        if (created) {
            cacheFile.writeText("{}") // Empty JSON
        }
    }

    private var cacheValues = Klaxon().converter(dateConverter).parse<CacheContainer>(cacheFile.reader())

    fun hasRepositories(): Boolean = cacheValues?.repositories != null
    fun getRepositories(): List<Repository> = cacheValues!!.repositories!!.content
    fun addRepository(value: Repository) {
        if (!hasRepositories()) {
            val expirationInstant = Instant.now() + Duration.ofSeconds(expirationTime)
            cacheValues!!.repositories = RepositoriesValue(expirationInstant, mutableListOf())
        }
        cacheValues!!.repositories!!.content.add(value)
    }

    override fun close() {
        cacheFile.writeText(Klaxon().toJsonString(cacheValues))
    }
}
