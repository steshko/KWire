package dev.steshko.kwire.ir

import dev.steshko.kwire.Bean
import dev.steshko.kwire.beans.BeanConfigInternal
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

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


internal fun topologicalSortBeans(
    properties: List<IrProperty>,
    beans: List<BeanConfigInternal>
): List<IrProperty> {
    val propertyMap = properties.associateBy { it.name.asString() }

    // Build dependency graph from BeanConfigInternal.dependencies
    val dependencies = mutableMapOf<String, Set<String>>()

    beans.forEach { bean ->
        dependencies[bean.name] = bean.dependencies
            ?.filter {  it.dependency != null }
            ?.map { it.dependency!!.name }
            ?.toSet()
            ?: emptySet()
    }

    // Kahn's algorithm for topological sort
    val inDegree = beans.associate { it.name to 0 }.toMutableMap()

    // Calculate in-degrees
    dependencies.values.forEach { deps ->
        deps.forEach { dep ->
            inDegree[dep] = inDegree[dep]!! + 1
        }
    }

    // Queue of beans with no dependencies
    val queue = ArrayDeque(
        inDegree.filter { it.value == 0 }.keys
    )

    // Process queue
    val sorted = mutableListOf<String>()
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        sorted.add(current)

        // For each bean that current depends on
        dependencies[current]?.forEach { dependency ->
            inDegree[dependency] = inDegree[dependency]!! - 1
            if (inDegree[dependency] == 0) {
                queue.add(dependency)
            }
        }
    }

    // Return properties in sorted order (dependencies first)
    return sorted.reversed().mapNotNull { propertyMap[it] }
}

fun IrDeclarationWithName.getBeanNameFromIrSymbol(): String? {
    val annotation = this.getAnnotation(FqName(Bean::class.qualifiedName!!)) ?: return null
    val defaultName by lazy {
        this.name.asString().replaceFirstChar { it.lowercase() }
    }
    val argument = annotation.arguments[0] ?: return defaultName // replace hardcoded 0
    return (argument as IrConst).value?.toString() ?: defaultName
}

fun String.fqnToIrType(context: IrPluginContext): IrType? {
    val classId = ClassId.topLevel(FqName(this))
    val classSymbol = context.referenceClass(classId) ?: return null
    return classSymbol.defaultType
}