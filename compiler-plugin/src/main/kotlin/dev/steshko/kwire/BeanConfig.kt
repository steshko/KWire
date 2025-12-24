package dev.steshko.kwire

import kotlinx.serialization.Serializable

@Serializable
data class GlobalBeanConfig<T : BeanConfig>(
    val beans: MutableList<T>
)

interface BeanConfig {
    val fqName: String
    val name: String
    val source: BeanSource
}
@Serializable
data class BeanConfigUser(
    override val fqName: String,
    override val name: String,
    override val source: BeanSource
) : BeanConfig

@Serializable
enum class BeanSource {
    ANNOTATION, GRADLE_PLUGIN
}