package org.springframework.data.web

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.PolymorphicKind.OPEN
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializerOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.Slice
import org.springframework.data.domain.serializer.PageSerializer
import org.springframework.data.domain.serializer.SliceSerializer
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractGenericHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * {@link HttpMessageConverter} implementation to enable kotlinx.Serialization of Page<T>, Slice<T> interface
 *
 * @author meloning
 */
class KotlinSerializationPageAndSliceHttpMessageConverter(
    private val format: Json,
    private val kotlinSerializationJsonHttpMessageConverter: KotlinSerializationJsonHttpMessageConverter
) : AbstractGenericHttpMessageConverter<Any>() {
    override fun canRead(type: Type, contextClass: Class<*>?, mediaType: MediaType?): Boolean {
        return kotlinSerializationJsonHttpMessageConverter.canRead(type, contextClass, mediaType)
    }

    override fun canWrite(type: Type?, clazz: Class<*>, mediaType: MediaType?): Boolean {
        return kotlinSerializationJsonHttpMessageConverter.canWrite(type, clazz, mediaType)
    }

    override fun read(type: Type, contextClass: Class<*>?, inputMessage: HttpInputMessage): Any {
        return kotlinSerializationJsonHttpMessageConverter.read(type, contextClass, inputMessage)
    }

    override fun supports(clazz: Class<*>): Boolean {
        return serializer(clazz) != null
    }

    override fun readInternal(clazz: Class<out Any>, inputMessage: HttpInputMessage): Any {
        return kotlinSerializationJsonHttpMessageConverter.read(clazz, inputMessage)
    }

    override fun writeInternal(any: Any, outputMessage: HttpOutputMessage) {
        writeInternal(any, null, outputMessage)
    }

    override fun writeInternal(any: Any, type: Type?, outputMessage: HttpOutputMessage) {
        val resolvedType = type ?: any.javaClass
        val serializer = serializer(resolvedType)
                ?: throw HttpMessageNotWritableException("Could not find KSerializer for $resolvedType")
        try {
            val s: String = format.encodeToString(serializer, any)
            val charset = charset(outputMessage.headers.contentType)
            outputMessage.body.write(s.toByteArray(charset))
            outputMessage.body.flush()
        } catch (ex: SerializationException) {
            throw HttpMessageNotWritableException("Could not write " + format + ": " + ex.message, ex)
        }
    }

    private fun charset(contentType: MediaType?): Charset {
        return if (contentType != null && contentType.charset != null) {
            contentType.charset!!
        } else StandardCharsets.UTF_8
    }

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalSerializationApi::class)
    private fun serializer(type: Type): KSerializer<Any>? {
        val serializer = try {
            when(type) {
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
        } catch (_: IllegalArgumentException) {
            null
        }
        if (serializer != null) {
            if (this.hasPolymorphism(serializer.descriptor, HashSet())) {
                return null
            }
        }
        return serializer
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun hasPolymorphism(descriptor: SerialDescriptor, alreadyProcessed: MutableSet<String>): Boolean {
        alreadyProcessed.add(descriptor.serialName)
        if (descriptor.kind == OPEN) {
            return true
        }
        for (i in 0 until descriptor.elementsCount) {
            val elementDescriptor = descriptor.getElementDescriptor(i)
            if (!alreadyProcessed.contains(elementDescriptor.serialName) && hasPolymorphism(elementDescriptor, alreadyProcessed)) {
                return true
            }
        }
        return false
    }
}
