package dev.steshko.kwire.util

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

fun String.fqnToPropName(): String = this.split('.').last().replaceFirstChar { it.lowercase()}

fun FirSession.getClassSymbolFromFqn(fqn: String): FirClassLikeSymbol<*>? {
    val classId = ClassId.topLevel(FqName(fqn))
    listOf(
        this.symbolProvider,
        this.dependenciesSymbolProvider
    ).forEach { provider ->
        provider.getClassLikeSymbolByClassId(classId)?.let { return it }
    }
    return null
}