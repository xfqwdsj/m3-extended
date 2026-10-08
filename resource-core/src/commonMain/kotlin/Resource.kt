package top.ltfan.m3x.resource

/** A value described independently of any UI toolkit. Adapters may provide other sources. */
public interface Resource<out T> {
    /** A resource that can be read without a UI or platform context. */
    public interface Global<out T> : Resource<T> {
        public val value: T

        public data class Static<T>(override val value: T) : Global<T>

        public class Dynamic<T>(private val provider: () -> T) : Global<T> {
            override val value: T
                get() = provider()
        }

        public class Lazy<T>(initializer: () -> T) : Global<T> {
            override val value: T by lazy(initializer)
        }
    }
}
