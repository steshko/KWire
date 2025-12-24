plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
    implementation(project(":library"))
    implementation(libs.kotlinx.serialization.json)
}

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }

    packageName(group.toString())
    buildConfigField("String", "KOTLIN_COMPILER_PLUGIN_ID", "\"${rootProject.group}\"")
    buildConfigField("String", "KOTLIN_COMPILER_PLUGIN_GROUP", "\"${project.group}\"")
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}