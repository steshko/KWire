package dev.steshko.kwire.util

fun String.fqnToPropName(): String = this.split('.').last().replaceFirstChar { it.lowercase()}
