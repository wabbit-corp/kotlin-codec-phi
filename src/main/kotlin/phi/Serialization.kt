@file:OptIn(ExperimentalSerializationApi::class)

package phi

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.reflect.KType

private const val DEBUG_ENCODER = false
private const val DEBUG_DECODER = false

private sealed class TreePhiCompositeEncoder(override val serializersModule: SerializersModule) : CompositeEncoder {
    abstract fun encodeElement(descriptor: SerialDescriptor, index: Int, tag: PhiNode)

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) = encodeElement(descriptor, index, PhiNode.Byte(if (value) 1 else 0))
    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) = encodeElement(descriptor, index, PhiNode.Byte(value))
    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) = encodeElement(descriptor, index, PhiNode.Short(value))
    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) = encodeElement(descriptor, index, PhiNode.String(value.toString()))
    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) = encodeElement(descriptor, index, PhiNode.Int(value))
    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) = encodeElement(descriptor, index, PhiNode.Float(value))
    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) = encodeElement(descriptor, index, PhiNode.Double(value))
    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) = encodeElement(descriptor, index, PhiNode.Long(value))
    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) = encodeElement(descriptor, index, PhiNode.String(value))
    override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder = TreePhiEncoder(serializersModule)

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        val encoder = TreePhiEncoder(serializersModule)
        encoder.encodeSerializableValue(serializer, value)
        encodeElement(descriptor, index, encoder.result!!)
    }
    @ExperimentalSerializationApi
    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (value == null) {
            encodeElement(descriptor, index, PhiNode.Null)
        } else {
            val encoder = TreePhiEncoder(serializersModule)
            encoder.encodeSerializableValue(serializer, value)
            encodeElement(descriptor, index, encoder.result!!)
        }
    }
}

private class TreePhiListEncoder(private val parent: TreePhiEncoder) : TreePhiCompositeEncoder(parent.module) {
    private val list: MutableList<PhiNode> = mutableListOf()
    override fun encodeElement(descriptor: SerialDescriptor, index: Int, tag: PhiNode) {
        if (DEBUG_ENCODER) println("NbtListEncoder.encodeElement: $descriptor, $index, $tag")
        list.add(tag)
    }
    override fun endStructure(descriptor: SerialDescriptor) {
        if (DEBUG_ENCODER) println("NbtListEncoder.endStructure: $descriptor")
        parent.result = PhiNode.List(list)
    }
}

private class TreePhiMapEncoder(private val parent: TreePhiEncoder) : TreePhiCompositeEncoder(parent.module) {
    private val map: MutableMap<PhiNode, PhiNode> = mutableMapOf()
    private var lastKey: PhiNode? = null
    override fun encodeElement(descriptor: SerialDescriptor, index: Int, tag: PhiNode) {
        if (DEBUG_ENCODER) println("NbtMapEncoder.encodeElement: $descriptor, $index, $tag")
        if (index % 2 == 0) {
            lastKey = tag
        } else {
            map[lastKey!!] = tag
        }
    }
    override fun endStructure(descriptor: SerialDescriptor) {
        if (DEBUG_ENCODER) println("NbtMapEncoder.endStructure: $descriptor")
        parent.result = PhiNode.Map(map)
    }
}

private class TreePhiClassEncoder(private val parent: TreePhiEncoder) : TreePhiCompositeEncoder(parent.module) {
    private val list: MutableList<PhiNode> = mutableListOf()
    override fun encodeElement(descriptor: SerialDescriptor, index: Int, tag: PhiNode) {
        if (DEBUG_ENCODER) println("NbtClassEncoder.encodeElement: $descriptor, $index, $tag")
        list.add(tag)
    }
    override fun endStructure(descriptor: SerialDescriptor) {
        if (DEBUG_ENCODER) println("NbtClassEncoder.endStructure: $descriptor")
        parent.result = PhiNode.Compound(
            null,
            list.mapIndexed { index, tag ->
                val phiIdAnnotation = descriptor.getElementAnnotations(index).firstOrNull { it is Phi.Id } as Phi.Id?
                val phiId = phiIdAnnotation?.id
                val name = descriptor.getElementName(index)
                val key = if (phiId != null) PhiNode.Key.Int(phiId) else PhiNode.Key.String(name)
                key to tag
            }.toMap()
        )
    }
}

