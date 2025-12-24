package dev.steshko.kwire.fir

import dev.steshko.kwire.BeanSource
import dev.steshko.kwire.Inject
import dev.steshko.kwire.Named
import dev.steshko.kwire.beans.BeanConfigCompiler
import dev.steshko.kwire.beans.BeanDependency
import dev.steshko.kwire.util.getAnnotationFieldValue
import dev.steshko.kwire.util.getClassSymbolFromFqn
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.config.FirLanguageVersionSettingsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class KWireDependencyChecker(
    private val session: FirSession,
    private val beans: List<BeanConfigCompiler>
) : FirLanguageVersionSettingsChecker() {
    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    override fun check(
        context: CheckerContext,
        reporter: BaseDiagnosticsCollector.RawReporter
    ) {

        // Check that beans have unique names
        beans.groupBy {
            it.name
        }.filter {
            it.value.size > 1
        }.keys.takeIf {
            it.isNotEmpty()
        }?.run {
            reporter.report(
                message = "Duplicate names for kwire beans [${joinToString()}]",
                severity = CompilerMessageSeverity.ERROR
            )
        }

        // Check for beans that don't exist
        beans.filter {
            context.session.getClassSymbolFromFqn(it.fqName) == null
        }.takeIf {
            it.isNotEmpty()
        }?.run {
            reporter.report(
                message = "Missing dependencies for kwire beans [${joinToString()}]${System.lineSeparator()} Check build.gradle and kotlin compiler arguments",
                severity = CompilerMessageSeverity.ERROR
            )
        }

        // Check for no arg or @Inject constructors
        val gradlePluginBeansErrors = mutableListOf<String>()
        beans.forEach { bean ->
            val classSymbol: FirClassLikeSymbol<*> = context.session.getClassSymbolFromFqn(bean.fqName) ?: return@forEach
            //val constructors = classSymbol.getContainingDeclaration()
            val constructorDeclarations = (classSymbol.fir as FirClass).declarations.filter { declaration ->
                declaration is FirConstructor
            } as List<FirConstructor>
            val toUseConstructor = constructorDeclarations.find {
                it.getAnnotationByClassId(ClassId.topLevel(FqName(Inject::class.qualifiedName!!)), session) != null
            } ?: constructorDeclarations.find {
                it.valueParameters.isEmpty()
            }

            if (toUseConstructor == null) {
                if (bean.source == BeanSource.GRADLE_PLUGIN)
                    gradlePluginBeansErrors.add("No `@Inject` or no arg constructor found for ${bean.fqName}")
                return@forEach
            }
            bean.foundMatchingConstructor = true
            bean.dependencies.clear()
            toUseConstructor.valueParameters.forEach { valueParameter ->
                val dependency = BeanDependency()
                bean.dependencies.add(dependency)
                val nameValue = valueParameter.getAnnotationByClassId(
                    ClassId.topLevel(FqName(Named::class.qualifiedName!!)),
                    session
                )?.getAnnotationFieldValue(Named::value.name)
                val valueParameterTypeFqn = valueParameter.returnTypeRef.coneType.toString().replace("/", ".")
                if (nameValue != null) {
                    val namedBean = beans.find { it.name == nameValue }
                    if (namedBean == null) {
                        dependency.errorMessage = "Error injecting beans for ${bean.fqName}, @Named($nameValue) does not exist"
                    } else if (namedBean.fqName != valueParameterTypeFqn) {
                        //todo add inheritance check
                        dependency.errorMessage = "Error injecting beans for ${bean.fqName}, @Named($nameValue) is of wrong type: ${namedBean.fqName} expected: $valueParameterTypeFqn"
                    } else {
                        dependency.resolved = true
                        dependency.dependency = namedBean
                    }
                    return@forEach
                }

                //todo add inheritance check
                val possibleBeans = beans.filter { it.fqName == valueParameterTypeFqn }
                if (possibleBeans.isEmpty()) {
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
            gradlePluginBeansErrors.addAll(beans.filter { it.source == BeanSource.GRADLE_PLUGIN }.flatMap { it.dependencies }.mapNotNull { it.errorMessage })
            if (gradlePluginBeansErrors.isNotEmpty()) {
                reporter.report(
                    message = gradlePluginBeansErrors.toSet().joinToString("\n"),
                    severity = CompilerMessageSeverity.ERROR
                )
            }
        }
    }
}