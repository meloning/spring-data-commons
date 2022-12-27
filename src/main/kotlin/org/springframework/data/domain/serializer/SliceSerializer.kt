package org.springframework.data.domain.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Slice

class SliceSerializer<T>(
    private val dataSerializer: KSerializer<List<T>>
) : KSerializer<Slice<T>> {
    override val descriptor: SerialDescriptor = dataSerializer.descriptor
    override fun serialize(encoder: Encoder, value: Slice<T>) = dataSerializer.serialize(encoder, value.content)
    override fun deserialize(decoder: Decoder) = PageImpl(dataSerializer.deserialize(decoder))
}
