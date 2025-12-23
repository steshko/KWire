plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":compiler-plugin"))
    implementation(libs.kotlin.gradle.plugin.api)
}

buildConfig {
    packageName(project.group.toString())

    buildConfigField("String", "KOTLIN_COMPILER_PLUGIN_ID", "\"${rootProject.group}\"")

    val compilerPluginProject = project(":compiler-plugin")
    buildConfigField("String", "KOTLIN_COMPILER_PLUGIN_GROUP", "\"${compilerPluginProject.group}\"")
    buildConfigField("String", "KOTLIN_COMPILER_PLUGIN_NAME", "\"${compilerPluginProject.name}\"")
    buildConfigField("String", "KOTLIN_COMPILER_PLUGIN_VERSION", "\"${compilerPluginProject.version}\"")
}

gradlePlugin {
    plugins {
        create("kwire") {
            id = rootProject.group.toString()
            implementationClass = "dev.steshko.kwire.KWireGradlePlugin"
            displayName = "Kotlin compile time dependency injection plugin"
            description = "Kotlin compile time dependency injection plugin"
        }
    }
}