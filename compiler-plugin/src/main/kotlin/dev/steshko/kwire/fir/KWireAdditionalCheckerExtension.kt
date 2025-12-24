package dev.steshko.kwire.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.LanguageVersionSettingsCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class KWireAdditionalCheckerExtension(
    session: FirSession,
    beans: List<String>
) : FirAdditionalCheckersExtension(session) {

    override val languageVersionSettingsCheckers = object : LanguageVersionSettingsCheckers() {
        override val languageVersionSettingsCheckers = setOf(
            KWireDependencyChecker(beans)
        )
    }

}