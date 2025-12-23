package dev.steshko.kwire

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@Suppress("unused")
open class KWireGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        return project.provider {
            val extension = project.extensions.getByType(KWireGradleExtension::class.java)

            emptyList()
        }
    }

    override fun getCompilerPluginId() = BuildConfig.KOTLIN_COMPILER_PLUGIN_ID

    /**
     * Provides the Maven coordinates for the compiler plugin JAR that Gradle should
     * load and apply during Kotlin compilation. The JAR must contain a CommandLineProcessor
     * in META-INF.services whose plugin ID matches [getCompilerPluginId].
     */
    override fun getPluginArtifact() = SubpluginArtifact(
        groupId = BuildConfig.KOTLIN_COMPILER_PLUGIN_GROUP,
        artifactId = BuildConfig.KOTLIN_COMPILER_PLUGIN_NAME,
        version = BuildConfig.KOTLIN_COMPILER_PLUGIN_VERSION,
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) = true

    /**
     * Registers the 'kwire { }' configuration block that users can use in their build.gradle files
     * to configure dependency injection settings.
     */
    override fun apply(target: Project) {
        target.extensions.create("kwire", KWireGradleExtension::class.java)
    }
}