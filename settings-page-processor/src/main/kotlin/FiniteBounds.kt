package top.ltfan.m3x.settingspage.processor

import top.ltfan.m3x.settingspage.NumberKind
import top.ltfan.m3x.settingspage.SettingsNumbers

/** Annotation bounds must be checked in their declared type, especially Long. */
internal fun validFiniteBounds(min: Number, max: Number, step: Number, kind: NumberKind): Boolean =
    min.toDouble().isFinite() &&
        max.toDouble().isFinite() &&
        SettingsNumbers.compare(min, max, kind) <= 0 &&
        step.toDouble().isFinite() &&
        step.toDouble() > 0.0
