package github

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.sun.org.apache.xpath.internal.operations.Bool
import java.io.Reader
import java.net.HttpURLConnection
import java.time.Instant
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

val dateConverter = object: Converter {
    override fun canConvert(cls: Class<*>)
        = cls == Instant::class.java

    override fun toJson(value: Any): String = "\"${(value as Instant)}\""

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

class LazyWithSetter<R, T>(val initializer: () -> T ) : ReadWriteProperty<R, T> {
    var propValue: T? = null

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
