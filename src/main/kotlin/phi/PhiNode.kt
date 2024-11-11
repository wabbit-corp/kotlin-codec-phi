@file:OptIn(ExperimentalSerializationApi::class)

package phi

import kotlinx.serialization.*
import kotlinx.serialization.Transient
import kotlinx.serialization.modules.SerializersModule
import one.wabbit.formatting.escapeJavaString
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private typealias KByte = kotlin.Byte
private typealias KShort = kotlin.Short
private typealias KInt = kotlin.Int
private typealias KLong = kotlin.Long
private typealias KFloat = kotlin.Float
private typealias KDouble = kotlin.Double
private typealias KString = kotlin.String
private typealias KList<A> = kotlin.collections.List<A>
private typealias KMap<A, B> = kotlin.collections.Map<A, B>
private typealias KAny = kotlin.Any

@Serializable sealed abstract class PhiNode {
    abstract val type: PhiType

    @Serializable sealed interface Key : Comparable<Key> {
        override fun compareTo(other: Key): kotlin.Int {
            when (this) {
                is Int -> {
                    if (other is Int) return this.value.compareTo(other.value)
                    return -1
                }
                is String -> {
                    if (other is String) return this.value.compareTo(other.value)
                    return 1
                }
            }
        }

        data class Int(val value: KInt) : Key {
            init {
                require(value >= 0) { "Negative key: $value" }
            }
            override fun toString(): kotlin.String = "#$value"
        }
        data class String(val value: KString) : Key {
            override fun toString(): kotlin.String = value
        }
    }

    @Serializable data object Null : PhiNode() {
        override val type: PhiType.Null = PhiType.Null
        override fun toString(): kotlin.String = "null"
    }
    @Serializable data class Byte(val value: KByte) : PhiNode() {
        override val type: PhiType.Byte = PhiType.Byte
        override fun toString(): kotlin.String = "${value}b"
    }
    @Serializable data class Short(val value: KShort) : PhiNode() {
        override val type: PhiType.Short = PhiType.Short
        override fun toString(): kotlin.String = "${value}s"
    }
    @Serializable data class Int(val value: KInt) : PhiNode() {
        override val type: PhiType.Int = PhiType.Int
        override fun toString(): kotlin.String = value.toString()
    }
    @Serializable data class Long(val value: KLong) : PhiNode() {
        override val type: PhiType.Long = PhiType.Long
        override fun toString(): kotlin.String = "${value}l"
    }
    @Serializable data class Float(val value: KFloat) : PhiNode() {
        override val type: PhiType.Float = PhiType.Float
        override fun toString(): kotlin.String = "${value}f"
    }
    @Serializable data class Double(val value: KDouble) : PhiNode() {
        override val type: PhiType.Double = PhiType.Double
        override fun toString(): kotlin.String = "${value}d"
    }
    @Serializable data class String(val value: KString) : PhiNode() {
        override val type: PhiType.String = PhiType.String
        override fun toString(): kotlin.String = "\"${escapeJavaString(value, doubleQuoted = true)}\""
    }
    @Serializable data class List(val value: KList<PhiNode>) : PhiNode() {
        constructor(vararg value: PhiNode) : this(value.toList())

        override val type: PhiType.List = run {
            var type: PhiType = PhiType.Nothing
            for (element in value)
                type = type union element.type
            PhiType.List(value.size.toUInt(), type)
        }

        override fun toString(): kotlin.String = value.joinToString(prefix = "[", postfix = "]")
    }
    @Serializable data class Map(val value: KMap<PhiNode, PhiNode>) : PhiNode() {
        constructor(vararg value: Pair<PhiNode, PhiNode>) : this(linkedMapOf(*value))

        override val type: PhiType.Map = run {
            var keyType: PhiType = PhiType.Nothing
            var targetType: PhiType = PhiType.Nothing
            for ((key, element) in value) {
                keyType = keyType union key.type
                targetType = targetType union element.type
            }
            PhiType.Map(value.size.toUInt(), keyType, targetType)
        }

