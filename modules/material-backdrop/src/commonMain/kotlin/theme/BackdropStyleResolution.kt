package top.ltfan.m3x.material.backdrop.theme

import top.ltfan.m3x.backdrop.BackdropStyle

/** Resolves role overrides against receiving defaults while retaining delayed producers. */
internal fun BackdropStyle.resolveAgainst(parent: BackdropStyle): BackdropStyle =
    BackdropStyle.Default {
        colorControls = this@resolveAgainst.colorControls ?: parent.colorControls ?: colorControls
        blur = this@resolveAgainst.blur ?: parent.blur ?: blur
        vibrancy = this@resolveAgainst.vibrancy ?: parent.vibrancy ?: vibrancy
        lens = this@resolveAgainst.lens ?: parent.lens ?: lens
        fill = this@resolveAgainst.fill ?: parent.fill ?: fill
        highlight = this@resolveAgainst.highlight ?: parent.highlight ?: highlight
        shadow = this@resolveAgainst.shadow ?: parent.shadow ?: shadow
        innerShadow = this@resolveAgainst.innerShadow ?: parent.innerShadow ?: innerShadow
        interactionLighting =
            this@resolveAgainst.interactionLighting
                ?: parent.interactionLighting
                ?: interactionLighting
        insets = this@resolveAgainst.insets ?: parent.insets ?: insets
    }
