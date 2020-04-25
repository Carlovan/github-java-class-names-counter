package github

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import java.io.Reader
import java.net.HttpURLConnection
import java.time.Instant
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Converter used to read / write dates to / from JSON
 */
val dateConverter = object: Converter {
    override fun canConvert(cls: Class<*>) = cls == Instant::class.java

    override fun toJson(value: Any): String = "\"${value as Instant}\""

    override fun fromJson(jv: JsonValue) = Instant.parse(jv.string)
}

/**
 * Handy extension property to access connection input reader
 */
val HttpURLConnection.reader: Reader
    get() = inputStream.bufferedReader()

fun <T> Sequence<T>.onEachIndexed(action: (Int, T) -> Unit) = this.mapIndexed { index, value ->
    action(index, value)
    value
}

/**
 * This class is useful to access paginated data from Github API.
 *
 * The path given to the constructor is used to get the first page,
 * then the `Link` header is used to determine subsequent URLs.
 */
class PaginatedRequest(path: String) {
    private var nextLink: String? = GithubConnector.createUrl(path)

    /**
     * Returns the next page of data
     */
    fun next(): Reader? {
        if (nextLink == null)
            return null
        val connection =  GithubConnector.requestUrl(nextLink!!)
        nextLink = Regex("""\s*<(.*?)>;\s*rel="next"""")
            .find(connection.getHeaderField("Link"))
            ?.groupValues?.getOrNull(1)
        return connection.reader
    }
}

/**
 * A property delegate similar to [lazy] that also allows setting a value
 *
 * This is useful when loading lazy properties from the cache
 */
class LazyWithSetter<R, T>(val initializer: () -> T ) : ReadWriteProperty<R, T> {
    private var propValue: T? = null

    override fun getValue(thisRef: R, property: KProperty<*>): T {
        if (propValue == null) {
            propValue = initializer()
        }
        return propValue!!
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        propValue = value
    }

}
