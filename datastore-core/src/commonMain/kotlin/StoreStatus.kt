package top.ltfan.m3x.datastore

/** Observable state of a [MutableStore]. Failures retain the local value for retry. */
public sealed interface StoreStatus {
    public data object Loading : StoreStatus

    public data object Ready : StoreStatus

    public data class ReadFailed(val cause: Throwable) : StoreStatus

    public data class WriteFailed(val cause: Throwable) : StoreStatus
}
