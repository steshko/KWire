package dev.steshko.kwire.fir

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.analysis.checkers.config.FirLanguageVersionSettingsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class KWireDependencyChecker(
    private val beans: List<String>
) : FirLanguageVersionSettingsChecker() {
    override fun check(
        context: CheckerContext,
        reporter: BaseDiagnosticsCollector.RawReporter
    ) {
        beans.filterNot {
            val classId = ClassId.topLevel(FqName(it))
            listOf(
                context.session.symbolProvider,
                context.session.dependenciesSymbolProvider
            ).any { provider ->
                provider.getClassLikeSymbolByClassId(classId) != null
            }
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