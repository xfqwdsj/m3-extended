package top.ltfan.m3x.settingspage

/**
 * Current model and atomic transformation boundary used by storage-independent generated settings.
 */
public interface SettingsModel<T> {
    public val value: T

    public fun update(transform: (T) -> T)
}
