package top.ltfan.m3x.datastore.processor.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

/** A constructor parameter of the model. */
data class PropertyModel(
    val name: String,
    val type: TypeName,
)

/** A `@Derived` member. */
sealed interface DerivedModel {
    /** The generated delegate name. */
    val name: String

    /** The type of the derived value. */
    val type: TypeName

    /** The readable property the generated getter reads from. */
    val getter: String

    /** The derivations provider, or `null` when the member is declared on the model itself. */
    val owner: ClassName?

    /** The write function, or `null` for a read-only derived value. */
    val function: String?

    data class ReadOnly(
        override val name: String,
        override val type: TypeName,
        override val getter: String,
        override val owner: ClassName? = null,
    ) : DerivedModel {
        override val function: String?
            get() = null
    }

    data class Writable(
        override val name: String,
        override val type: TypeName,
        override val getter: String,
        override val function: String,
        override val owner: ClassName? = null,
    ) : DerivedModel
}

/** Everything needed to generate the definition object and its delegates for a model. */
data class StoreModel(
    val className: ClassName,
    val storeName: String,
    val codec: ClassName,
    val codecFunction: String,
    val properties: List<PropertyModel>,
    val derivations: List<DerivedModel>,
    val hasSettingsStore: Boolean,
)
