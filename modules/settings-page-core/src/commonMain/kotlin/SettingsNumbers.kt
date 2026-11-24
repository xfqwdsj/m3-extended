package top.ltfan.m3x.settingspage

import kotlin.math.abs
import kotlin.math.roundToInt

/** Numeric operations shared by generated, handwritten and rendered settings. */
public object SettingsNumbers {
    /** Maximum interval count for a discrete Material slider. */
    public const val MAX_SLIDER_INTERVALS: Int = 4096

    /** Compares values according to their declared representation. */
    public fun compare(a: Number, b: Number, kind: NumberKind): Int =
        when (kind) {
            NumberKind.Long -> a.toLong().compareTo(b.toLong())
            NumberKind.Int -> a.toInt().compareTo(b.toInt())
            NumberKind.Float -> a.toFloat().compareTo(b.toFloat())
            NumberKind.Double -> a.toDouble().compareTo(b.toDouble())
        }

    /** Parses an editor string using the declared numeric representation. */
    public fun parse(input: String, kind: NumberKind): Number? =
        when (kind) {
            NumberKind.Int -> input.toIntOrNull()
            NumberKind.Long -> input.toLongOrNull()
            NumberKind.Float -> input.toFloatOrNull()?.takeIf { it.isFinite() }
            NumberKind.Double -> input.toDoubleOrNull()?.takeIf { it.isFinite() }
        }

    /** Moves one valid increment without manufacturing an off-grid endpoint. */
    public fun stepNumber(value: Number, bounds: NumberType.Finite<*>, increase: Boolean): Number {
        return when (bounds.kind) {
            NumberKind.Long -> {
                val current = value.toLong()
                if (current < bounds.min.toLong() || current > bounds.max.toLong()) return value
                val step = bounds.step.toLong().toULong()
                val limit = if (increase) bounds.max.toLong() else bounds.min.toLong()
                val remaining =
                    if (increase) limit.toULong() - current.toULong()
                    else current.toULong() - limit.toULong()
                if (step > remaining) value
                else if (increase) current + step.toLong() else current - step.toLong()
            }

            NumberKind.Int -> {
                val current = value.toInt()
                if (current < bounds.min.toInt() || current > bounds.max.toInt()) return value
                val step = bounds.step.toLong()
                val limit = if (increase) bounds.max.toInt() else bounds.min.toInt()
                val remaining =
                    if (increase) limit.toLong() - current.toLong()
                    else current.toLong() - limit.toLong()
                if (step > remaining) value
                else (current.toLong() + if (increase) step else -step).toInt()
            }

            NumberKind.Double -> {
                val current = value.toDouble()
                if (current < bounds.min.toDouble() || current > bounds.max.toDouble()) return value
                val next = current + (if (increase) 1.0 else -1.0) * bounds.step.toDouble()
                if (next < bounds.min.toDouble() || next > bounds.max.toDouble()) value else next
            }

            NumberKind.Float -> {
                val current = value.toFloat()
                if (current < bounds.min.toFloat() || current > bounds.max.toFloat()) return value
                val next =
                    current.toDouble() + (if (increase) 1.0 else -1.0) * bounds.step.toDouble()
                if (next < bounds.min.toDouble() || next > bounds.max.toDouble()) value
                else next.toFloat()
            }
        }
    }

    /**
     * A normalized slider mapping. Positions are always in `0f..1f`. The underlying value is never
     * changed merely by obtaining its position.
     */
    public class SliderMapping(public val bounds: NumberType.Finite<*>) {
        private val min = bounds.min
        private val max = bounds.max
        private val intervalCount: Int by lazy {
            if (bounds.sliderMode == SliderMode.Steps) countIntervals() else 0
        }

        /** Number of interior Material Slider stops. */
        public val steps: Int
            get() = if (intervalCount > 1) intervalCount - 1 else 0

        public val isFixed: Boolean
            get() = min == max

        init {
            require(bounds.type == RangeBasedNumberInfo.Type.Slider) {
                "Slider mapping requires a slider"
            }
            require(min.toDouble().isFinite() && max.toDouble().isFinite()) {
                "Bounds must be finite"
            }
            require(compare(min, max, bounds.kind) <= 0) { "Bounds must be ordered" }
            require(bounds.step.toDouble().isFinite() && bounds.step.toDouble() > 0.0) {
                "Step must be positive"
            }
            intervalCount // Reject invalid discrete mappings at construction.
        }

        /** Returns the original value represented at [position]. */
        public fun valueAt(position: Float): Number {
            require(position.isFinite()) { "Slider position must be finite" }
            val p = position.coerceIn(0f, 1f)
            if (isFixed || p == 0f) return min
            if (p == 1f) return max
            if (bounds.sliderMode == SliderMode.Steps) {
                val index = (p * intervalCount).roundToInt().coerceIn(0, intervalCount)
                return steppedValue(index)
            }
            return interpolate(p)
        }

