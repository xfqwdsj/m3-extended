package top.ltfan.m3x.settingspage

import top.ltfan.m3x.resource.Resource

/** Presentation metadata. UI adapters decide how to render the optional icon resource. */
public interface SettingsResources {
    public val label: Resource<String>
    public val description: Resource<String?>?
        get() = null

    public val icon: Resource<*>?
        get() = null
}
