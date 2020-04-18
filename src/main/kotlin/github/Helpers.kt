package github

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import java.io.Reader
import java.net.HttpURLConnection
import java.time.Instant
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

val dateConverter = object: Converter {
    override fun canConvert(cls: Class<*>)
        = cls == Instant::class.java

    override fun toJson(value: Any): String = "\"${value as Instant}\""

    override fun fromJson(jv: JsonValue) = Instant.parse(jv.string)

}

val HttpURLConnection.reader: Reader
    get() = inputStream.bufferedReader()

class PaginatedRequest(path: String) {
    private var nextLink: String? = GithubConnector.createUrl(path)

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
 * This is useful when loading from the cache
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
