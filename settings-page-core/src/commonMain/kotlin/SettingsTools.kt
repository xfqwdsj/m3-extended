package top.ltfan.m3x.settingspage

/** A settings tree and its optional, independent persistence status. */
public class SettingsSource(
    public val id: String,
    public val tree: SettingsTree,
    public val persistence: SettingsPersistence? = null,
)

/** Combines generated and handwritten trees while preserving each source. */
public class CompositeSettingsTree(public val sources: List<SettingsSource>) : SettingsTree {
    init {
        require(sources.map { it.id }.distinct().size == sources.size) {
            "Duplicate settings source ID"
        }
    }

    override val items: List<SettingsItem>
        get() {
            val all = sources.flatMap { it.tree.items }
            SettingsItems.requireUniqueIds(all)
            return all
        }
}

/** Tree traversal shared by renderers. */
public object SettingsItems {
    /** Checks item and option IDs, including dynamically supplied options. */
    public fun requireUniqueIds(roots: List<SettingsItem>) {
        val items = roots.flatMap(::all)
        val ids = items.flatMap { item ->
            when (item) {
                is SettingsItem.Item.Selector<*> -> listOf(item.id) + item.options.map { it.id }
                is SettingsItem.Item.MultiSelector<*> ->
                    listOf(item.id) + item.options.map { it.id }
                else -> listOf(item.id)
            }
        }
        require(ids.distinct().size == ids.size) { "Duplicate settings item or option ID" }
    }

    public fun all(item: SettingsItem): List<SettingsItem> =
        listOf(item) + item.children.orEmpty().flatMap(::all)

    public fun visibleGroups(item: SettingsItem): List<SettingsItem> {
        if (!item.shouldShow) return emptyList()
        return if (item is SettingsItem.Category)
            listOf(item) +
                item.children.filterIsInstance<SettingsItem.Category>().flatMap(::visibleGroups)
        else listOf(item)
    }

    public fun visibleEntries(item: SettingsItem): List<SettingsItem.Item<*>> {
        if (!item.shouldShow || item !is SettingsItem.Item<*>) return emptyList()
        return listOf(item) + item.children.orEmpty().flatMap(::visibleEntries)
    }
}

/** Identity operations for option collections. */
public object SettingsValues {
    public fun containsId(values: Collection<SettingsValue<*>>, id: String): Boolean = values.any {
        it.id == id
    }

    public fun selectedIds(values: Collection<SettingsValue<*>>): Set<String> =
        values.mapTo(mutableSetOf()) { it.id }

    public fun <T> withOption(
        values: Set<SettingsValue<T>>,
        option: SettingsValue<T>,
    ): Set<SettingsValue<T>> =
        values.filterNotTo(mutableSetOf()) { it.id == option.id }.apply { add(option) }

    public fun <T> withoutOption(values: Set<SettingsValue<T>>, id: String): Set<SettingsValue<T>> =
        values.filterNotTo(mutableSetOf()) { it.id == id }
}