        /** Returns the nearest display position without changing [value]. */
        public fun positionOf(value: Number): Float {
            if (isFixed) return 0f
            if (compare(value, min, bounds.kind) <= 0) return 0f
            if (compare(value, max, bounds.kind) >= 0) return 1f
            val ratio =
                when (bounds.kind) {
                    NumberKind.Long ->
                        (value.toLong().toULong() - min.toLong().toULong()).toDouble() /
                            (max.toLong().toULong() - min.toLong().toULong()).toDouble()

                    NumberKind.Int ->
                        (value.toInt().toLong() - min.toInt().toLong()).toDouble() /
                            (max.toInt().toLong() - min.toInt().toLong()).toDouble()

                    else -> {
                        val a = min.toDouble()
                        val b = max.toDouble()
                        val v = value.toDouble()
                        val span = b - a
                        if (span.isFinite()) (v - a) / span
                        else (v / 2.0 - a / 2.0) / (b / 2.0 - a / 2.0)
                    }
                }
            return ratio.toFloat().coerceIn(0f, 1f)
        }

        private fun interpolate(p: Float): Number =
            when (bounds.kind) {
                NumberKind.Long -> {
                    val span = max.toLong().toULong() - min.toLong().toULong()
                    val denominator = 16_777_216uL
                    val k = (p * 16_777_216f).roundToInt().toULong()
                    val q = span / denominator
                    val r = span % denominator
                    (min.toLong().toULong() + q * k + (r * k + denominator / 2uL) / denominator)
                        .toLong()
                }

                NumberKind.Int -> {
                    val span = max.toInt().toLong() - min.toInt().toLong()
                    val k = (p * 16_777_216f).roundToInt().toLong()
                    (min.toInt().toLong() + (span * k + 8_388_608L) / 16_777_216L).toInt()
                }

                NumberKind.Float -> {
                    val a = min.toDouble()
                    val b = max.toDouble()
                    ((1.0 - p.toDouble()) * a + p.toDouble() * b).coerceIn(a, b).toFloat()
                }

                NumberKind.Double -> {
                    val a = min.toDouble()
                    val b = max.toDouble()
                    ((1.0 - p.toDouble()) * a + p.toDouble() * b).coerceIn(a, b)
                }
            }

        private fun countIntervals(): Int {
            if (min == max) return 0
            val count =
                when (bounds.kind) {
                    NumberKind.Long -> {
                        val span = max.toLong().toULong() - min.toLong().toULong()
                        val step = bounds.step.toLong().toULong()
                        require(step > 0uL && span % step == 0uL) {
                            "Step does not divide the range"
                        }
                        val n = span / step
                        require(n <= MAX_SLIDER_INTERVALS.toULong()) { "Too many slider stops" }
                        n.toInt()
                    }

                    NumberKind.Int -> {
                        val span = max.toInt().toLong() - min.toInt().toLong()
                        val step = bounds.step.toInt().toLong()
                        require(step > 0 && span % step == 0L) { "Step does not divide the range" }
                        val n = span / step
                        require(n <= MAX_SLIDER_INTERVALS) { "Too many slider stops" }
                        n.toInt()
                    }

                    NumberKind.Float,
                    NumberKind.Double -> {
                        val ratio = (max.toDouble() - min.toDouble()) / bounds.step.toDouble()
                        require(ratio.isFinite() && ratio in 1.0..MAX_SLIDER_INTERVALS.toDouble()) {
                            "Too many slider stops"
                        }
                        val n = ratio.roundToInt()
                        require(abs(ratio - n) <= 1e-5 * n) { "Step does not divide the range" }
                        n
                    }
                }
            var previous: Number = min
            for (i in 1..count) {
                val current = steppedValue(i, count)
                require(compare(current, previous, bounds.kind) > 0) {
                    "Slider stops are not distinct"
                }
                // Kotlin/JS models Float as a JS number, so numeric comparison alone
                // can accept stops that collapse to the IEEE-754 Float used by Slider.
                if (bounds.kind == NumberKind.Float) {
                    require(current.toFloat().toBits() != previous.toFloat().toBits()) {
                        "Slider stops are not distinct as Float values"
                    }
                }
                require((i.toFloat() / count) > ((i - 1).toFloat() / count)) {
                    "Slider positions are not distinct"
                }
                previous = current
            }
            return count
        }

        private fun steppedValue(index: Int, count: Int = intervalCount): Number {
            if (index == 0) return min
            if (index == count) return max
            return when (bounds.kind) {
                NumberKind.Long ->
                    (min.toLong().toULong() + bounds.step.toLong().toULong() * index.toULong())
                        .toLong()
                NumberKind.Int ->
                    (min.toInt().toLong() + bounds.step.toInt().toLong() * index).toInt()
                NumberKind.Float -> (min.toDouble() + bounds.step.toDouble() * index).toFloat()
                NumberKind.Double -> min.toDouble() + bounds.step.toDouble() * index
            }
        }
    }
}
