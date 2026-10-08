package top.ltfan.m3x.datastore.processor.util

/** `AppListSettings` -> `app_list_settings`, `UiSettingsModel` -> `ui_settings_model`. */
internal fun String.toSnakeCase(): String = buildString {
    this@toSnakeCase.forEachIndexed { index, char ->
        if (char.isUpperCase()) {
            val previous = this@toSnakeCase.getOrNull(index - 1)
            val next = this@toSnakeCase.getOrNull(index + 1)
            val boundary =
                index > 0 &&
                    previous != null &&
                    previous != '_' &&
                    (previous.isLowerCase() ||
                        previous.isDigit() ||
                        (previous.isUpperCase() && next != null && next.isLowerCase()))
            if (boundary) append('_')
            append(char.lowercaseChar())
        } else {
            append(char)
        }
    }
}

/** The store name: the explicit annotation value, or the snake_case class name. */
internal fun resolveStoreName(simpleName: String, explicit: String): String = explicit.ifEmpty {
    simpleName.toSnakeCase()
}

/** `withBlurEnabled` -> `blurEnabled`; `null` when the function is not named `withXxx`. */
internal fun derivedDefaultName(functionName: String): String? {
    if (!functionName.startsWith("with") || functionName.length == 4) return null
    return functionName.removePrefix("with").replaceFirstChar { it.lowercaseChar() }
}
