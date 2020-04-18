import github.CachedGithubApi
import github.GithubApiImpl
import github.GithubCache
import github.Repository

fun main() {
    CachedGithubApi().use {
        it.getPublicJavaRepositories().take(50).forEach { println(it) }
    }

//    val api = GithubApiImpl()
//    api.getPublicRepositories().filter { it.isJava }.take(150).forEach { println(it.name) }
}