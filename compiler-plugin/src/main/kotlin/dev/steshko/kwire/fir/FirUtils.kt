package dev.steshko.kwire.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

fun getDefaultBeanName(declaration: FirBasedSymbol<*>): String? = when (declaration) {
    is FirClassLikeSymbol -> declaration.classId.shortClassName.toString().replaceFirstChar { it.lowercase() }
    is FirFunctionSymbol -> declaration.name.asString().replaceFirstChar { it.lowercase() }
    else -> null
}

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
