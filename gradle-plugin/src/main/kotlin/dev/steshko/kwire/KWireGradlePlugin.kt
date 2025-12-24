package dev.steshko.kwire

import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Suppress("unused")
open class KWireGradlePlugin : KotlinCompilerPluginSupportPlugin {
    @OptIn(ExperimentalEncodingApi::class)
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project


        kotlinCompilation.dependencies {
            implementation(BuildConfig.PLUGIN_LIBRARY_COORDINATES)
        }

        return project.provider {
            val extension = project.extensions.getByType(KWireGradleExtension::class.java)
            val beanList = extension.beans.get()

            val globalBeanConfig = GlobalBeanConfig(beanList)
            listOf(
                SubpluginOption(key = KWireCommandLineProcessor.BEANS_OPTION, value = Base64.encode(Json.encodeToString(globalBeanConfig).toByteArray()))
            )
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