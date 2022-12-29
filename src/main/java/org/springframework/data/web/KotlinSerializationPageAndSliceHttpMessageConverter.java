package org.springframework.data.web;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializationException;
import kotlinx.serialization.descriptors.PolymorphicKind;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.json.Json;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.serializer.DomainSerializerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class KotlinSerializationPageAndSliceHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {
    private final Json format;

    public KotlinSerializationPageAndSliceHttpMessageConverter(MediaType... supportedMediaTypes) {
        this(Json.Default, supportedMediaTypes);
    }

    public KotlinSerializationPageAndSliceHttpMessageConverter(Json format, MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
        this.format = format;
    }


    @Override
    protected boolean supports(Class<?> clazz) {
        return this.serializer(clazz) != null;
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        KSerializer<Object> serializer = this.serializer(clazz);
        if (serializer == null) {
            throw new HttpMessageNotReadableException("Could not find KSerializer for " + clazz, inputMessage);
        } else {
            Charset charset = charset(inputMessage.getHeaders().getContentType());
            String s = StreamUtils.copyToString(inputMessage.getBody(), charset);

            try {
                return format.decodeFromString(serializer, s);
            } catch (SerializationException var7) {
                throw new HttpMessageNotReadableException("Could not read " + format + ": " + var7.getMessage(), var7, inputMessage);
            }
        }
    }

    @Override
    public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
        return this.serializer(GenericTypeResolver.resolveType(type, contextClass)) != null && this.canRead(mediaType);
    }

    @Override
    public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        Type resolvedType = GenericTypeResolver.resolveType(type, contextClass);
        KSerializer<Object> serializer = this.serializer(resolvedType);
        if (serializer == null) {
            throw new HttpMessageNotReadableException("Could not find KSerializer for " + resolvedType, inputMessage);
        } else {
            Charset charset = charset(inputMessage.getHeaders().getContentType());
            String s = StreamUtils.copyToString(inputMessage.getBody(), charset);

            try {
                return format.decodeFromString(serializer, s);
            } catch (SerializationException var7) {
                throw new HttpMessageNotReadableException("Could not read " + format + ": " + var7.getMessage(), var7, inputMessage);
            }
        }
    }

    @Override
    public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
        return this.serializer(type != null ? GenericTypeResolver.resolveType(type, clazz) : clazz) != null && this.canWrite(mediaType);
    }

    @Override
    protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        Type resolvedType = type != null ? type : object.getClass();
        KSerializer<Object> serializer = this.serializer(resolvedType);
        if (serializer == null) {
            throw new HttpMessageNotWritableException("Could not find KSerializer for " + resolvedType);
        } else {
            try {
                String s = format.encodeToString(serializer, object);
                Charset charset = charset(outputMessage.getHeaders().getContentType());
                outputMessage.getBody().write(s.getBytes(charset));
                outputMessage.getBody().flush();
            } catch (SerializationException var7) {
                throw new HttpMessageNotWritableException("Could not write " + format + ": " + var7.getMessage(), var7);
            }
        }
    }

    @Nullable
    private KSerializer<Object> serializer(Type type) {
        KSerializer<Object> serializer = null;
        try {
            serializer = DomainSerializerFactory.INSTANCE.createSerializer(type);
        } catch (IllegalArgumentException ignored) {
        }

        if (serializer != null) {
            if (this.hasPolymorphism(serializer.getDescriptor(), new HashSet())) {
                return null;
            }
        }

        return serializer;
    }

    private boolean hasPolymorphism(SerialDescriptor descriptor, Set<String> alreadyProcessed) {
        alreadyProcessed.add(descriptor.getSerialName());
        if (descriptor.getKind().equals(PolymorphicKind.OPEN.INSTANCE)) {
            return true;
        } else {
            for(int i = 0; i < descriptor.getElementsCount(); ++i) {
                SerialDescriptor elementDescriptor = descriptor.getElementDescriptor(i);
                if (!alreadyProcessed.contains(elementDescriptor.getSerialName()) && this.hasPolymorphism(elementDescriptor, alreadyProcessed)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static Charset charset(@Nullable MediaType contentType) {
        return contentType != null && contentType.getCharset() != null ? contentType.getCharset() : StandardCharsets.UTF_8;
    }
}
