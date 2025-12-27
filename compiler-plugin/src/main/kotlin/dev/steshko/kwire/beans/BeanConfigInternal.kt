package dev.steshko.kwire.beans

import dev.steshko.kwire.BeanConfig
import dev.steshko.kwire.BeanConfigUser
import dev.steshko.kwire.BeanSource
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data class BeanConfigInternal(
    override val name: String,
    override val fqName: String,
    override val source: BeanSource,
    var beanCreationMethod: BeanCreationMethod = BeanCreationMethod.UNDETERMINED,
    var foundMatchingConstructor: Boolean = false
) : BeanConfig {
    companion object {
        fun fromUser(beanConfigUser: BeanConfigUser): BeanConfigInternal {
            return BeanConfigInternal(
                name = beanConfigUser.name,
                fqName = beanConfigUser.fqName,
                source = beanConfigUser.source
            )
        }
    }

    /**
     * Fully Qualified name of the class/function/property to get bean
     */
    var originFqName: String = fqName
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

enum class BeanCreationMethod {
    UNDETERMINED, CLASS_CONSTRUCTOR, TOP_LEVEL_FUNCTION
}
class BeanDependency {
    var resolved: Boolean = false
    var nullable: Boolean = false
    var dependency: BeanConfigInternal? = null
    var errorMessage: String? = null
}