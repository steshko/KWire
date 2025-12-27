package dev.steshko.kwire.fir

import dev.steshko.kwire.Inject
import dev.steshko.kwire.Named
import dev.steshko.kwire.beans.BeanConfigInternal
import dev.steshko.kwire.beans.BeanDependency
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * @return [FirConstructor] annotated with @Inject or noarg constructor if exist
 */
@OptIn(DirectDeclarationsAccess::class)
fun getToUseConstructor(firClass: FirClass, session: FirSession): FirConstructor? {
    val functionDeclarations = firClass.declarations.filter { declaration ->
        declaration is FirConstructor
    } as List<FirConstructor>

    return functionDeclarations.find {
        it.getAnnotationByClassId(ClassId.topLevel(FqName(Inject::class.qualifiedName!!)), session) != null
    } ?: functionDeclarations.find {
        it.valueParameters.isEmpty()
    }
}
/**
 * Returns error message for @Inject constructor
 */
fun validateConstructor(
    constructor: FirFunction,
    bean: BeanConfigInternal,
    beans: List<BeanConfigInternal>,
    session: FirSession
) {
    bean.dependencies = mutableListOf()
    constructor.valueParameters.forEach { valueParameter ->
        val dependency = BeanDependency()
        bean.dependencies!!.add(dependency)
        val nameValue = valueParameter.getAnnotationByClassId(
            ClassId.topLevel(FqName(Named::class.qualifiedName!!)),
            session
        )?.getAnnotationFieldValue(Named::value.name)
        val valueParameterConeType = valueParameter.returnTypeRef.coneType
        var valueParameterTypeFqn = valueParameterConeType.toString().replace("/", ".")
        if (valueParameterTypeFqn.endsWith("?")) {
            dependency.nullable = true
            valueParameterTypeFqn = valueParameterTypeFqn.dropLast(1)
        }
        if (nameValue != null) {
            val namedBean = beans.find { it.name == nameValue }
            if (namedBean == null) {
                if (dependency.nullable) return@forEach
                dependency.errorMessage = "Error injecting beans for ${bean.fqName}, no beans with name '$nameValue' found"
            } else if (namedBean.fqName != valueParameterTypeFqn) {
                dependency.errorMessage = "Error injecting beans for ${bean.fqName}, @Named($nameValue) is of wrong type: ${namedBean.fqName} expected: $valueParameterTypeFqn"
            } else {
                dependency.resolved = true
                dependency.dependency = namedBean
            }
            return@forEach
        }

        val possibleBeans = beans.filter { isValidTypeForParameter(
            fqName = it.fqName,
            parameterType = valueParameterConeType,
            session = session
        ) }
        if (possibleBeans.isEmpty()) {
            if (dependency.nullable) return@forEach
            dependency.errorMessage = "Error injecting beans for ${bean.fqName}, no beans of type $valueParameterTypeFqn"
            return@forEach
        } else if (possibleBeans.size > 1) {
            dependency.errorMessage = "Error injecting beans for ${bean.fqName}, multiple beans of type $valueParameterTypeFqn found, [${
                possibleBeans.joinToString(
                    ", "
                ) { it.name }
            }] specify which one to use using @Named()"
            return@forEach
        }
        dependency.resolved = true
        dependency.dependency = possibleBeans.first()
    }
}

fun findAllCircularDependencies(beans: List<BeanConfigInternal>): Map<String, List<String>> {
    val circularDeps = mutableMapOf<String, List<String>>()
    val globalVisited = mutableSetOf<String>()

    fun detectCycle(current: BeanConfigInternal, path: MutableList<String>): List<String>? {
        val currentName = current.name

        if (currentName in path) {
            val cycleStartIndex = path.indexOf(currentName)
            return path.subList(cycleStartIndex, path.size) + listOf(currentName)
        }

        if (currentName in globalVisited) return null

        path.add(currentName)

        current.dependencies?.forEach { dep ->
            if (dep.dependency is BeanConfigInternal) {
                val cycle = detectCycle(dep.dependency as BeanConfigInternal, path)
                if (cycle != null) {
                    path.removeAt(path.lastIndex)
                    return cycle
                }
            }
        }

        path.removeAt(path.lastIndex)
        globalVisited.add(currentName)

        return null
    }

    beans.forEach { bean ->
        if (bean.name !in globalVisited) {
            val cycle = detectCycle(bean, mutableListOf())
            if (cycle != null) {
                circularDeps[bean.name] = cycle
            }
        }
    }

    return circularDeps
}