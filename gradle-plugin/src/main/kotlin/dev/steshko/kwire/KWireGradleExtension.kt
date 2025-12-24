package dev.steshko.kwire

import dev.steshko.kwire.util.fqnToPropName
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

open class KWireGradleExtension @Inject constructor(
    objects: ObjectFactory
) {
    internal val beans: ListProperty<BeanConfigUser> = objects.listProperty(BeanConfigUser::class.java)

    /**
     * Register a bean by its fully qualified class name
     * Usage: bean("com.example.MyService")
     */
    fun bean(fqName: String, name: String = fqName.fqnToPropName()) {
        beans.add(BeanConfigUser(fqName, name, BeanSource.GRADLE_PLUGIN))
    }
}