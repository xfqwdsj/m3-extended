package top.ltfan.m3x.material.theming

import top.ltfan.m3x.material.theme.ExpressiveCornerRadii
import top.ltfan.m3x.material.theme.M3xCornerRadii
import top.ltfan.m3x.material.theme.M3xShapes
import top.ltfan.m3x.material.theme.RoundedM3xShapeFactory

/** Builds the standard rounded preset from an explicit radius scale. */
public fun roundedShapes(radii: M3xCornerRadii = ExpressiveCornerRadii): M3xShapes =
    M3xShapes(cornerRadii = radii, factory = RoundedM3xShapeFactory)
