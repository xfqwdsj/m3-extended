package top.ltfan.m3x.settingspage

import kotlinx.coroutines.flow.StateFlow

/** A renderer-independent settings tree. Dynamic getters must read observable state. */
public interface SettingsTree {
    public val items: List<SettingsItem>
}

/** Persistence state associated with a settings tree. */
public interface SettingsPersistence {
    public val status: StateFlow<SettingsPersistenceStatus>

    public fun retrySave()
}

/** A generated tree bound to a persisted store. */
public interface SettingsStoreDescriber : SettingsTree, SettingsPersistence

/** The restore-default interaction available for a settings store. */
public enum class SettingsResetMode {
    /** Settings items do not expose a generated restore-default value. */
    None,

    /** Settings items expose fresh model defaults for restore-default actions. */
    Menu,
}

public interface SettingsDisplayable {
    public val resources: SettingsResources
}

/** The model consumed by any settings UI. IDs are stable property paths. */
public sealed interface SettingsItem : SettingsDisplayable {
    public val id: String
    public val children: List<SettingsItem>?
    public val shouldShow: Boolean
    public val enabled: Boolean

    public interface Category : SettingsItem {
        override val children: List<SettingsItem>
    }

    public sealed interface Item<T> : SettingsItem {
        public var value: T

        /**
         * Restore-default interaction mode. Handwritten items default to [SettingsResetMode.None].
         */
        public val resetMode: SettingsResetMode
            get() = SettingsResetMode.None

        /** A fresh model's value for this item, or `null` when no default is available. */
        public val defaultValue: T?
            get() = null

        /** A structured rejection reason for a candidate. */
        public fun issue(candidate: T): SettingsValidation.Issue? = null

        /** Returns a user-facing reason why [candidate] cannot be saved. */
        public fun validate(candidate: T): String? = null

        public interface Switch : Item<Boolean>

        public interface Selector<T> : Item<SettingsValue<T>> {
            public val options: List<SettingsValue<T>>
        }

        public interface MultiSelector<T> : Item<Set<SettingsValue<T>>> {
            public val options: List<SettingsValue<T>>
        }

        public interface SingleNumber<T : Number> : Item<T> {
            public val type: NumberType<T>
        }

        public interface NumberRange<T : Number> : Item<Pair<T, T>> {
            public val startType: NumberType<T>
            public val endType: NumberType<T>
        }

        public interface Text : Item<String> {
            public val minLength: Int
                get() = 0

            public val maxLength: Int
                get() = Int.MAX_VALUE

            public val multiline: Boolean
                get() = false
        }

        /** A value with an application-provided UI renderer. */
        public interface Custom<T> : Item<T> {
            public val rendererId: String
        }
    }
}

public sealed interface NumberType<T : Number> {
    public val kind: NumberKind

    public companion object {
        public fun finiteInt(
            type: RangeBasedNumberInfo.Type,
            min: Int,
            max: Int,
            step: Int,
            allowInput: Boolean,
            sliderMode: SliderMode = SliderMode.Continuous,
        ): Finite<Int> = Finite(type, min, max, step, allowInput, sliderMode, NumberKind.Int)

        public fun finiteLong(
            type: RangeBasedNumberInfo.Type,
            min: Long,
            max: Long,
            step: Long,
            allowInput: Boolean,
            sliderMode: SliderMode = SliderMode.Continuous,
        ): Finite<Long> = Finite(type, min, max, step, allowInput, sliderMode, NumberKind.Long)

        public fun finiteFloat(
            type: RangeBasedNumberInfo.Type,
            min: Float,
            max: Float,
            step: Float,
            allowInput: Boolean,
            sliderMode: SliderMode = SliderMode.Continuous,
        ): Finite<Float> = Finite(type, min, max, step, allowInput, sliderMode, NumberKind.Float)

        public fun finiteDouble(
            type: RangeBasedNumberInfo.Type,
            min: Double,
            max: Double,
            step: Double,
            allowInput: Boolean,
            sliderMode: SliderMode = SliderMode.Continuous,
        ): Finite<Double> = Finite(type, min, max, step, allowInput, sliderMode, NumberKind.Double)

        public fun inputInt(): Input<Int> = Input(NumberKind.Int)

        public fun inputLong(): Input<Long> = Input(NumberKind.Long)

        public fun inputFloat(): Input<Float> = Input(NumberKind.Float)

        public fun inputDouble(): Input<Double> = Input(NumberKind.Double)
    }

    public class Finite<T : Number>
    internal constructor(
        override val type: RangeBasedNumberInfo.Type,
        override val min: T,
        override val max: T,
        override val step: T,
        override val allowInput: Boolean,
        override val sliderMode: SliderMode = SliderMode.Continuous,
        override val kind: NumberKind,
    ) : NumberType<T>, RangeBasedNumberInfo<T> {
        override fun equals(other: Any?): Boolean =
            other is Finite<*> &&
                type == other.type &&
                min == other.min &&
                max == other.max &&
                step == other.step &&
                allowInput == other.allowInput &&
                sliderMode == other.sliderMode &&
                kind == other.kind

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + min.hashCode()
            result = 31 * result + max.hashCode()
            result = 31 * result + step.hashCode()
            result = 31 * result + allowInput.hashCode()
            result = 31 * result + sliderMode.hashCode()
            return 31 * result + kind.hashCode()
        }
    }

    public class Input<T : Number> internal constructor(override val kind: NumberKind) :
        NumberType<T>
}

/** Explicit numeric representation; JS erases Int, Float and Double runtime checks. */
public enum class NumberKind {
    Int,
    Long,
    Float,
    Double,
}

public interface RangeBasedNumberInfo<T : Number> {
    public val type: Type
    public val min: T
    public val max: T
    public val step: T
    public val allowInput: Boolean
    public val sliderMode: SliderMode
        get() = SliderMode.Continuous

    /**
     * Preferred control. A renderer may document a fallback for unsupported controls. The Material
     * renderer uses a discrete Slider for Selector/Wheel when the stops are representable,
     * otherwise a NumberStepper.
     */
    public enum class Type {
        Slider,
        Selector,
        Wheel,
    }
}

/** How a slider exposes values from a finite numeric range. */
public enum class SliderMode {
    Continuous,
    Steps,
}

public interface SettingsValue<T> : SettingsDisplayable {
    /** Stable identity, unique across a settings tree. Implementations compare by this ID. */
    public val id: String
    public val value: T
    public val children: List<SettingsItem>?
        get() = null
}

/** Reusable ID equality for generated and handwritten option wrappers. */
public abstract class AbstractSettingsValue<T> : SettingsValue<T> {
    final override fun equals(other: Any?): Boolean = other is SettingsValue<*> && id == other.id

    final override fun hashCode(): Int = id.hashCode()
}