        override fun toString(): kotlin.String =
            value.entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "$key: $value" }
    }
    @Serializable data class Compound(val subtype: Key?, val value: KMap<Key, PhiNode>) : PhiNode() {
        constructor(id: KString, vararg value: Pair<Key, PhiNode>) : this(Key.String(id), linkedMapOf(*value))
        constructor(id: KInt, vararg value: Pair<Key, PhiNode>) : this(Key.Int(id), linkedMapOf(*value))
        constructor(vararg value: Pair<Key, PhiNode>) : this(null, linkedMapOf(*value))

        override val type: PhiType = if (subtype == null) {
            val fields = LinkedHashMap<Key, PhiType>()
            for ((key, value) in value) fields[key] = value.type
            PhiType.Object(fields)
        } else {
            val fields = LinkedHashMap<Key, PhiType>()
            for ((key, value) in value) fields[key] = value.type
            PhiType.Choice(linkedMapOf(subtype to PhiType.Object(fields)))
        }

        override fun toString(): kotlin.String {
            val elements = value.entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "$key: $value" }
            return if (subtype != null) "$subtype$elements" else elements
        }
    }

    val asCompound: Compound get() = this as Compound

    fun toBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        val writer = Writer.from(DataOutputStream(baos))
        writer.writeTopLevel(this)
        writer.close()
        return baos.toByteArray()
    }

    companion object {
        fun fromBytes(bytes: ByteArray): PhiNode {
            val reader = Reader.from(bytes)
            return reader.readTopLevel()
        }
    }
}

@Serializable sealed interface PhiType {
    @Serializable data object Nothing : PhiType {
        override fun toString(): kotlin.String = "Nothing"
    }
    @Serializable data object Any : PhiType {
        override fun toString(): kotlin.String = "Any"
    }

    @Serializable data object Null : PhiType {
        override fun toString(): kotlin.String = "Null"
    }
    @Serializable data object Byte : PhiType {
        override fun toString(): kotlin.String = "Byte"
    }
    @Serializable data object Short : PhiType {
        override fun toString(): kotlin.String = "Short"
    }
    @Serializable data object Int : PhiType {
        override fun toString(): kotlin.String = "Int"
    }
    @Serializable data object Long : PhiType {
        override fun toString(): kotlin.String = "Long"
    }
    @Serializable data object Float : PhiType {
        override fun toString(): kotlin.String = "Float"
    }
    @Serializable data object Double : PhiType {
        override fun toString(): kotlin.String = "Double"
    }
    @Serializable data object String : PhiType {
        override fun toString(): kotlin.String = "String"
    }
    @Serializable data class List(val size: UInt?, val elementType: PhiType) : PhiType {
        init {
            check((elementType == PhiType.Nothing) == (size == 0u)) {
                "$elementType $size"
            }
        }

        override fun toString(): kotlin.String =
            if (size == null) "List[$elementType]"
            else "List[$elementType, $size]"
    }
    @Serializable data class Map(val size: UInt?, val key: PhiType, val target: PhiType) : PhiType {
        init {
            val n1 = key == PhiType.Nothing
            val n2 = target == PhiType.Nothing
            val n3 = size == 0u
            check((n1 && n2 && n3) || (!n1 && !n2 && !n3)) {
                "$key $target $size"
            }
        }

        override fun toString(): kotlin.String =
            if (size == null) "Map[$key, $target]"
            else "Map[$key, $target, $size]"
    }
    @Serializable data class Object(val fields: LinkedHashMap<PhiNode.Key, PhiType>) : PhiType {
        fun fieldTypeByIndex(index: UInt): PhiType = fields.values.elementAt(index.toInt())
        fun fieldKeyByIndex(index: UInt): PhiNode.Key = fields.keys.elementAt(index.toInt())

        val keyToIndex by lazy {
            val out = LinkedHashMap<PhiNode.Key, UInt>()
            var index = 0u
            for ((key, _) in fields) out[key] = index++
            out
        }

        infix fun union(other: Object): Object {
            val fields = LinkedHashMap<PhiNode.Key, PhiType>()
            for ((key, value) in this.fields) fields[key] = value
            for ((key, value) in other.fields) fields[key] = fields[key]?.union(value) ?: value
            return Object(fields)
        }

