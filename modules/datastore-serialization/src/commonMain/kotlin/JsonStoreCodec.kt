package top.ltfan.m3x.datastore.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import top.ltfan.m3x.datastore.StoreCodec

/** Encodes UTF-8 JSON bytes with caller-owned serializer and format configuration. */
public fun <T> jsonStoreCodec(serializer: KSerializer<T>, json: Json = Json): StoreCodec<T> =
    object : StoreCodec<T> {
        override fun encode(value: T): ByteArray =
            json.encodeToString(serializer, value).encodeToByteArray()

        override fun decode(bytes: ByteArray): T =
            json.decodeFromString(serializer, bytes.decodeToString())
    }
