import github.GithubApi

fun main() {
    val api = GithubApi()
    api.getPublicRepositories().filter { it.isJava }.take(150).forEach { println(it.name) }
}