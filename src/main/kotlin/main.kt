import com.github.javaparser.JavaParser
import github.CachedGithubApi
import github.GithubConnector
import java.io.File
import kotlin.system.exitProcess

const val OUTPUT_FILE = "output.csv"

fun main(args: Array<String>) {
    val arguments = parseArguments(args.toList())
    arguments.getOrDefault(Arguments.TOKEN, null)?.let {
        GithubConnector.apiToken = it
    }

    var ignoreCacheRepositories: Boolean
    var ignoreCacheFiles: Boolean
    getIgnoreCacheValue(arguments.getOrDefault(Arguments.IGNORE_CACHE, "")).let {
        ignoreCacheRepositories = it in listOf(IgnoreCacheValues.REPOS, IgnoreCacheValues.ALL)
        ignoreCacheFiles = it in listOf(IgnoreCacheValues.FILES, IgnoreCacheValues.ALL)
        if (it == IgnoreCacheValues.WRONG) {
            println("Invalid 'ignore-cache' value")
            exitProcess(1)
        }
    }

    var repoCount = 0
    val outcome = CachedGithubApi(ignoreCacheRepositories, ignoreCacheFiles, ignoreCacheFiles).use { api ->
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