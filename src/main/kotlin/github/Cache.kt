package github

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import java.io.Closeable
import java.time.Duration
import java.time.Instant

/**
 * Interface to represent a generic value stored in the cache
 */
interface CacheValue<T> {
    val lifeDuration: Duration // This field is meant to allow every subclass to choose a specific expiration time
    var expiration: Instant
    var content: T
    @Json(ignored = true) val expired: Boolean get() = expiration <= Instant.now()

    fun hasValue() = !expired
    fun getValue(): T = if (hasValue()) content else throw IllegalStateException()
    fun setValue(value: T) {
        content = value
        resetExpiration()
    }
    fun resetExpiration() {
        expiration = Instant.now() + lifeDuration
    }
}

/**
 * Interface to represent a list of values stored in the cache
 *
 * The expiration time is set on the list, not on individual items
 */
interface ListCacheValue<T>: CacheValue<MutableList<T>> {
    fun addValue(value: T) {
        if (!hasValue()) {
            setValue(mutableListOf())
        }
        content.add(value)
    }
}

/**
 * Interface to represent a mapping between keys and values stored in the cache
 *
 * Every value can be accessed with a key. Every value has its own expiration time
 */
interface MappedCacheValue<C : CacheValue<T>, T> {
    var content: MutableMap<String, C>

    fun hasValue(key: String): Boolean = !(content.getOrDefault(key, null)?.expired ?: true)
    fun getValue(key: String) = if (hasValue(key)) content[key]!!.getValue() else throw IllegalStateException()
    fun setValue(key: String, value: C) {
        content[key] = value
    }
}

/*
    These classes implements some cache-value interfaces.
    This allows the deserializer to know every type at runtime (since these are real classes)
    but still maintain type safety and flexibility with generic interfaces
 */
class RepositoriesValue(override var expiration: Instant = Instant.MIN, override var content: MutableList<Repository> = mutableListOf()) : ListCacheValue<Repository> {
    @Json(ignored = true) override val lifeDuration: Duration = Duration.ofDays(30)
}
class FilesValue(override var expiration: Instant = Instant.MIN, override var content: MutableList<File> = mutableListOf()) : ListCacheValue<File> {
    @Json(ignored = true) override val lifeDuration: Duration = Duration.ofDays(7)
}
class StringValue(override var expiration: Instant = Instant.MIN, override var content: String = "") : CacheValue<String> {
    @Json(ignored = true) override val lifeDuration: Duration = Duration.ofDays(7)
}
class MappedFilesValue(override var content: MutableMap<String, FilesValue>): MappedCacheValue<FilesValue, MutableList<File>>
class MappedFileContentValue(override var content: MutableMap<String, StringValue>): MappedCacheValue<StringValue, String>

/**
 * Class representing the complete cache. Useful to be serialized
 */
data class CacheContainer(
    var repositories: RepositoriesValue? = null,
    var files: MappedFilesValue? = null,
    var fileContent: MappedFileContentValue? = null)

/**
 * This class allows access to the cache values with specific methods
 *
 * Saves data to disk when closed.
 */
class GithubCache : Closeable {
    private val cacheFilename = "cache.json"
    private val cacheFile = java.io.File(cacheFilename)

    init {
        val created = cacheFile.createNewFile() // Only if it does not exist
        if (created) {
            cacheFile.writeText("{}") // Empty JSON
        }
    }

    private var cacheValues = Klaxon().converter(dateConverter).parse<CacheContainer>(cacheFile.reader()) ?: CacheContainer()

    fun hasRepositories(): Boolean = cacheValues.repositories?.hasValue() ?: false
    fun getRepositories(): List<Repository> = cacheValues.repositories?.getValue() ?: throw IllegalStateException()
    fun addRepository(value: Repository) {
        if (cacheValues.repositories == null)
            cacheValues.repositories = RepositoriesValue()
        cacheValues.repositories?.addValue(value)
    }

    private fun filesKey(repo: Repository) = repo.name
    fun hasFiles(repo: Repository): Boolean = cacheValues.files?.hasValue(filesKey(repo)) ?: false
    fun getFiles(repo: Repository): List<File> = cacheValues.files?.getValue(filesKey(repo)) ?: throw IllegalStateException()
    fun addFile(repo: Repository, file: File) {
        if (cacheValues.files == null) {
            cacheValues.files = MappedFilesValue(mutableMapOf())
        }
        if (!hasFiles(repo)) {
            val newValue = FilesValue()
            newValue.resetExpiration()
            cacheValues.files?.setValue(filesKey(repo), newValue)
        }
        cacheValues.files?.content?.get(filesKey(repo))?.addValue(file)
    }

    private fun contentKey(file: File) = file.contentUrl
    fun hasContent(file: File): Boolean = cacheValues.fileContent?.hasValue(contentKey(file)) ?: false
    fun getContent(file: File): String = cacheValues.fileContent?.getValue(contentKey(file)) ?: throw IllegalStateException()
    fun setContent(file: File, content: String) {
        if (cacheValues.fileContent == null) {
            cacheValues.fileContent = MappedFileContentValue(mutableMapOf())
        }
        val newValue = StringValue(content=content)
        newValue.resetExpiration()
        cacheValues.fileContent?.setValue(contentKey(file), newValue)
    }

    override fun close() {
        cacheFile.writeText(Klaxon().toJsonString(cacheValues))
    }
}
