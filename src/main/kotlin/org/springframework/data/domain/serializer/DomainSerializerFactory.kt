package org.springframework.data.domain.serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.serializerOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.Slice
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

object DomainSerializerFactory {
    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalSerializationApi::class)
    fun createSerializer(type: Type): KSerializer<Any>? {
        return when(type) {
            is ParameterizedType -> {
                val rootClass = (type.rawType as Class<*>)
                val args = (type.actualTypeArguments)
                val argsSerializers = args.map { serializerOrNull(it) }
                if (argsSerializers.isEmpty() && argsSerializers.first() == null) return null
                when {
                    Slice::class.java.isAssignableFrom(rootClass) ->
                        SliceSerializer(ListSerializer(argsSerializers.first()!!.nullable)) as KSerializer<Any>?
                    Page::class.java.isAssignableFrom(rootClass) ->
                        PageSerializer(ListSerializer(argsSerializers.first()!!.nullable)) as KSerializer<Any>?
                    else -> null
                }
            }
            else -> serializerOrNull(type)
        }
    }
}