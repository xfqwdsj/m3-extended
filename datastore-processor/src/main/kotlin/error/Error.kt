package top.ltfan.m3x.datastore.processor.error

data class DatastoreProcessorError(override val message: String?) : Throwable(message)

fun processorError(message: String?): Nothing = throw DatastoreProcessorError(message)
