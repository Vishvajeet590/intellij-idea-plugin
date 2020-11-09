plugins {
    // Java support
    id("java")
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "0.6.2"
}

// Import variables from gradle.properties file
val pluginGroup: String by project
// `pluginName_` variable ends with `_` because of the collision with Kotlin magic getter in the `intellij` closure.
// Read more about the issue: https://github.com/JetBrains/intellij-platform-plugin-template/issues/29
val pluginName_: String by project
val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project
val pluginVerifierIdeVersions: String by project

val forgeVersion: String by project

val platformType: String by project
val platformVersion: String by project
val platformPlugins: String by project
val platformDownloadSources: String by project

val addons: Configuration by configurations.creating
val addonsDir by extra("addon-repository")

group = pluginGroup
version = pluginVersion

// Configure project's dependencies
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(enforcedPlatform("org.jboss.forge:forge-bom:${forgeVersion}"))
    implementation("com.miglayout:miglayout-swing:4.2")
    implementation("org.jboss.logmanager:jboss-logmanager:1.4.1.Final")
    implementation("org.jboss.forge.furnace:furnace-se")
    implementation("org.jboss.forge.addon:facets-api")
    implementation("org.jboss.forge.addon:resources-api") {
        exclude("org.jboss.forge.addon", "facets-impl")
    }
    implementation("org.jboss.forge.addon:environment-api")
    implementation("org.jboss.forge.addon:convert-api")
    implementation("org.jboss.forge.addon:projects-api")
    implementation("org.jboss.forge.addon:ui-api") {
        exclude("org.jboss.forge.addon", "facets-impl")
        exclude("org.jboss.forge.addon", "environment-impl")
        exclude("org.jboss.forge.addon", "facets")
        exclude("org.jboss.forge.addon", "environment")
    }

    // Contains the core addons Forge needs
    addons("org.jboss.forge", "forge-distribution", forgeVersion, null, "offline", "zip")
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    // This creates the plugin using the ID as the base name
    pluginName = pluginGroup
    version = platformVersion
    type = platformType
    downloadSources = platformDownloadSources.toBoolean()
    updateSinceUntilBuild = true

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    setPlugins(*platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty).toTypedArray())
}

tasks {
    val extractAddons by creating(Sync::class) {
        dependsOn(addons)
        // Unzip only the addons directory
        from(zipTree(addons.asPath)) {
            include("forge-distribution-${forgeVersion}/addons/**")
            eachFile {
                relativePath = RelativePath(true, *relativePath.segments.drop(2).toTypedArray())
            }
            includeEmptyDirs = false
        }
        into("$buildDir/$addonsDir")
    }

//    buildSearchableOptions {
//        enabled = false
//    }

    // Set the compatibility versions to 1.8
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    buildPlugin {
        // Generate the addon-repository when the JAR is built
        dependsOn("extractAddons")
        from("$buildDir/$addonsDir"){
            into(addonsDir)
        }
    }

    prepareSandbox {
        dependsOn("extractAddons")
        from("$buildDir/$addonsDir"){
            into("$pluginName/$addonsDir")
        }
    }

    patchPluginXml {
        version(pluginVersion)
        sinceBuild(pluginSinceBuild)
        untilBuild(pluginUntilBuild)
        changeNotes("Bundled with Forge $forgeVersion");
    }

    runPluginVerifier {
        ideVersions(pluginVerifierIdeVersions)
    }

    publishPlugin {
        token(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://jetbrains.org/intellij/sdk/docs/tutorials/build_system/deployment.html#specifying-a-release-channel
        channels(pluginVersion.split('-').getOrElse(1) { "default" }.split('.').first())
    }
}
