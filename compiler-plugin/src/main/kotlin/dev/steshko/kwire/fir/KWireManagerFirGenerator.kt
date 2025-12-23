package dev.steshko.kwire.fir

import dev.steshko.kwire.KWireDeclarationKey
import dev.steshko.kwire.managerClassId
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.plugin.createTopLevelClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class KWireManagerFirGenerator(
    session: FirSession,
    private val beans: List<String>
) : FirDeclarationGenerationExtension(session) {
    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId != managerClassId) return super.generateTopLevelClassLikeDeclaration(classId)

        return createTopLevelClass(
            classId = managerClassId,
            key = KWireDeclarationKey,
            classKind = ClassKind.OBJECT
        ).symbol
    }

    @OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
    override fun getTopLevelClassIds(): Set<ClassId> {
        return setOf(managerClassId)
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
        return packageFqName == managerClassId.packageFqName
    }
}