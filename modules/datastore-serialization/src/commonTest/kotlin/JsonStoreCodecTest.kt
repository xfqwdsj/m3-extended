package top.ltfan.m3x.datastore.serialization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class JsonStoreCodecTest {
    @Test
    fun utf8AndEscapedTextRoundTripThroughTheSuppliedSerializer() {
        val value = buildJsonObject { put("greeting", "你好\"") }
        val codec = jsonStoreCodec(JsonObject.serializer())
        val bytes = codec.encode(value)
        assertEquals("""{"greeting":"你好\""}""", bytes.decodeToString())
        assertEquals(value, codec.decode(bytes))
    }

    @Test
    fun independentCodecsRetainTheirCallersFormatConfiguration() {
        val value = buildJsonObject { put("enabled", true) }
        val compact = jsonStoreCodec(JsonObject.serializer())
        val formatted = jsonStoreCodec(JsonObject.serializer(), Json { prettyPrint = true })
        assertTrue('\n' !in compact.encode(value).decodeToString())
        assertTrue('\n' in formatted.encode(value).decodeToString())
        assertEquals(value, compact.decode(formatted.encode(value)))
    }
}
