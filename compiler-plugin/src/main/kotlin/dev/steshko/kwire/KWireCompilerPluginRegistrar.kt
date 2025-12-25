package dev.steshko.kwire

import dev.steshko.kwire.fir.KWireAdditionalCheckerExtension
import dev.steshko.kwire.fir.KWireManagerFirGenerator
import dev.steshko.kwire.ir.KWireManagerIrGenerator
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@Suppress("unused")
class KWireCompilerPluginRegistrar: CompilerPluginRegistrar() {
    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val globalBeanConfig = configuration.get(KWireCommandLineProcessor.BEANS_CONFIGURATION_KEY, GlobalBeanConfig(mutableListOf()))
        val beans = globalBeanConfig.beans

        FirExtensionRegistrarAdapter.registerExtension(object : FirExtensionRegistrar() {
            override fun ExtensionRegistrarContext.configurePlugin() {
                +{ session: FirSession -> KWireManagerFirGenerator(session, beans) }
                +{ session: FirSession -> KWireAdditionalCheckerExtension(session, beans) }
            }
        })

        IrGenerationExtension.registerExtension(
            KWireManagerIrGenerator(beans)
        )
    }
}