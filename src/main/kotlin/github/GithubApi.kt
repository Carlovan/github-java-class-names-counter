package github

import com.beust.klaxon.Klaxon
import java.io.Closeable


interface GithubApi {
    fun getPublicJavaRepositories(): Sequence<Repository>
}

class GithubApiImpl : GithubApi {
    override fun getPublicJavaRepositories(): Sequence<Repository> {
        val pagination = PaginatedRequest("repositories")
        return generateSequence { pagination.next() }
            .flatMap { Klaxon().parseArray<Repository>(it)?.asSequence() ?: sequenceOf() }
            .filter { it.isJava }
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

    override fun close() {
        cache.close()
    }
}