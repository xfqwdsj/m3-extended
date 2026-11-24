package top.ltfan.m3x.datastore.annotation

import kotlin.reflect.KClass

/**
 * Marks a class as a persisted store model.
 *
 * `datastore-processor` generates a `<ClassName>Store` definition object and a delegate property
 * for every constructor parameter.
 *
 * The consuming module must apply the kotlinx-serialization plugin, because the generated
 * definition references `X.serializer()`.
 *
 * @param name the store name, used as the cache key and the file name. The default is the class
 *   name in snake_case.
 * @param codec a [KClass] of the object or class providing the codec factory called by the
 *   generated definition.
 * @param codecFunction the name of the codec factory function, `create` by default.
 * @param derivations a [KClass] of an object or class providing derived values, used when the model
 *   itself cannot be modified.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Store(
    val name: String = "",
    val codec: KClass<*>,
    val codecFunction: String = "create",
    val derivations: KClass<*> = Unit::class,
)

/**
 * Marks a read-only computed property or a `withXxx` function as a derived value of a `@Store`
 * model.
 *
 * A read-only computed property generates a read-only delegate:
 * ```
 * @Derived val orderedSources: List<DownloadSource> get() = ...
 * // -> val MutableStore<DownloadSourceOrder>.orderedSources
 * ```
 *
 * A pure transformation function generates a writable delegate:
 * ```
 * @Derived("blurEnabled", getter = "blur")
 * fun withBlurEnabled(enabled: Boolean): UiSettingsModel = ...
 * // -> var MutableStore<UiSettingsModel>.blurEnabled
 * //        get() = value.blur
 * //        set(v) { write(value.withBlurEnabled(v)) }
 * ```
 *
 * @param name the generated delegate name. The default is the property name for properties, or the
 *   `with` prefix stripped and decapitalized for functions.
 * @param getter the readable property the generated getter reads from, only valid for functions.
 *   The default is [name].
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Derived(
    val name: String = "",
    val getter: String = "",
)
