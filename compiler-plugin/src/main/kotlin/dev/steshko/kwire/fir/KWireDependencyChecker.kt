package dev.steshko.kwire.fir

import dev.steshko.kwire.BeanConfig
import dev.steshko.kwire.util.getClassSymbolFromFqn
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.analysis.checkers.config.FirLanguageVersionSettingsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext

class KWireDependencyChecker(
    private val beans: List<BeanConfig>
) : FirLanguageVersionSettingsChecker() {
    override fun check(
        context: CheckerContext,
        reporter: BaseDiagnosticsCollector.RawReporter
    ) {
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
    }
}