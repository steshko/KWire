package dev.steshko.kwire.ir

import dev.steshko.kwire.KWireDeclarationKey
import dev.steshko.kwire.managerClassId
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class KWireManagerIRVisitor(
    private val context: IrPluginContext
) : IrVisitorVoid() {

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration,
            is IrFile,
            is IrModuleFragment,
                -> element.acceptChildrenVoid(this)
            else -> Unit
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitClass(declaration: IrClass) {
        if (declaration.origin == GeneratedByPlugin(KWireDeclarationKey)) {
            if (declaration.classId == managerClassId && declaration.primaryConstructor == null)
                declaration.addDefaultPrimaryConstructor(context, DescriptorVisibilities.PRIVATE)
        }
        declaration.acceptChildrenVoid(this)
    }
}