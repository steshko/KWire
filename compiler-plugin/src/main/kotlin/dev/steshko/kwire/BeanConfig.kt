package dev.steshko.kwire

import kotlinx.serialization.Serializable

@Serializable
data class BeanConfig(
    val fqName: String,
    val name: String
)