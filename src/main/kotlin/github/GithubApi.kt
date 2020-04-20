package github

import com.beust.klaxon.JsonReader
import com.beust.klaxon.Klaxon
import java.io.Closeable
import java.util.*


interface GithubApi {
    fun getPublicJavaRepositories(): Sequence<Repository>
    fun getRepositoryFiles(repo: Repository): Sequence<File>
    fun getFileContent(file: File): String
}

class GithubApiImpl : GithubApi {
    override fun getPublicJavaRepositories(): Sequence<Repository> {
        val pagination = PaginatedRequest("repositories")
        return generateSequence { pagination.next() }
            .flatMap { Klaxon().parseArray<Repository>(it)?.asSequence() ?: sequenceOf() }
            .filter { it.isJava }
    }

    override fun getRepositoryFiles(repo: Repository): Sequence<File> {
        val conn = GithubConnector.request("repos/${repo.name}/git/trees/master?recursive=true")
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

    override fun getFileContent(file: File): String {
        val conn = GithubConnector.requestUrl(file.contentUrl)

        return String(Base64.getDecoder().decode(Klaxon().parseJsonObject(conn.reader).string("content")!!.replace("\n", "")))
    }
}

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

    override fun close() {
        cache.close()
    }
}