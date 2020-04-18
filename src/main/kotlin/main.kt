import github.CachedGithubApi

fun main() {
    CachedGithubApi().use { api ->
        api.getPublicJavaRepositories().take(50)
            .onEach(::println)
            .flatMap { api.getRepositoryFiles(it) }
            .filter { it.isJava }
            .forEach(::println)
    }
}