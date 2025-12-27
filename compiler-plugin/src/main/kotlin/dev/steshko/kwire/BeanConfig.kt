package dev.steshko.kwire

import kotlinx.serialization.Serializable

@Serializable
data class GlobalBeanConfig<T : BeanConfig>(
    val beans: MutableList<T>
)

interface BeanConfig {
    val name: String
    val fqName: String
    val source: BeanSource
}
@Serializable
data class BeanConfigUser(
    override val name: String,
    override val fqName: String,
    override val source: BeanSource
) : BeanConfig

@Serializable
enum class BeanSource {
    ANNOTATION, GRADLE_PLUGIN
}