private class TreePhiSealedEncoder(private val parent: TreePhiEncoder) : TreePhiCompositeEncoder(parent.module) {
    private var type: PhiNode.Key? = null

    @OptIn(ExperimentalSerializationApi::class)
    override fun encodeElement(descriptor: SerialDescriptor, index: Int, tag: PhiNode) {
        val sealedDescriptor = descriptor.getElementDescriptor(1)
        if (DEBUG_ENCODER) println("NbtSealedEncoder.encodeElement: $descriptor, $index [ ${descriptor.getElementName(index)} ], $tag")
        if (DEBUG_ENCODER) println("  annotations: ${sealedDescriptor.annotations}")

        if (index == 0) {
            val type = (tag as PhiNode.String).value
            val index = sealedDescriptor.getElementIndex(type)
            this.type = sealedDescriptor.keyP(index)
        }
        else if (index == 1) {
            if (DEBUG_ENCODER) println("NbtSealedEncoder.encodeElement.2: $type")
            val compound = tag as PhiNode.Compound
            check(compound.subtype == null)
            parent.result = compound.copy(subtype = type)
        }
    }
    override fun endStructure(descriptor: SerialDescriptor) {
        if (DEBUG_ENCODER) println("NbtSealedEncoder.endStructure: $descriptor")
    }
}

private class TreePhiObjectEncoder(private val parent: TreePhiEncoder) : TreePhiCompositeEncoder(parent.module) {
    // private val list: MutableList<Phi?> = mutableListOf()
    override fun encodeElement(descriptor: SerialDescriptor, index: Int, tag: PhiNode) {
        if (DEBUG_ENCODER) println("NbtObjectEncoder.encodeElement: $descriptor, $index, $tag")
        // list.add(tag)
        if (DEBUG_ENCODER) println("Unexpected element: ${descriptor.getElementName(index)}")
    }
    override fun endStructure(descriptor: SerialDescriptor) {
        if (DEBUG_ENCODER) println("NbtObjectEncoder.endStructure: $descriptor")
        // check(list.isEmpty())
        parent.result = PhiNode.Compound(null, emptyMap())
//        parent.result = compoundTagOf(*list.mapIndexed { index, tag ->
//            descriptor.getElementName(index) to tag!!
//        }.toTypedArray())
    }
}

internal class TreePhiEncoder(val module: SerializersModule) : Encoder {
    var result: PhiNode? = null
    private fun attach(tag: PhiNode) {
        result = tag
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (DEBUG_ENCODER) println("beginStructure: $descriptor")
        when (descriptor.kind) {
            StructureKind.CLASS    -> return TreePhiClassEncoder(this)
            StructureKind.LIST     -> return TreePhiListEncoder(this)
            PolymorphicKind.SEALED -> return TreePhiSealedEncoder(this)
            StructureKind.MAP      -> return TreePhiMapEncoder(this)
            StructureKind.OBJECT   -> return TreePhiObjectEncoder(this)

            PolymorphicKind.OPEN -> TODO()
            SerialKind.CONTEXTUAL -> TODO()
            is PrimitiveKind -> TODO()
            SerialKind.ENUM -> TODO()
        }
    }

    override val serializersModule: SerializersModule = module
    override fun encodeBoolean(value: Boolean) = attach(PhiNode.Byte(if (value) 1 else 0))
    override fun encodeByte(value: Byte)       = attach(PhiNode.Byte(value))
    override fun encodeChar(value: Char)       = attach(PhiNode.String(value.toString()))
    override fun encodeShort(value: Short)     = attach(PhiNode.Short(value))
    override fun encodeInt(value: Int)         = attach(PhiNode.Int(value))
    override fun encodeLong(value: Long)       = attach(PhiNode.Long(value))
    override fun encodeDouble(value: Double)   = attach(PhiNode.Double(value))
    override fun encodeFloat(value: Float)     = attach(PhiNode.Float(value))
    override fun encodeString(value: String)   = attach(PhiNode.String(value))
    @ExperimentalSerializationApi
    override fun encodeNull() = attach(PhiNode.Null)
    @OptIn(ExperimentalSerializationApi::class)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        val phiIdAnnotation = enumDescriptor.getElementAnnotations(index).firstOrNull { it is Phi.Id } as Phi.Id?
        val phiId = phiIdAnnotation?.id
        val name = enumDescriptor.getElementName(index)
        val key = if (phiId != null) PhiNode.Key.Int(phiId) else PhiNode.Key.String(name)
        attach(PhiNode.Compound(key, emptyMap()))
    }
    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this
}

