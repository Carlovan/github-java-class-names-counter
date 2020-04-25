import com.github.javaparser.JavaParser
import github.CachedGithubApi
import java.io.File

const val OUTPUT_FILE = "output.csv"

fun main() {
    var repoCount = 0
    val outcome = CachedGithubApi().use { api ->
        val parser = JavaParser()
        api.getPublicJavaRepositories()
            .onEach {
                repoCount++
                println("Analyzing [$repoCount] '${it.name}'...")
            }
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
    println("\nDone. Analyzed $repoCount repositories")
    val outFile = File(OUTPUT_FILE)
    outFile.writeText("Name,Count\n")
    outcome.forEach {outFile.appendText("\"${it.key}\",${it.value}\n")}
    println("Written output file '$OUTPUT_FILE'")
}