package dev.steshko.kwire

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal object KWireDeclarationKey : GeneratedDeclarationKey() {
    override fun toString() = "kwire"
}

val managerClassId = ClassId(
    packageFqName = FqName(BuildConfig.KOTLIN_COMPILER_PLUGIN_GROUP),
    topLevelName = Name.identifier("KWireManager")
)