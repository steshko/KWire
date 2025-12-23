package dev.steshko.kwire

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

open class KWireGradleExtension @Inject constructor(
    objects: ObjectFactory
) {
    internal val beans: ListProperty<String> = objects.listProperty(String::class.java)

    /**
     * Register a bean by its fully qualified class name
     * Usage: bean("com.example.MyService")
     */
    fun bean(className: String) {
        beans.add(className)
    }
}