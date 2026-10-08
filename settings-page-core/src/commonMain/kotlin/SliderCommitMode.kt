package top.ltfan.m3x.settingspage

/** When inline numeric sliders validate and commit their preview to the settings model. */
public enum class SliderCommitMode {
    /** Keeps a local preview during the gesture and commits its final value on release. */
    OnRelease,
    /** Commits each changed value during dragging or keyboard adjustment for live previews. */
    WhileDragging,
}
