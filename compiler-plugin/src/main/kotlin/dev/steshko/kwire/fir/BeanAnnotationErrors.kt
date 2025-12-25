package dev.steshko.kwire.fir

import dev.steshko.kwire.Bean
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import javax.swing.UIManager.put

object BeanAnnotationErrors : BaseDiagnosticRendererFactory() {
    val UNKNOWN_BEAN_ERROR by error0<KtAnnotation>()
    val DUPLICATE_BEAN_NAME_ERROR by error1<KtAnnotation, String>()
    val BEAN_MISSING_VALID_CONSTRUCTOR_ERROR by error0<KtClassOrObject>()
    val BEAN_INJECTION_ERROR by error1<KtConstructor<*>, String>()

    override val MAP = KtDiagnosticFactoryToRendererMap(Bean::class.simpleName.toString()).apply {
        put(UNKNOWN_BEAN_ERROR, "Error Processing Bean Annotation")
        put(DUPLICATE_BEAN_NAME_ERROR, "Duplicate Bean Name")
        put(BEAN_MISSING_VALID_CONSTRUCTOR_ERROR, "Bean must have @Inject or no arg constructor")
    }

    init {
        RootDiagnosticRendererFactory.registerFactory(this)
    }
}