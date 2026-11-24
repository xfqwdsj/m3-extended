package top.ltfan.m3x.settingspage

/** Result of accepting a setting into its local store; persistence is asynchronous. */
public sealed interface SettingsCommitResult {
    public data object AcceptedLocally : SettingsCommitResult

    public data object Unavailable : SettingsCommitResult

    public data object InvalidCandidateType : SettingsCommitResult

    public data class BuiltInIssue(val issue: SettingsValidation.Issue) : SettingsCommitResult

    public data class CustomIssue(val message: String) : SettingsCommitResult
}

/** One validation and commit path for generated and handwritten settings. */
public object SettingsCommit {
    public fun <T> tryCommit(item: SettingsItem.Item<T>, candidate: T): SettingsCommitResult =
        tryCommitDraft(item, candidate)

    /** Restores an item's declared default value through the normal validation and commit path. */
    public fun tryRestoreDefault(item: SettingsItem.Item<*>): SettingsCommitResult {
        if (!item.shouldShow || !item.enabled) return SettingsCommitResult.Unavailable
        if (item.resetMode != SettingsResetMode.Menu) return SettingsCommitResult.Unavailable
        val default = item.defaultValue ?: return SettingsCommitResult.Unavailable
        return tryCommitDraft(item, default, preserveSelectionValues = false)
    }

    private fun commit(item: SettingsItem.Item<Any?>, candidate: Any?): SettingsCommitResult {
        if (!item.shouldShow || !item.enabled) return SettingsCommitResult.Unavailable
        val builtIn = SettingsValidation.issueFor(item, candidate)
        if (builtIn != null) return SettingsCommitResult.BuiltInIssue(builtIn)
        val issue = item.issue(candidate)
        if (issue != null) return SettingsCommitResult.BuiltInIssue(issue)
        val custom = item.validate(candidate)
        if (custom != null) return SettingsCommitResult.CustomIssue(custom)
        item.value = candidate
        return SettingsCommitResult.AcceptedLocally
    }

    /** Boundary for heterogeneous editors whose draft is held as [Any]. */
    public fun tryCommitDraft(item: SettingsItem.Item<*>, candidate: Any?): SettingsCommitResult =
        tryCommitDraft(item, candidate, preserveSelectionValues = true)

    private fun tryCommitDraft(
        item: SettingsItem.Item<*>,
        candidate: Any?,
        preserveSelectionValues: Boolean,
    ): SettingsCommitResult {
        val canonical =
            when (item) {
                is SettingsItem.Item.Selector<*> -> {
                    val id =
                        (candidate as? SettingsValue<*>)?.id
                            ?: return SettingsCommitResult.InvalidCandidateType
                    val option =
                        item.options.singleOrNull { it.id == id }
                            ?: return SettingsCommitResult.InvalidCandidateType
                    if (!compatible(option.value, candidate.value))
                        return SettingsCommitResult.InvalidCandidateType
                    if (preserveSelectionValues) item.value.takeIf { it.id == id } ?: option
                    else candidate
                }

                is SettingsItem.Item.MultiSelector<*> -> {
                    val selected =
                        candidate as? Set<*> ?: return SettingsCommitResult.InvalidCandidateType
                    if (selected.any { it !is SettingsValue<*> })
                        return SettingsCommitResult.InvalidCandidateType
                    val ids = selected.map { (it as SettingsValue<*>).id }
                    if (ids.distinct().size != ids.size)
                        return SettingsCommitResult.InvalidCandidateType
                    val current = item.value.associateBy { it.id }
                    ids.map { id ->
                            val option =
                                item.options.singleOrNull { it.id == id }
                                    ?: return SettingsCommitResult.InvalidCandidateType
                            val incoming =
                                selected.single { (it as SettingsValue<*>).id == id }
                                    as SettingsValue<*>
                            if (!compatible(option.value, incoming.value))
                                return SettingsCommitResult.InvalidCandidateType
                            if (preserveSelectionValues) current[id] ?: option else incoming
                        }
                        .toSet()
                }

                else -> candidate
            }
        val typeMatches =
            when (item) {
                is SettingsItem.Item.SingleNumber<*> -> numericCandidate(canonical, item.type.kind)
                is SettingsItem.Item.NumberRange<*> -> {
                    val pair = canonical as? Pair<*, *>
                    pair != null &&
                        numericCandidate(pair.first, item.startType.kind) &&
                        numericCandidate(pair.second, item.endType.kind)
                }

                else -> compatible(item.value, canonical)
            }
        if (!typeMatches) return SettingsCommitResult.InvalidCandidateType
        val normalized =
            when (item) {
                is SettingsItem.Item.SingleNumber<*> ->
                    normalizeNumber(canonical as Number, item.type.kind)
                is SettingsItem.Item.NumberRange<*> -> {
                    val pair = canonical as Pair<*, *>
                    normalizeNumber(pair.first as Number, item.startType.kind) to
                        normalizeNumber(pair.second as Number, item.endType.kind)
                }

                else -> canonical
            }
        @Suppress("UNCHECKED_CAST")
        return commit(item as SettingsItem.Item<Any?>, normalized)
    }

    private fun compatible(current: Any?, candidate: Any?): Boolean =
        when (current) {
            null -> true
            is Int -> candidate is Int
            is Long -> candidate is Long
            is Float -> candidate is Float
            is Double -> candidate is Double
            is Pair<*, *> ->
                candidate is Pair<*, *> &&
                    compatible(current.first, candidate.first) &&
                    compatible(current.second, candidate.second)

            is Set<*> -> candidate is Set<*>
            is SettingsValue<*> -> candidate is SettingsValue<*>
            else -> candidate != null && candidate::class == current::class
        }

    private fun numericCandidate(candidate: Any?, kind: NumberKind): Boolean {
        val value = candidate as? Number ?: return false
        return when (kind) {
            NumberKind.Int ->
                value.toDouble().let {
                    it.isFinite() && it % 1.0 == 0.0 && it >= Int.MIN_VALUE && it <= Int.MAX_VALUE
                }

            NumberKind.Long -> value is Long
            NumberKind.Float,
            NumberKind.Double -> value.toDouble().isFinite()
        }
    }

    private fun normalizeNumber(value: Number, kind: NumberKind): Number =
        when (kind) {
            NumberKind.Int -> value.toInt()
            NumberKind.Long -> value.toLong()
            NumberKind.Float -> value.toFloat()
            NumberKind.Double -> value.toDouble()
        }
}
