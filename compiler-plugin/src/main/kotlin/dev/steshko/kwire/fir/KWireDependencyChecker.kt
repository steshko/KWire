package dev.steshko.kwire.fir

import dev.steshko.kwire.BeanSource
import dev.steshko.kwire.beans.GlobalBeanConfigInternal
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.config.FirLanguageVersionSettingsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

class KWireDependencyChecker(
    private val session: FirSession,
    private val globalBeanConfig: GlobalBeanConfigInternal
) : FirLanguageVersionSettingsChecker() {
    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    override fun check(
        context: CheckerContext,
        reporter: BaseDiagnosticsCollector.RawReporter
    ) {

        // Check that gradle plugin defined beans have unique names
        globalBeanConfig.beans.filter {
            it.source == BeanSource.GRADLE_PLUGIN
        }.groupBy {
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
        globalBeanConfig.beans.filter {
            it.source == BeanSource.GRADLE_PLUGIN && context.session.getClassSymbolFromFqn(it.fqName) == null
        }.takeIf {
            it.isNotEmpty()
        }?.run {
            reporter.report(
                message = "Missing dependencies for kwire beans [${joinToString { it.name }}]${System.lineSeparator()} Check build.gradle and kotlin compiler arguments",
                severity = CompilerMessageSeverity.ERROR
            )
        }

        // Check for no arg or @Inject constructors
        val gradlePluginBeansErrors = mutableListOf<String>()
        globalBeanConfig.beans.filter {
            !it.foundMatchingConstructor
        }.forEach { bean ->
            val classSymbol: FirClassLikeSymbol<*> = context.session.getClassSymbolFromFqn(bean.fqName) ?: return@forEach

            val toUseConstructor = getToUseConstructor(classSymbol.fir as FirClass, session)

            if (toUseConstructor == null) {
                if (bean.source == BeanSource.GRADLE_PLUGIN)
                    gradlePluginBeansErrors.add("No `@Inject` or no arg constructor found for ${bean.fqName}")
                return@forEach
            }
            bean.foundMatchingConstructor = true
            bean.dependencies = mutableListOf()

            gradlePluginBeansErrors.addAll(globalBeanConfig.beans.filter { it.source == BeanSource.GRADLE_PLUGIN && it.dependencies != null }.flatMap { it.dependencies!! }.mapNotNull { it.errorMessage })
            if (gradlePluginBeansErrors.isNotEmpty()) {
                reporter.report(
                    message = gradlePluginBeansErrors.toSet().joinToString("\n"),
                    severity = CompilerMessageSeverity.ERROR
                )
            }
        }

        val circularDependencies = findAllCircularDependencies(globalBeanConfig.beans)
        if (circularDependencies.isNotEmpty()) {
            reporter.report(
                message = "Circular Dependencies Detected:\n" + circularDependencies.map { (beanName, cycle) ->
                    "'$beanName': ${cycle.joinToString(" -> ")}"
                }.joinToString("\n"),
                severity = CompilerMessageSeverity.ERROR
            )
        }
        globalBeanConfig.generatedBeanDependencies = true
    }
}