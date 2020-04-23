package github

import com.beust.klaxon.JsonReader
import com.beust.klaxon.Klaxon
import com.damnhandy.uri.template.UriTemplate
import java.io.Closeable
import java.util.*

/**
 * Methods useful to access Github API
 */
interface GithubApi {
    fun getPublicJavaRepositories(): Sequence<Repository>
    fun getRepositoryFiles(repo: Repository): Sequence<File>
    fun getFileContent(file: File): String
}

/**
 * Makes requests to Github API to retrieve the requested data.
 *
 * Lists are returned as [Sequence] to allow laziness
 */
class GithubApiImpl : GithubApi {
    /**
     * Returns all the public Java-based repositories hosted on Github.
     * Requests are made lazily only when necessary
     */
    override fun getPublicJavaRepositories(): Sequence<Repository> {
        val pagination = PaginatedRequest("repositories")
        return generateSequence { pagination.next() }
            .flatMap { Klaxon().parseArray<Repository>(it)?.asSequence() ?: sequenceOf() }
            .filter { it.isJava }
    }

    /**
     * Returns all the files contained inside the given repository
     * Only a single request is made using the "recursive" query parameter,
     * but the JSON is parsed lazily (not really useful though).
     * This could lead to truncated output, but the threshold is very high
     */
    override fun getRepositoryFiles(repo: Repository): Sequence<File> {
        val url = UriTemplate.fromTemplate(repo.trees_url)
            .set("sha", "master")
            .expand() + "?recursive=true"
        println(url)
        val conn = GithubConnector.requestUrl(url)
        val klaxon = Klaxon()

        val files = mutableListOf<File>()
        JsonReader(conn.reader).use {reader ->
            reader.beginObject {
                while (reader.hasNext()) {
                    when(reader.nextName()) {
                        "tree" -> {
                            reader.beginArray {
                                while(reader.hasNext())
                                    files.add(klaxon.parse(reader)!!)
                            }
                        }
                        "truncated" -> if (reader.nextBoolean()) println("Tree for ${repo.name} was truncated")
                    }
                }
            }
        }

        return files.asSequence()
    }

    /**
     * Returns the content of a file as a [String].
     * In this application this is only called on Java source file so [String] is appropriate.
     */
    override fun getFileContent(file: File): String {
        val conn = GithubConnector.requestUrl(file.contentUrl)

        return String(Base64.getDecoder().decode(Klaxon().parseJsonObject(conn.reader).string("content")!!.replace("\n", "")))
    }
}

/**
 * This class allows accessing Github API and caching responses.
 *
 * Cached data are returned if present, otherwise [GithubApiImpl] is used to retrieve them and then they are stored.
 * Cache data is written to disk only when [close] is called
 */
class CachedGithubApi : GithubApi, Closeable {
    private val cache = GithubCache()
    private val api = GithubApiImpl()

    override fun getPublicJavaRepositories(): Sequence<Repository> {
        return if (cache.hasRepositories()) {
            cache.getRepositories().asSequence()
        } else {
            api.getPublicJavaRepositories()
                .onEach { cache.addRepository(it) }
        }
    }

    override fun getRepositoryFiles(repo: Repository): Sequence<File> {
        return if (cache.hasFiles(repo)) {
            cache.getFiles(repo).asSequence()
        } else {
            api.getRepositoryFiles(repo)
                .onEach { cache.addFile(repo, it) }
        }
    }

    override fun getFileContent(file: File): String {
        return if (cache.hasContent(file)) {
            cache.getContent(file)
        } else {
            val result = api.getFileContent(file)
            cache.setContent(file, result)
            return result
        }
    }

    /**
     * Closes the cache object so data is written to disk.
     */
    override fun close() {
        cache.close()
    }
}