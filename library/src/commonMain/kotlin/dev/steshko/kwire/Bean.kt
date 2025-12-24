package dev.steshko.kwire

@Target(AnnotationTarget.CLASS)
annotation class Bean(
    val name: String = ""
)
