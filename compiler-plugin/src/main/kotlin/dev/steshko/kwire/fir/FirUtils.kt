package dev.steshko.kwire.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker

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

fun isValidTypeForParameter(
    fqName: String,
    parameterType: ConeKotlinType,
    session: FirSession
): Boolean {
    val classId = ClassId.topLevel(FqName(fqName))

    val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirClassSymbol ?: return false

    val candidateType = classSymbol.defaultType()

    return AbstractTypeChecker.isSubtypeOf(
        context = session.typeContext,
        subType = candidateType,
        superType = parameterType
    )
}
