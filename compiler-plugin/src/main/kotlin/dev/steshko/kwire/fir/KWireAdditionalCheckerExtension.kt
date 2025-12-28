package dev.steshko.kwire.fir

import dev.steshko.kwire.beans.GlobalBeanConfigInternal
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.LanguageVersionSettingsCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class KWireAdditionalCheckerExtension(
    session: FirSession,
    globalBeanConfig: GlobalBeanConfigInternal
) : FirAdditionalCheckersExtension(session) {

    override val languageVersionSettingsCheckers = object : LanguageVersionSettingsCheckers() {
        override val languageVersionSettingsCheckers = setOf(
            KWireDependencyChecker(session, globalBeanConfig)
        )
    }

    override val declarationCheckers = object : DeclarationCheckers() {
        override val classCheckers: Set<FirClassChecker> = setOf(
            BeanAnnotationClassChecker(globalBeanConfig)
        )

        override val functionCheckers: Set<FirFunctionChecker> = setOf(
            BeanAnnotationFunctionChecker(globalBeanConfig)
        )
    }
}