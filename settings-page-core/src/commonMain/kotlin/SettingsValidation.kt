package top.ltfan.m3x.settingspage

import kotlin.math.abs
import kotlin.math.round

/** Shared validation used by generated models and alternate renderers. */
public object SettingsValidation {
    /** Structured reasons let UI adapters localize built-in validation. */
    public sealed interface Issue {
        public data class TooShort(val minimum: Int) : Issue

        public data class TooLong(val maximum: Int) : Issue

        public data object NotFinite : Issue

        public data object InvalidBounds : Issue

        public data class OutOfBounds(val minimum: String, val maximum: String) : Issue

        public data class InvalidStep(val step: String) : Issue

        public data object RangeOrder : Issue
    }

    /** Built-in validation for a dynamically selected setting. */
    public fun issueFor(item: SettingsItem.Item<*>, candidate: Any?): Issue? =
        when (item) {
            is SettingsItem.Item.Text ->
                (candidate as? String)?.let {
                    textIssue(it, item.minLength, item.maxLength)
                }

            is SettingsItem.Item.SingleNumber<*> ->
                (candidate as? Number)?.let {
                    numberIssue(it, item.type)
                }

            is SettingsItem.Item.NumberRange<*> -> {
                val pair = candidate as? Pair<*, *>
                val start = pair?.first as? Number
                val end = pair?.second as? Number
                if (start != null && end != null)
                    rangeIssue(start to end, item.startType, item.endType)
                else null
            }

            else -> null
        }

    public fun textIssue(value: String, minLength: Int, maxLength: Int): Issue? =
        when {
            value.length < minLength -> Issue.TooShort(minLength)
            value.length > maxLength -> Issue.TooLong(maxLength)
            else -> null
        }

    public fun numberIssue(value: Number, type: NumberType<*>): Issue? {
        val number = value.toDouble()
        if (!number.isFinite()) return Issue.NotFinite
        if (type !is NumberType.Finite<*>) return null
        if (
            type.kind == NumberKind.Int &&
                (number % 1.0 != 0.0 || number < Int.MIN_VALUE || number > Int.MAX_VALUE)
        )
            return Issue.InvalidStep("1")
        if (type.kind == NumberKind.Long && value !is Long) return Issue.InvalidStep("1")
        val min = type.min.toDouble()
        val max = type.max.toDouble()
        val step = type.step.toDouble()
        if (
            !min.isFinite() ||
                !max.isFinite() ||
                SettingsNumbers.compare(type.min, type.max, type.kind) > 0 ||
                step <= 0.0 ||
                !step.isFinite()
        ) {
            return Issue.InvalidBounds
        }
        if (
            SettingsNumbers.compare(value, type.min, type.kind) < 0 ||
                SettingsNumbers.compare(value, type.max, type.kind) > 0
        ) {
            return Issue.OutOfBounds(type.min.toString(), type.max.toString())
        }
        // Slider stops constrain the control, not values entered precisely.
        if (type.type == RangeBasedNumberInfo.Type.Slider) return null
        if (type.kind == NumberKind.Long) {
            val lower = type.min.toLong()
            val increment = type.step.toLong()
            if ((value.toLong().toULong() - lower.toULong()) % increment.toULong() != 0uL) {
                return Issue.InvalidStep(increment.toString())
            }
            return null
        }
        if (type.kind == NumberKind.Int) {
            val lower = type.min.toInt()
            val increment = type.step.toInt()
            if ((value.toInt().toLong() - lower.toLong()) % increment.toLong() != 0L) {
                return Issue.InvalidStep(increment.toString())
            }
            return null
        }
        val count = (number - min) / step
        return if (abs(count - round(count)) > 1e-5) Issue.InvalidStep(step.toString()) else null
    }

    public fun rangeIssue(
        value: Pair<Number, Number>,
        start: NumberType<*>,
        end: NumberType<*>,
    ): Issue? =
        numberIssue(value.first, start)
            ?: numberIssue(value.second, end)
            ?: if (SettingsNumbers.compare(value.first, value.second, start.kind) > 0)
                Issue.RangeOrder
            else null

    /** Plain Kotlin fallback for adapters without localized resources. */
    public fun Issue.message(): String =
        when (this) {
            is Issue.TooShort -> "Enter at least $minimum characters"
            is Issue.TooLong -> "Enter at most $maximum characters"
            Issue.NotFinite -> "Enter a finite number"
            Issue.InvalidBounds -> "Invalid bounds"
            is Issue.OutOfBounds -> "Enter a value from $minimum to $maximum"
            is Issue.InvalidStep -> "Use increments of $step"
            Issue.RangeOrder -> "Start must not exceed end"
        }

    public fun text(value: String, minLength: Int, maxLength: Int): String? =
        textIssue(value, minLength, maxLength)?.run { message() }

    public fun number(value: Number, type: NumberType<*>): String? =
        numberIssue(value, type)?.run { message() }

    public fun range(
        value: Pair<Number, Number>,
        start: NumberType<*>,
        end: NumberType<*>,
    ): String? = rangeIssue(value, start, end)?.run { message() }
}