sealed class TreePhiCompositeDecoder(override val serializersModule: SerializersModule) : CompositeDecoder
{
    abstract fun decodeElement(descriptor: SerialDescriptor, index: Int): PhiNode
    // override fun decodeElementIndex(descriptor: SerialDescriptor): Int
    // override fun endStructure(descriptor: SerialDescriptor)

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean =
        (decodeElement(descriptor, index) as PhiNode.Byte).value != 0.toByte()
    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte =
        (decodeElement(descriptor, index) as PhiNode.Byte).value
    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short =
        (decodeElement(descriptor, index) as PhiNode.Short).value
    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char =
        (decodeElement(descriptor, index) as PhiNode.String).value[0]
    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int =
        (decodeElement(descriptor, index) as PhiNode.Int).value
    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long =
        (decodeElement(descriptor, index) as PhiNode.Long).value
    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float =
        (decodeElement(descriptor, index) as PhiNode.Float).value
    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double =
        (decodeElement(descriptor, index) as PhiNode.Double).value
    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String =
        (decodeElement(descriptor, index) as PhiNode.String).value
    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder =
        TreePhiDecoder(serializersModule, decodeElement(descriptor, index))

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T {
        val decoder = TreePhiDecoder(serializersModule, decodeElement(descriptor, index))
        return decoder.decodeSerializableValue(deserializer)
    }

    @ExperimentalSerializationApi
    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        val tag = decodeElement(descriptor, index)
        if (tag is PhiNode.Null) return null
        val decoder = TreePhiDecoder(serializersModule, tag)
        return decoder.decodeSerializableValue(deserializer)
    }
}

class TreePhiListTagDecoder(
    override val serializersModule: SerializersModule,
    private val tag: PhiNode.List
) : TreePhiCompositeDecoder(serializersModule)
{
    private var index = 0

    override fun decodeElement(descriptor: SerialDescriptor, index: Int): PhiNode {
        return tag.value[index]
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (index < tag.value.size) index++ else CompositeDecoder.DECODE_DONE
    }

    override fun endStructure(descriptor: SerialDescriptor) { }
}

class TreePhiMapDecoder(
    override val serializersModule: SerializersModule,
    tag: PhiNode.Map
) : TreePhiCompositeDecoder(serializersModule)
{
    private val size = tag.value.size
    private var it = tag.value.iterator()
    private var entry: Map.Entry<PhiNode, PhiNode>? = null
    override fun decodeElement(descriptor: SerialDescriptor, index: Int): PhiNode {
        return if (index % 2 == 0) {
            entry = it.next()
            entry!!.key
        } else {
            entry!!.value
        }
    }
    private var index = 0
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (index < 2 * size) index++ else CompositeDecoder.DECODE_DONE
    }

    override fun endStructure(descriptor: SerialDescriptor) { }
}

class TreePhiSealedDecoder(
    override val serializersModule: SerializersModule,
    private val tag: PhiNode.Compound
) : TreePhiCompositeDecoder(serializersModule)
{
    override fun decodeElement(descriptor: SerialDescriptor, index: Int): PhiNode {
        if (DEBUG_DECODER) println("NbtSealedDecoder.decodeElement: $descriptor, $index")
        if (index == 0) {
            val key = tag.subtype
            check(key != null)
            val elementDescriptors = descriptor.getElementDescriptor(1)
            for (index in 0 until elementDescriptors.elementsCount) {
                val elKey = elementDescriptors.keyP(index)
                if (elKey != key) continue
                return PhiNode.String(elementDescriptors.getElementName(index))
            }
            error("No element found for key $key")
        } else {
            return tag.copy(subtype = null)
        }
    }

    private var index = 0
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (index < 2) index++ else CompositeDecoder.DECODE_DONE
    }

    override fun endStructure(descriptor: SerialDescriptor) { }
}

