package dev.steshko.kwire.util

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

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

fun FirAnnotation.getAnnotationFieldValue(field: String): String? =
    (this.argumentMapping.mapping[Name.identifier(field)] as? FirLiteralExpression)?.value as String?