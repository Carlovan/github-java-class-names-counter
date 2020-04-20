import com.github.javaparser.JavaParser
import github.CachedGithubApi

fun main() {
    val outcome = CachedGithubApi().use { api ->
        val parser = JavaParser()
        api.getPublicJavaRepositories()
            .take(50)
            .flatMap(api::getRepositoryFiles)
            .filter { it.isJava }
            .map(api::getFileContent)
            .mapNotNull { code ->
                parser.parse(code).result.orElse(null)?.types
                    ?.mapNotNull { it.toClassOrInterfaceDeclaration().orElse(null) }
                    ?.filter { !it.isInterface }
                    ?.map { it.nameAsString }
            }
            .flatMap { it.asSequence() }
            .count()
    }
    println(outcome)
}