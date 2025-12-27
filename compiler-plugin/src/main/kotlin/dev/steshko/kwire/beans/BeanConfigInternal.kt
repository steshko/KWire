package dev.steshko.kwire.beans

import dev.steshko.kwire.BeanConfig
import dev.steshko.kwire.BeanConfigUser
import dev.steshko.kwire.BeanSource
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class BeanConfigInternal(
    override val fqName: String,
    override val name: String,
    override val source: BeanSource
) : BeanConfig {
    companion object {
        fun fromUser(beanConfigUser: BeanConfigUser): BeanConfigInternal {
            return BeanConfigInternal(
                fqName = beanConfigUser.fqName,
                name = beanConfigUser.name,
                source = beanConfigUser.source
            )
        }
    }
    var foundMatchingConstructor: Boolean = false
    var dependencies: MutableList<BeanDependency>? = null

    val getClassId: ClassId
        get() {
            val split = fqName.split(".")
            return ClassId(
                packageFqName = FqName(split.dropLast(1).joinToString(".")),
                topLevelName = Name.identifier(split.last())
            )
        }
}

class BeanDependency {
    var resolved: Boolean = false
    var nullable: Boolean = false
    var dependency: BeanConfigInternal? = null
    var errorMessage: String? = null
}