        override fun toString(): kotlin.String =
            fields.entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "$key: $value" }
    }
    @Serializable data class Choice(val subtypes: LinkedHashMap<PhiNode.Key, PhiType.Object>) : PhiType {
        fun subtypeByIndex(index: UInt): PhiType.Object = subtypes.values.elementAt(index.toInt())
        fun subtypeKeyByIndex(index: UInt): PhiNode.Key = subtypes.keys.elementAt(index.toInt())

        val keyToIndex by lazy {
            val out = LinkedHashMap<PhiNode.Key, UInt>()
            var index = 0u
            for ((key, _) in subtypes) out[key] = index++
            out
        }

        infix fun union(other: Choice): Choice {
            val subtypes = LinkedHashMap<PhiNode.Key, PhiType.Object>()
            for ((key, subtype) in this.subtypes) subtypes[key] = subtype
            for ((key, subtype) in other.subtypes) subtypes[key] = subtypes[key]?.union(subtype) ?: subtype
            return Choice(subtypes)
        }

        override fun toString(): kotlin.String =
            subtypes.entries.joinToString(" | ") { (key, value) -> "$key: $value" }
    }

    infix fun union(other: PhiType): PhiType {
        if (this.equals(other)) return this
        if (this is Nothing) return other
        if (other is Nothing) return this
        if (this is List && other is List) {
            val elementType = this.elementType union other.elementType
            if (this.size == null || other.size == null) return List(null, elementType)
            return List(
                if (this.size == other.size) this.size else null,
                elementType
            )
        }
        if (this is Map && other is Map) {
            val key = this.key union other.key
            val target = this.target union other.target
            if (this.size == null || other.size == null) return Map(null, key, target)
            return Map(
                if (this.size == other.size) this.size else null,
                key, target)
        }
        if (this is Object && other is Object) return this union other
        if (this is Choice && other is Choice) return this union other
        return Any
    }

    fun toBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        val writer = Writer.from(DataOutputStream(baos))
        writer.writeType(this)
        writer.close()
        return baos.toByteArray()
    }

    companion object {
        fun fromBytes(bytes: ByteArray): PhiType {
            val reader = Reader.from(bytes)
            return reader.readType()
        }
    }
}

sealed class Phi {
    @SerialInfo @Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
    annotation class Id(val id: kotlin.Int)

    val serializersModule = SerializersModule {  }
    val serializers = ConcurrentHashMap<KClass<*>, KSerializer<*>>()

    inline fun <reified T : KAny> serializer(): KSerializer<T> {
        val kclass = T::class
        return serializers.getOrPut(kclass) {
            serializersModule.serializer<T>()
        } as KSerializer<T>
    }

    inline fun <reified T : KAny> toPhiNode(value: T): PhiNode {
        val strategy = serializer<T>()
        return toPhiNode<T>(strategy, value)
    }

    fun <T : KAny> toPhiNode(strategy: SerializationStrategy<T>, value: T): PhiNode {
        val encoder = TreePhiEncoder(serializersModule)
        strategy.serialize(encoder, value)
        return encoder.result!!
    }

    fun <T : KAny> toPhiNode(strategy: SerializationStrategy<T>, value: T, serializersModule: SerializersModule): PhiNode {
        val encoder = TreePhiEncoder(serializersModule)
        strategy.serialize(encoder, value)
        return encoder.result!!
    }

    inline fun <reified T : KAny> fromPhiNode(node: PhiNode): T {
        val strategy = serializer<T>()
        return fromPhiNode<T>(strategy, node)
    }

    fun <T : kotlin.Any> fromPhiNode(strategy: DeserializationStrategy<T>, node: PhiNode): T {
        val decoder = TreePhiDecoder(serializersModule, node)
        return strategy.deserialize(decoder)
    }

    fun <T : kotlin.Any> fromPhiNode(strategy: DeserializationStrategy<T>, node: PhiNode, serializersModule: SerializersModule): T {
        val decoder = TreePhiDecoder(serializersModule, node)
        return strategy.deserialize(decoder)
    }

    companion object : Phi() {

    }
}