class TreePhiClassDecoder(
    override val serializersModule: SerializersModule,
    private val tag: PhiNode.Compound
) : TreePhiCompositeDecoder(serializersModule)
{
    private var index = 0
    private var fields: List<Pair<Int, PhiNode>>? = null

    private fun buildFields(descriptor: SerialDescriptor) {
        if (DEBUG_DECODER) println("buildFields: $descriptor")
        if (DEBUG_DECODER) println("  elementDescriptors: ${descriptor.elementDescriptors.toList()}")
        val keyMap = descriptor.elementDescriptors.withIndex().associate { (index, it) ->
            descriptor.key(index) to index
        }
        if (DEBUG_DECODER) println("buildFields: $keyMap")

        fields = tag.value.map { (key, value) ->
            val index = keyMap[key] ?: error("No field found for key $key")
            index to value
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (fields == null) {
            buildFields(descriptor)
        }

        if (index >= fields!!.size) return CompositeDecoder.DECODE_DONE
        val (index, value) = fields!![index]
        return index
    }

    override fun decodeElement(descriptor: SerialDescriptor, index: Int): PhiNode {
        val result = fields?.get(index)
        this.index += 1
        return result?.second ?: error("No field found")
    }

    override fun endStructure(descriptor: SerialDescriptor) { }
}

class TreePhiObjectDecoder(
    override val serializersModule: SerializersModule,
    private val tag: PhiNode.Compound
) : TreePhiCompositeDecoder(serializersModule)
{
    override fun decodeElement(descriptor: SerialDescriptor, index: Int): PhiNode {
        if (DEBUG_DECODER) println("NbtObjectDecoder.decodeElement: $descriptor, $index")
        TODO()
    }

    private var index = 0
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (DEBUG_DECODER) println("NbtObjectDecoder.decodeElementIndex: $descriptor")
        return if (index < descriptor.elementsCount) index++ else CompositeDecoder.DECODE_DONE
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (DEBUG_DECODER) println("NbtObjectDecoder.endStructure: $descriptor")
    }
}

class TreePhiDecoder(
    override val serializersModule: SerializersModule,
    private val tag: PhiNode
) : Decoder
{
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (DEBUG_DECODER) println("beginStructure: $descriptor")
        when (descriptor.kind) {
            StructureKind.CLASS -> {
                require(tag is PhiNode.Compound)
                return TreePhiClassDecoder(serializersModule, tag)
            }
            StructureKind.LIST -> {
                require(tag is PhiNode.List)
                return TreePhiListTagDecoder(serializersModule, tag)
            }
            PolymorphicKind.SEALED -> {
                require(tag is PhiNode.Compound)
                return TreePhiSealedDecoder(serializersModule, tag)
            }
            StructureKind.MAP -> {
                require(tag is PhiNode.Map)
                return TreePhiMapDecoder(serializersModule, tag)
            }
            StructureKind.OBJECT -> {
                require(tag is PhiNode.Compound)
                return TreePhiObjectDecoder(serializersModule, tag)
            }

            PolymorphicKind.OPEN -> TODO()
            SerialKind.CONTEXTUAL -> error("impossible")
            is PrimitiveKind -> error("impossible")
            SerialKind.ENUM -> error("impossible")
        }
    }
    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean = tag !is PhiNode.Null

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing? {
        if (tag is PhiNode.Null) return null
        error("Expected null tag, but got $tag")
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        TODO()
    }
    override fun decodeInline(descriptor: SerialDescriptor): Decoder = this
    override fun decodeBoolean() = (tag as PhiNode.Byte).value != 0.toByte()
    override fun decodeByte() = (tag as PhiNode.Byte).value
    override fun decodeChar() = (tag as PhiNode.String).value[0]
    override fun decodeShort() = (tag as PhiNode.Short).value
    override fun decodeInt() = (tag as PhiNode.Int).value
    override fun decodeLong() = (tag as PhiNode.Long).value
    override fun decodeFloat() = (tag as PhiNode.Float).value
    override fun decodeDouble() = (tag as PhiNode.Double).value
    override fun decodeString() = (tag as PhiNode.String).value
}
