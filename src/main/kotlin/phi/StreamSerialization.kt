@file:OptIn(ExperimentalSerializationApi::class)

package phi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.*

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.key(index: Int): PhiNode.Key {
    val phiIdAnnotation = getElementAnnotations(index).firstOrNull { it is Phi.Id } as Phi.Id?
    val phiId = phiIdAnnotation?.id
    return if (phiId != null) PhiNode.Key.Int(phiId)
    else PhiNode.Key.String(getElementName(index))
}
@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.keyP(index: Int): PhiNode.Key {
    val phiIdAnnotation = getElementDescriptor(index).annotations.firstOrNull { it is Phi.Id } as Phi.Id?
    val phiId = phiIdAnnotation?.id
    return if (phiId != null) PhiNode.Key.Int(phiId)
    else PhiNode.Key.String(getElementName(index))
}
@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.key(): PhiNode.Key {
    val phiIdAnnotation = annotations.firstOrNull { it is Phi.Id } as Phi.Id?
    val phiId = phiIdAnnotation?.id
    return if (phiId != null) PhiNode.Key.Int(phiId)
    else PhiNode.Key.String(serialName)
}

//internal fun SerialDescriptor.asPhiType(): PhiType {
//    return when (kind) {
//        is PrimitiveKind -> {
//            val kind = kind as PrimitiveKind
//            when (kind) {
//                PrimitiveKind.BOOLEAN -> PhiType.Byte
//                PrimitiveKind.BYTE    -> PhiType.Byte
//                PrimitiveKind.SHORT   -> PhiType.Short
//                PrimitiveKind.CHAR    -> PhiType.String
//                PrimitiveKind.INT     -> PhiType.Int
//                PrimitiveKind.FLOAT   -> PhiType.Float
//                PrimitiveKind.DOUBLE  -> PhiType.Double
//                PrimitiveKind.LONG    -> PhiType.Long
//                PrimitiveKind.STRING  -> PhiType.String
//            }
//        }
//
//        StructureKind.LIST -> {
//            val elementDescriptor = getElementDescriptor(0)
//            val elementType = elementDescriptor.asPhiType()
//            PhiType.List(null, elementType)
//        }
//        StructureKind.MAP -> {
//            val keyDescriptor = getElementDescriptor(0)
//            val valueDescriptor = getElementDescriptor(1)
//            val keyType = keyDescriptor.asPhiType()
//            val valueType = valueDescriptor.asPhiType()
//            PhiType.Map(null, keyType, valueType)
//        }
//        SerialKind.ENUM -> {
//            // enums are represented as sealed classes
//            println("SerialKind.ENUM: $this")
//            TODO()
//        }
//
//        PolymorphicKind.SEALED -> {
//            val descriptor = getElementDescriptor(1)
//            val subtypes = mutableMapOf<Phi.Key, Map<Phi.Key, PhiType>>()
//            for (subtype in descriptor.elementDescriptors) {
//                val key = subtype.key()
//                val value = subtype.asPhiType()
//                check(value is PhiType.Object)
//                subtypes[key] = value.fields
//            }
//            return PhiType.Choice(subtypes)
//        }
//        StructureKind.CLASS    -> {
//            if (this.isInline) {
//                return elementDescriptors.first().asPhiType()
//            }
//            // val keys = (0 until elementsCount).map { key(it) }
//            // println("StructureKind.CLASS: $this, $keys")
//            val fields = mutableMapOf<Phi.Key, PhiType>()
//            for (index in 0 until elementsCount) {
//                val key = key(index)
//                val value = getElementDescriptor(index).asPhiType()
//                fields[key] = value
//            }
//            PhiType.Object(fields)
//        }
//        StructureKind.OBJECT   -> TODO()
//
//        PolymorphicKind.OPEN -> TODO()
//        SerialKind.CONTEXTUAL -> TODO()
//    }
//}

