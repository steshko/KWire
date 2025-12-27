package dev.steshko.kwire.ir

import dev.steshko.kwire.Inject
import dev.steshko.kwire.KWireDeclarationKey
import dev.steshko.kwire.beans.BeanConfigInternal
import dev.steshko.kwire.beans.BeanCreationMethod
import dev.steshko.kwire.managerClassId
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class KWireManagerIRVisitor(
    private val beans: List<BeanConfigInternal>,
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
        if (declaration.origin == GeneratedByPlugin(KWireDeclarationKey) && declaration.classId == managerClassId) {
            if (declaration.primaryConstructor == null)
                declaration.addDefaultPrimaryConstructor(context, DescriptorVisibilities.PRIVATE)

            val declarationBuilder = DeclarationIrBuilder(context, declaration.symbol)

            val beanProperties = declaration.properties
                .filter { beans.any { bean -> bean.name == it.name.toString() } }
                .toList()

            val sortedBeanProperties = topologicalSortBeans(beanProperties, beans)
            declaration.declarations.removeAll(beanProperties)
            sortedBeanProperties.forEach { property ->
                declaration.declarations.add(property)
                if (property.origin != GeneratedByPlugin(KWireDeclarationKey)) return@forEach
                val bean = beans.find { it.name == property.name.toString() } ?: error("Bean not found: ${property.name}")

                val constructorToUse: IrFunctionSymbol = when (bean.beanCreationMethod) {
                    BeanCreationMethod.CLASS_CONSTRUCTOR -> {
                        val beanReferenceConstructors = context.referenceConstructors(bean.getClassId)

                        beanReferenceConstructors.find {
                            it.owner.getAnnotation(FqName(Inject::class.qualifiedName.toString())) != null
                        } ?: beanReferenceConstructors.find {
                            it.owner.parameters.isEmpty()
                        } ?: error("Constructor not found for creating bean: ${bean.name}")
                    }
                    BeanCreationMethod.TOP_LEVEL_FUNCTION -> {
                        val originPath = bean.originFqName.split(".")
                        context.referenceFunctions(CallableId(
                            FqName(originPath.dropLast(1).joinToString(".")),
                            className = null,
                            Name.identifier(originPath.last())
                        )).first { it.owner.getBeanNameFromIrSymbol() == bean.name }
                    }
                    BeanCreationMethod.UNDETERMINED -> error("bean creation method is undetermined")
                }


                property.backingField?.apply {
                    initializer = context.irFactory.createExpressionBody(
                        expression = declarationBuilder.irCall(
                            callee = constructorToUse
                        ).apply {
                            constructorToUse.owner.parameters.forEachIndexed { index, parameter ->
                                val beanDependency = bean.dependencies?.getOrNull(index) ?: error("Dependency $index not found for bean ${bean.name}")
                                if (beanDependency.nullable && beanDependency.dependency == null) {
                                    arguments[index] = IrConstImpl.constNull(
                                        startOffset = UNDEFINED_OFFSET,
                                        endOffset = UNDEFINED_OFFSET,
                                        type = context.irBuiltIns.nothingNType
                                    )
                                    return@forEachIndexed
                                }
                                val dependencyBean = beanDependency.dependency ?: error("Dependency not found for bean ${bean.name}")
                                if (parameter.type.classFqName.toString() != dependencyBean.fqName) error("Error injecting value parameters for bean: ${bean.name}, dependency bean type mismatch, expected ${parameter.type.classFqName} actual: ${dependencyBean.fqName}")
                                val dependencyProperty = declaration.properties.find {
                                    it.name.toString() == dependencyBean.name
                                } ?: error("Dependency property not found for bean ${bean.name}")
                                arguments[index] = declarationBuilder.irCall(
                                    dependencyProperty.getter!!
                                ).apply {
                                    origin = IrStatementOrigin.GET_PROPERTY
                                    dispatchReceiver = declarationBuilder.irGet(declaration.thisReceiver!!).apply {
                                        origin = IrStatementOrigin.IMPLICIT_ARGUMENT
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
        declaration.acceptChildrenVoid(this)
    }
}