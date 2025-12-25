package dev.steshko.kwire.ir

import dev.steshko.kwire.beans.BeanConfigInternal
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class KWireManagerIrGenerator(val beans: List<BeanConfigInternal>) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val visitor = KWireManagerIRVisitor(beans, pluginContext)
        moduleFragment.acceptVoid(visitor)
    }
}