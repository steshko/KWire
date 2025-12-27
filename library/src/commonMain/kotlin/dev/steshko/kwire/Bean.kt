package dev.steshko.kwire

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Bean(
    val name: String = ""
)
