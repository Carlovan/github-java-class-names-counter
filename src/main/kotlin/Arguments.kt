enum class Arguments {
    TOKEN,
    IGNORE_CACHE
}

enum class IgnoreCacheValues {
    ALL,
    REPOS,
    FILES,
    NOTHING,
    WRONG
}

fun getArgName(arg: Arguments): String = arg.name.toLowerCase().replace('_', '-')

fun parseArguments(args: Collection<String>): Map<Arguments, String> {
    val output = mutableMapOf<Arguments, String>()
    Arguments.values().forEach { argument ->
        val found = args.findLast { it.startsWith("--${getArgName(argument)}=") }
        if (found != null) {
            output[argument] = found.split('=', limit=2).getOrNull(1) ?: ""
        }
    }
    return output
}

fun getIgnoreCacheValue(str: String): IgnoreCacheValues = when(str) {
    "all" -> IgnoreCacheValues.ALL
    "repos" -> IgnoreCacheValues.REPOS
    "files" -> IgnoreCacheValues.FILES
    "" -> IgnoreCacheValues.NOTHING
    else -> IgnoreCacheValues.WRONG
}