//sealed class StreamPhiCompositeEncoder(override val serializersModule: SerializersModule) : CompositeEncoder {
//    abstract fun encodeElement(descriptor: SerialDescriptor, index: Int, tag: Phi)
//
//    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
//        encodeElement(descriptor, index, Phi.Byte(if (value) 1 else 0))
//    }
//    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
//        encodeElement(descriptor, index, Phi.Byte(value))
//    }
//    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
//        encodeElement(descriptor, index, Phi.Short(value))
//    }
//    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
//        encodeElement(descriptor, index, Phi.String(value.toString()))
//    }
//    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
//        encodeElement(descriptor, index, Phi.Int(value))
//    }
//    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
//        encodeElement(descriptor, index, Phi.Float(value))
//    }
//    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
//        encodeElement(descriptor, index, Phi.Double(value))
//    }
//    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
//        encodeElement(descriptor, index, Phi.Long(value))
//    }
//    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
//        encodeElement(descriptor, index, Phi.String(value))
//    }
//    override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
//        return TreePhiEncoder(serializersModule)
//    }
//    override fun <T> encodeSerializableElement(
//        descriptor: SerialDescriptor,
//        index: Int,
//        serializer: SerializationStrategy<T>,
//        value: T
//    ) {
//        val encoder = TreePhiEncoder(serializersModule)
//        encoder.encodeSerializableValue(serializer, value)
//        encodeElement(descriptor, index, encoder.result!!)
//    }
//    @ExperimentalSerializationApi
//    override fun <T : Any> encodeNullableSerializableElement(
//        descriptor: SerialDescriptor,
//        index: Int,
//        serializer: SerializationStrategy<T>,
//        value: T?
//    ) {
//        if (value == null) {
//            encodeElement(descriptor, index, Phi.Null)
//        } else {
//            val encoder = TreePhiEncoder(serializersModule)
//            encoder.encodeSerializableValue(serializer, value)
//            encodeElement(descriptor, index, encoder.result!!)
//        }
//    }
//}
//
//class StreamPhiListEncoder(private val parent: StreamPhiEncoder) : TreePhiCompositeEncoder(parent.module) {
//    private val list: MutableList<Phi> = mutableListOf()
//    override fun encodeElement(descriptor: SerialDescriptor, index: Int, tag: Phi) {
//        println("NbtListEncoder.encodeElement: $descriptor, $index, $tag")
//        list.add(tag)
//    }
//    override fun endStructure(descriptor: SerialDescriptor) {
//        println("NbtListEncoder.endStructure: $descriptor")
//        parent.result = Phi.List(list)
//    }
//}
//
//class StreamPhiMapEncoder(private val parent: StreamPhiEncoder) : TreePhiCompositeEncoder(parent.module) {
//    private val map: MutableMap<Phi, Phi> = mutableMapOf()
//    private var lastKey: Phi? = null
//    override fun encodeElement(descriptor: SerialDescriptor, index: Int, tag: Phi) {
//        println("NbtMapEncoder.encodeElement: $descriptor, $index, $tag")
//        if (index % 2 == 0) {
//            lastKey = tag
//        } else {
//            map[lastKey!!] = tag
//        }
//    }
//    override fun endStructure(descriptor: SerialDescriptor) {
//        println("NbtMapEncoder.endStructure: $descriptor")
//        parent.result = Phi.Map(map)
//    }
//}
//
//class StreamPhiClassEncoder(private val parent: StreamPhiEncoder) : TreePhiCompositeEncoder(parent.module) {
//    private val list: MutableList<Phi> = mutableListOf()
//    override fun encodeElement(descriptor: SerialDescriptor, index: Int, tag: Phi) {
//        println("NbtClassEncoder.encodeElement: $descriptor, $index, $tag")
//        list.add(tag)
//    }
//    override fun endStructure(descriptor: SerialDescriptor) {
//        println("NbtClassEncoder.endStructure: $descriptor")
//        parent.result = Phi.Compound(
//            null,
//            list.mapIndexed { index, tag ->
//                val phiIdAnnotation = descriptor.getElementAnnotations(index).firstOrNull { it is Phi.Id } as Phi.Id?
//                val phiId = phiIdAnnotation?.id
//                val name = descriptor.getElementName(index)
//                val key = if (phiId != null) Phi.Key.Int(phiId) else Phi.Key.String(name)
//                key to tag
//            }.toMap()
//        )
//    }
//}
//
//class StreamPhiSealedEncoder(private val parent: StreamPhiEncoder) : TreePhiCompositeEncoder(parent.module) {
//    private var type: Phi.Key? = null
//
//    @OptIn(ExperimentalSerializationApi::class)
//    override fun encodeElement(descriptor: SerialDescriptor, index: Int, tag: Phi) {
//        val sealedDescriptor = descriptor.getElementDescriptor(1).getElementDescriptor(1)
//        println("NbtSealedEncoder.encodeElement: $descriptor, $index [ ${descriptor.getElementName(index)} ], $tag")
//        println("  annotations: ${sealedDescriptor.annotations}")
//
//        if (index == 0) {
//            val type = (tag as Phi.String).value
//            val phiKeyAnnotation = sealedDescriptor.annotations.firstOrNull { it is Phi.Id } as Phi.Id?
//            val phiId = phiKeyAnnotation?.id
//            this.type = if (phiId != null) Phi.Key.Int(phiId) else Phi.Key.String(type)
//        }
//        else if (index == 1) {
//            println("NbtSealedEncoder.encodeElement.2: $type")
//            val compound = tag as Phi.Compound
//            check(compound.subtype == null)
//            parent.result = compound.copy(subtype = type)
//        }
//    }
//    override fun endStructure(descriptor: SerialDescriptor) {
//        println("NbtSealedEncoder.endStructure: $descriptor")
//    }
//}
//
//class StreamPhiObjectEncoder(private val parent: StreamPhiEncoder) : TreePhiCompositeEncoder(parent.module) {
//    // private val list: MutableList<Phi?> = mutableListOf()
//    override fun encodeElement(descriptor: SerialDescriptor, index: Int, tag: Phi) {
//        println("NbtObjectEncoder.encodeElement: $descriptor, $index, $tag")
//        // list.add(tag)
//        System.err.println("Unexpected element: ${descriptor.getElementName(index)}")
//    }
//    override fun endStructure(descriptor: SerialDescriptor) {
//        println("NbtObjectEncoder.endStructure: $descriptor")
//        // check(list.isEmpty())
//        parent.result = Phi.Compound(null, emptyMap())
////        parent.result = compoundTagOf(*list.mapIndexed { index, tag ->
////            descriptor.getElementName(index) to tag!!
////        }.toTypedArray())
//    }
//}
//
//class StreamPhiEncoder(val module: SerializersModule, val writer: Writer, val bound: PhiType) : Encoder {
//    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
//        println("beginStructure: $descriptor")
//        when (descriptor.kind) {
//            StructureKind.CLASS    -> return StreamPhiClassEncoder(this)
//            StructureKind.LIST     -> return StreamPhiListEncoder(this)
//            PolymorphicKind.SEALED -> return StreamPhiSealedEncoder(this)
//            StructureKind.MAP      -> return StreamPhiMapEncoder(this)
//            StructureKind.OBJECT   -> return StreamPhiObjectEncoder(this)
//
//            PolymorphicKind.OPEN -> TODO()
//            SerialKind.CONTEXTUAL -> TODO()
//            is PrimitiveKind -> TODO()
//            SerialKind.ENUM -> TODO()
//        }
//    }
//
//    override val serializersModule: SerializersModule = module
//
//    override fun encodeBoolean(value: Boolean) = writer.writeValue(if (value) 1.toByte() else 0.toByte(), bound)
//    override fun encodeByte(value: Byte)       = writer.writeValue(value, bound)
//    override fun encodeChar(value: Char)       = writer.writeValue(value.toString(), bound)
//    override fun encodeShort(value: Short)     = writer.writeValue(value, bound)
//    override fun encodeInt(value: Int)         = writer.writeValue(value, bound)
//    override fun encodeLong(value: Long)       = writer.writeValue(value, bound)
//    override fun encodeDouble(value: Double)   = writer.writeValue(value, bound)
//    override fun encodeFloat(value: Float)     = writer.writeValue(value, bound)
//    override fun encodeString(value: String)   = writer.writeValue(value, bound)
//    override fun encodeNull() = writer.writeValue(null, bound)
//    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
//        writer.writeValue(Phi.Compound(enumDescriptor.key(index), emptyMap()), bound)
//    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this
//}
