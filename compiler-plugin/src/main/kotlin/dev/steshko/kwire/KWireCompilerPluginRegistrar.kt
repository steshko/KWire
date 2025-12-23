package dev.steshko.kwire

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

@Suppress("unused")
class KWireCompilerPluginRegistrar: CompilerPluginRegistrar() {
    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        TODO("Register compiler extensions")
    }
}