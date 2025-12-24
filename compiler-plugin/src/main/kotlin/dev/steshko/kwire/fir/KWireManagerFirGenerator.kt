package dev.steshko.kwire.fir

import dev.steshko.kwire.BeanConfig
import dev.steshko.kwire.KWireDeclarationKey
import dev.steshko.kwire.managerClassId
import dev.steshko.kwire.util.getClassSymbolFromFqn
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createTopLevelClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class KWireManagerFirGenerator(
    session: FirSession,
    private val beans: List<BeanConfig>
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

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()


        return listOf(
            createMemberProperty(
                owner = owner,
                key = KWireDeclarationKey,
                name = callableId.callableName,
                returnType = session.getClassSymbolFromFqn(beans.find { it.name == callableId.callableName.identifier }!!.fqName)!!.constructType() //TODO fix
            ).symbol
        )
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return if (classSymbol.classId == managerClassId) {
            beans.map {
                Name.identifier(it.name)
            }.toSet()
        } else {
            emptySet()
        }
    }

    @OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
    override fun getTopLevelClassIds(): Set<ClassId> {
        return setOf(managerClassId)
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
        return packageFqName == managerClassId.packageFqName
    }
}