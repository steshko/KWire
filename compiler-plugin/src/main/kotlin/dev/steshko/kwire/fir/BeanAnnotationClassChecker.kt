package dev.steshko.kwire.fir

import dev.steshko.kwire.Bean
import dev.steshko.kwire.beans.GlobalBeanConfigInternal
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class BeanAnnotationClassChecker(
    private val globalBeanConfig: GlobalBeanConfigInternal
) : FirClassChecker(MppCheckerKind.Common) {
    @OptIn(DirectDeclarationsAccess::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        check(declaration, globalBeanConfig)
    }
}

class BeanAnnotationFunctionChecker(
    private val globalBeanConfig: GlobalBeanConfigInternal
) : FirFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        check(declaration, globalBeanConfig)
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
fun check(declaration: FirDeclaration, globalBeanConfig: GlobalBeanConfigInternal) {
    val beanAnnotation = declaration.getAnnotationByClassId(
        ClassId.topLevel(FqName(Bean::class.qualifiedName!!)),
        context.session
    )
    beanAnnotation ?: return

    val beans = when (globalBeanConfig.generatedBeans) {
        true -> globalBeanConfig.beans
        false -> globalBeanConfig.generateBeans(context.session)
    }
    val beanName = beanAnnotation.getAnnotationFieldValue(Bean::name.name) ?: getDefaultBeanName(declaration.symbol)
    val sameNamedBeans = beans.filter { it.name == beanName }
    val bean = when {
        sameNamedBeans.isEmpty() -> {
            reporter.reportOn(
                source = beanAnnotation.source,
                factory = BeanAnnotationErrors.UNKNOWN_BEAN_ERROR,
            )
            return
        }
        sameNamedBeans.size > 1 -> {
            reporter.reportOn(
                source = beanAnnotation.source,
                factory = BeanAnnotationErrors.DUPLICATE_BEAN_NAME_ERROR,
                "Duplicate Bean Name: $beanName"
            )
            /*sameNamedBeans.first {
                it.fqName == declaration.symbol.classId.toString().replace("/", ".")
            }*/
            return
        }
        else -> sameNamedBeans.first()
    }

    val toUseConstructor = when (declaration) {
        is FirClass -> {
            val toUseConstructor = getToUseConstructor(declaration, context.session)

            if (toUseConstructor == null) {
                reporter.reportOn(source = declaration.source, factory = BeanAnnotationErrors.BEAN_MISSING_VALID_CONSTRUCTOR_ERROR)
                return
            }

            toUseConstructor
        }
        else -> declaration
    }

    if (toUseConstructor is FirFunction) {
        validateConstructor(
            constructor = toUseConstructor,
            bean = bean,
            beans = beans,
            session = context.session
        )
    }

    bean.foundMatchingConstructor = true

    bean.dependencies?.forEach { dependency ->
        dependency.errorMessage?.let { errorMessage ->
            reporter.reportOn(
                source = toUseConstructor.source,
                factory = BeanAnnotationErrors.BEAN_INJECTION_ERROR,
                errorMessage
            )
        }
    }
}