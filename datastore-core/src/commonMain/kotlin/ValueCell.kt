package top.ltfan.m3x.datastore

/** A value that may notify readers when it changes. */
public interface ValueCell<T> {
    public var value: T
}

/** Creates the observation mechanism used by a [MutableStore]. */
public interface ValueCellFactory {
    public fun <T> create(initial: T): ValueCell<T>

    public companion object {
        /** A cell without UI observation, useful outside UI and in tests. */
        public val Plain: ValueCellFactory =
            object : ValueCellFactory {
                override fun <T> create(initial: T): ValueCell<T> =
                    object : ValueCell<T> {
                        override var value: T = initial
                    }
            }
    }
}
