package github

import com.beust.klaxon.Klaxon
import java.io.Closeable
import java.time.Duration
import java.time.Instant

class RepositoriesValue(var expiration: Instant, var content: MutableList<Repository>)
class FilesValue(var expiration: Instant, var content: MutableList<File>)

data class CacheContainer(
    var repositories: RepositoriesValue? = null,
    var files: MutableMap<String, FilesValue?>? = null)

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

    fun hasRepositories(): Boolean {
        val repos = cacheValues?.repositories
        val ok = repos != null && repos.expiration > Instant.now()
        if (!ok) {
            cacheValues?.repositories = null
        }
        return ok
    }
    fun getRepositories(): List<Repository> {
        return if (hasRepositories()) {
            cacheValues!!.repositories!!.content
        } else {
            throw IllegalStateException()
        }
    }
    fun addRepository(value: Repository) {
        if (!hasRepositories()) {
            val expirationInstant = Instant.now() + Duration.ofSeconds(expirationTime)
            cacheValues!!.repositories = RepositoriesValue(expirationInstant, mutableListOf())
        }
        cacheValues!!.repositories!!.content.add(value)
    }

    fun hasFiles(repo: Repository): Boolean {
        val key = repo.name
        val filesList = cacheValues?.files?.getOrDefault(key, null)
        val ok = filesList != null && filesList.expiration > Instant.now()
        if (!ok) {
            cacheValues?.files?.set(key, null)
        }
        return ok
    }
    fun getFiles(repo: Repository): List<File> {
        return if (hasFiles(repo)) {
            cacheValues!!.files!![repo.name]!!.content
        } else {
            throw IllegalStateException()
        }
    }
    fun addFile(repo: Repository, file: File) {
        val key = repo.name
        if (!hasFiles(repo)) {
            if (cacheValues?.files == null) {
                cacheValues?.files = mutableMapOf()
            }
            val expirationInstant = Instant.now() + Duration.ofSeconds(expirationTime)
            cacheValues?.files!![key] = FilesValue(expirationInstant, mutableListOf())
        }
        cacheValues!!.files!![key]!!.content.add(file)
    }

    override fun close() {
        cacheFile.writeText(Klaxon().toJsonString(cacheValues))
    }
}
