package dev.steshko.kwire.beans

import dev.steshko.kwire.BeanConfig
import dev.steshko.kwire.BeanConfigUser
import dev.steshko.kwire.BeanSource

class BeanConfigCompiler(
    override val fqName: String,
    override val name: String,
    override val source: BeanSource
) : BeanConfig {
    companion object {
        fun fromUser(beanConfigUser: BeanConfigUser): BeanConfigCompiler {
            return BeanConfigCompiler(
                fqName = beanConfigUser.fqName,
                name = beanConfigUser.name,
                source = beanConfigUser.source
            )
        }
    }
    var foundMatchingConstructor: Boolean = false
    val dependencies: MutableList<BeanDependency> = mutableListOf()
}

class BeanDependency {
    var resolved: Boolean = false
    var dependency: BeanConfig? = null
    var errorMessage: String? = null
}