package dev.steshko.kwire

import kotlinx.serialization.Serializable

@Serializable
data class GlobalBeanConfig(
    val beans: MutableList<BeanConfig>
)
@Serializable
data class BeanConfig(
    val fqName: String,
    val name: String,
    val source: BeanSource
)

@Serializable
enum class BeanSource {
    ANNOTATION, GRADLE_PLUGIN
}