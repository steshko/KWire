package dev.steshko.kwire.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.primaryConstructor

internal fun IrClass.addDefaultPrimaryConstructor(
    context: IrPluginContext,
    visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
) {
    addConstructor {
        isPrimary = true
        this.visibility = visibility
        returnType = this@addDefaultPrimaryConstructor.defaultType
    }.also { constructor ->
        // Create constructor body with super() call
        val anyConstructor = context.irBuiltIns.anyClass.owner.primaryConstructor ?: return
        val irBuilder = DeclarationIrBuilder(context, this@addDefaultPrimaryConstructor.symbol)

        constructor.body = irBuilder.irBlockBody {
            +irDelegatingConstructorCall(anyConstructor)
            +IrInstanceInitializerCallImpl(
                startOffset, endOffset,
                classSymbol = this@addDefaultPrimaryConstructor.symbol,
                type = context.irBuiltIns.unitType,
            )
        }
    }
}