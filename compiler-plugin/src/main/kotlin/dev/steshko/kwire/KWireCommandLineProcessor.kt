package dev.steshko.kwire

import dev.steshko.kwire.beans.BeanConfigInternal
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.nio.charset.Charset
import kotlin.io.encoding.Base64

@Suppress("unused")
class KWireCommandLineProcessor : CommandLineProcessor {

    companion object {
        const val BEANS_OPTION = "beans"
        val BEANS_CONFIGURATION_KEY = CompilerConfigurationKey<GlobalBeanConfig<BeanConfigInternal>>("registered beans")
    }

    override val pluginId: String = BuildConfig.KOTLIN_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = BEANS_OPTION,
            valueDescription = "comma separated list of fully qualified bean class names",
            description = "Bean classes to register",
            required = false
        )
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            BEANS_OPTION -> configuration.put(
                BEANS_CONFIGURATION_KEY,
                GlobalBeanConfig(
                    beans = Json.decodeFromString<GlobalBeanConfig<BeanConfigUser>>(
                        Base64.decode(value).toString(Charset.defaultCharset())
                    ).beans.map(BeanConfigInternal::fromUser).toMutableList()
                )
            )
            else -> error("Unexpected config option: '${option.optionName}'")
        }
    }
}