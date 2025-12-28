package dev.steshko.kwire.beans

import dev.steshko.kwire.Bean
import dev.steshko.kwire.BeanSource
import dev.steshko.kwire.fir.getAnnotationFieldValue
import dev.steshko.kwire.fir.getDefaultBeanName
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

data class GlobalBeanConfigInternal(
    val beans: MutableList<BeanConfigInternal>,
    var generatedBeans: Boolean = false,
    var generatedBeanDependencies: Boolean = false
) {
    fun generateBeans(session: FirSession): List<BeanConfigInternal> {
        if (generatedBeanDependencies) return beans
        beans.removeAll { it.source == BeanSource.ANNOTATION }
        session.predicateBasedProvider.getSymbolsByPredicate(
            LookupPredicate.AnnotatedWith(setOf(FqName(Bean::class.qualifiedName!!)))
        ).forEach { beanAnnotatedSymbol ->
            beanAnnotatedSymbol.lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
            val (beanCreationMethod, fqName) = when (beanAnnotatedSymbol) {
                is FirClassLikeSymbol<*> ->
                    BeanCreationMethod.CLASS_CONSTRUCTOR to beanAnnotatedSymbol.classId.asString().replace("/", ".")
                is FirNamedFunctionSymbol ->
                    BeanCreationMethod.TOP_LEVEL_FUNCTION to (beanAnnotatedSymbol.resolvedReturnTypeRef.coneType.classId?.asString()?.replace("/", ".") ?: return@forEach)
                else -> return@forEach
            }
            //if (beanAnnotatedClassSymbol !is FirClassLikeSymbol<*>) return@forEach

            val beanAnnotation = beanAnnotatedSymbol.getAnnotationByClassId(
                ClassId.topLevel(FqName(Bean::class.qualifiedName!!)),
                session
            ) ?: return@forEach

            val nameArgument = beanAnnotation.getAnnotationFieldValue(Bean::name.name)

            beans.add(BeanConfigInternal(
                name = if (nameArgument.isNullOrBlank()) getDefaultBeanName(beanAnnotatedSymbol)!! else nameArgument,
                fqName = fqName,
                source = BeanSource.ANNOTATION,
                beanCreationMethod = beanCreationMethod,
            ).apply {
                when (beanAnnotatedSymbol) {
                    is FirClassLikeSymbol -> originFqName = fqName
                    is FirFunctionSymbol ->
                        originFqName = beanAnnotatedSymbol.packageFqName().asString() + "." + beanAnnotatedSymbol.name

                }
            })
        }
        generatedBeans = true
        return beans
    }
}
