plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
}

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
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