import com.github.javaparser.JavaParser
import github.CachedGithubApi
import java.io.File

fun main() {
    val outcome = CachedGithubApi().use { api ->
        val parser = JavaParser()
        api.getPublicJavaRepositories()
            .onEach { println(it.name) }
            .flatMap(api::getRepositoryFiles)
            .filter { it.isJava }
            .map(api::getFileContent)
            .mapNotNull { code -> // Get a list of class names for every file
                parser.parse(code).result.orElse(null)?.types
                    ?.mapNotNull { it.toClassOrInterfaceDeclaration().orElse(null) }
                    ?.filter { !it.isInterface }
                    ?.map { it.nameAsString }
            }
            .flatMap { it.asSequence() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
    }
    val outFile = File("output.csv")
    outFile.writeText("Name,Count\n")
    outcome.forEach {outFile.appendText("\"${it.key}\",${it.value}\n")}
}