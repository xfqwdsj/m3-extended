// Tests use the complete rendering bundle, including its transitive Compose resources.
// Reuse its processed resource output, as native macOS packaging does.
tasks
    .withType<Copy>()
    .matching { it.name.startsWith("copyTestComposeResourcesFor") }
    .configureEach {
        val targetName =
            name.removePrefix("copyTestComposeResourcesFor").replaceFirstChar(Char::lowercaseChar)
        from(project(":material-bundle").tasks.named("${targetName}ProcessResources")) {
            include("composeResources/**")
            // The test module's main resources may also occur in the complete bundle.
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
