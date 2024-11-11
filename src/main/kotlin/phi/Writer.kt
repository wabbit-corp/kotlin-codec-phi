package phi

import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal fun Writer.println(v: Any?) {
    System.out.println("${this.writtenBytes()} - $v")
}

internal fun Reader.println(v: Any?) {
    System.out.println("${this.readBytes()} - $v")
}

private const val DEBUG = false

object PhiConstants {
    const val VERSION               = 0
    ///////////////////////////////////////////////////////////////////////////
    const val PRIM_FIRST            = 0
    const val NOTHING               = PRIM_FIRST + 0
    const val ANY                   = PRIM_FIRST + 1
    const val NULL                  = PRIM_FIRST + 2
    const val BYTE                  = PRIM_FIRST + 3
    const val SHORT                 = PRIM_FIRST + 4
    const val INT                   = PRIM_FIRST + 5
    const val LONG                  = PRIM_FIRST + 6
    const val FLOAT                 = PRIM_FIRST + 7
    const val DOUBLE                = PRIM_FIRST + 8
    const val STRING                = PRIM_FIRST + 9
    const val PRIM_LAST             = STRING
    ///////////////////////////////////////////////////////////////////////////
    const val LIST_FIRST                   = PRIM_LAST + 1
    const val LIST_0                       = LIST_FIRST + 0  // 0x0A
    const val LIST_1                       = LIST_FIRST + 1  // 0x0B
    const val LIST_UNTYPED_2               = LIST_FIRST + 2  // 0x0C
    const val LIST_TYPED_2                 = LIST_FIRST + 3  // 0x0D
    const val LIST_UNTYPED_3               = LIST_FIRST + 4  // 0x0E
    const val LIST_TYPED_3                 = LIST_FIRST + 5  // 0x0F
    const val LIST_UNTYPED_4               = LIST_FIRST + 6  // 0x10
    const val LIST_TYPED_4                 = LIST_FIRST + 7  // 0x11
    const val LIST_UNTYPED_SIZED           = LIST_FIRST + 8  // 0x12
    const val LIST_TYPED_SIZED             = LIST_FIRST + 9  // 0x13
    const val LIST_UNTYPED_UNSIZED         = LIST_FIRST + 10 // 0x14
    const val LIST_TYPED_UNSIZED           = LIST_FIRST + 11 // 0x15
    const val LIST_LAST                    = LIST_TYPED_UNSIZED
    ///////////////////////////////////////////////////////////////////////////
    const val MAP_FIRST                    = LIST_LAST + 1
    const val MAP_0                        = MAP_FIRST + 0  // 0x16
    const val MAP_1                        = MAP_FIRST + 1  // 0x17
    const val MAP_UNTYPED_UNTYPED_SIZED    = MAP_FIRST + 2  // 0x18
    const val MAP_UNTYPED_TYPED_SIZED      = MAP_FIRST + 3  // 0x19
    const val MAP_TYPED_UNTYPED_SIZED      = MAP_FIRST + 4  // 0x1A
    const val MAP_TYPED_TYPED_SIZED        = MAP_FIRST + 5  // 0x1B
    const val MAP_UNTYPED_UNTYPED_UNSIZED  = MAP_FIRST + 6  // 0x1C
    const val MAP_UNTYPED_TYPED_UNSIZED    = MAP_FIRST + 7  // 0x1D
    const val MAP_TYPED_UNTYPED_UNSIZED    = MAP_FIRST + 8  // 0x1E
    const val MAP_TYPED_TYPED_UNSIZED      = MAP_FIRST + 9  // 0x1F
    const val MAP_LAST                     = MAP_TYPED_TYPED_UNSIZED
    ///////////////////////////////////////////////////////////////////////////
    const val COMPLEX_FIRST                = MAP_LAST + 1
    const val COMPLEX_OBJECT_0             = COMPLEX_FIRST + 0    // 0x20
    const val COMPLEX_OBJECT_1             = COMPLEX_FIRST + 1    // 0x21
    const val COMPLEX_OBJECT_2             = COMPLEX_FIRST + 2    // 0x22
    const val COMPLEX_OBJECT_3             = COMPLEX_FIRST + 3    // 0x23
    const val COMPLEX_OBJECT_4             = COMPLEX_FIRST + 4    // 0x24
    const val COMPLEX_OBJECT_N             = COMPLEX_FIRST + 5    // 0x25
    const val COMPLEX_CHOICE_1             = COMPLEX_FIRST + 6    // 0x26
    const val COMPLEX_CHOICE_2             = COMPLEX_FIRST + 7    // 0x27
    const val COMPLEX_CHOICE_3             = COMPLEX_FIRST + 8    // 0x28
    const val COMPLEX_CHOICE_4             = COMPLEX_FIRST + 9    // 0x29
    const val COMPLEX_CHOICE_N             = COMPLEX_FIRST + 10   // 0x2A
}

abstract class Writer : AutoCloseable {
    enum class Region {
        TYPE, SIZE, VALUE, KEY
    }

    abstract fun writtenBytes(): Int
    abstract fun writeUInt8(v: Int)
    abstract fun writeUInt8(v: UByte)

    fun writeInt16(v: Short) {
        writeUInt8((v.toInt() ushr 8).toUByte())
        writeUInt8(v.toUByte())
    }
    fun writeInt32(v: Int) {
        writeUInt8((v ushr 24).toUByte())
        writeUInt8((v ushr 16).toUByte())
        writeUInt8((v ushr 8).toUByte())
        writeUInt8(v.toUByte())
    }
    fun writeInt64(v: Long) {
        writeUInt8((v ushr 56).toUByte())
        writeUInt8((v ushr 48).toUByte())
        writeUInt8((v ushr 40).toUByte())
        writeUInt8((v ushr 32).toUByte())
        writeUInt8((v ushr 24).toUByte())
        writeUInt8((v ushr 16).toUByte())
        writeUInt8((v ushr 8).toUByte())
        writeUInt8(v.toUByte())
    }
    fun writeFloat32(v: Float) {
        writeInt32(v.toRawBits())
    }
    fun writeDouble(v: Double) {
        writeInt64(v.toRawBits())
    }
    fun writeBytes(v: ByteArray) {
        for (b in v) {
            writeUInt8(b.toUByte())
        }
    }
    fun writeZigZagVarInt(value: Int): Unit = writeVarInt(signTransform(value))
    fun writeZigZagVarLong(value: Long): Unit = writeVarLong(signTransform(value))
    fun writeVarInt(input: UInt) {
        var input = input
        while ((input and 0xFFFFFF80u) != 0u) {
            this.writeUInt8(((input and 0x7Fu) or 0x80u).toUByte())
            input = input shr 7
        }
        this.writeUInt8(input.toUByte())
    }
    fun writeVarLong(value: ULong) {
        var value = value
        while ((value and 0xFFFFFFFFFFFFFF80UL) != 0UL) {
            this.writeUInt8(((value and 0x7FUL) or 0x80UL).toUByte())
            value = value shr 7
        }
        this.writeUInt8(value.toUByte())
    }
    fun writeString(s: String) {
        val bytes = s.toByteArray(StandardCharsets.UTF_8)
        writeSize(bytes.size.toUInt())
        writeBytes(bytes)
    }

    fun writeSize(size: UInt) {
        scope(Region.SIZE) { writeVarInt(size) }
    }
    fun writeKey(key: PhiNode.Key) {
        scope(Region.KEY) {
            when (key) {
                is PhiNode.Key.Int -> {
                    writeZigZagVarInt(key.value)
                }
                is PhiNode.Key.String -> {
                    writeZigZagVarInt(-key.value.length)
                    writeBytes(key.value.toByteArray(StandardCharsets.UTF_8))
                }
            }
        }
    }
    fun writeTopLevel(node: PhiNode) {
        writeValue(node, PhiType.Any)
    }

    ///////////////////////////////////////////////////////////////////////////

    fun writeType(type: PhiType): Unit = scope(Writer.Region.TYPE) {
        if (DEBUG) println("type.write.start: $type")
        when (type) {
            is PhiType.Nothing -> error("This should not happen")
                // writeUInt8(PhiConstants.NOTHING)
            is PhiType.Any     -> writeUInt8(PhiConstants.ANY)
            is PhiType.Null    -> writeUInt8(PhiConstants.NULL)
            is PhiType.Byte    -> writeUInt8(PhiConstants.BYTE)
            is PhiType.Short   -> writeUInt8(PhiConstants.SHORT)
            is PhiType.Int     -> writeUInt8(PhiConstants.INT)
            is PhiType.Long    -> writeUInt8(PhiConstants.LONG)
            is PhiType.Float   -> writeUInt8(PhiConstants.FLOAT)
            is PhiType.Double  -> writeUInt8(PhiConstants.DOUBLE)
            is PhiType.String  -> writeUInt8(PhiConstants.STRING)
            is PhiType.List -> {
                if (type.size == null) {
                    if (type.elementType == PhiType.Any) {
                        writeUInt8(PhiConstants.LIST_UNTYPED_UNSIZED)
                    } else {
                        writeUInt8(PhiConstants.LIST_TYPED_UNSIZED)
                        writeType(type.elementType)
                    }
                } else {
                    if (type.size == 0u) {
                        writeUInt8(PhiConstants.LIST_0)
                    }
                    else if (type.size == 1u) {
                        writeUInt8(PhiConstants.LIST_1)
                        writeType(type.elementType)
                    }
                    else if (type.size == 2u) {
                        if (type.elementType == PhiType.Any) {
                            writeUInt8(PhiConstants.LIST_UNTYPED_2)
                        } else {
                            writeUInt8(PhiConstants.LIST_TYPED_2)
                            writeType(type.elementType)
                        }
                    }
                    else if (type.size == 3u) {
                        if (type.elementType == PhiType.Any) {
                            writeUInt8(PhiConstants.LIST_UNTYPED_3)
                        } else {
                            writeUInt8(PhiConstants.LIST_TYPED_3)
                            writeType(type.elementType)
                        }
                    }
                    else if (type.size == 4u) {
                        if (type.elementType == PhiType.Any) {
                            writeUInt8(PhiConstants.LIST_UNTYPED_4)
                        } else {
                            writeUInt8(PhiConstants.LIST_TYPED_4)
                            writeType(type.elementType)
                        }
                    }
                    else {
                        if (type.elementType == PhiType.Any) {
                            writeUInt8(PhiConstants.LIST_UNTYPED_SIZED)
                            writeSize(type.size)
                        }
                        else {
                            writeUInt8(PhiConstants.LIST_TYPED_SIZED)
                            writeSize(type.size)
                            writeType(type.elementType)
                        }
                    }
                }
            }
            is PhiType.Map -> {
                if (type.size == null) {
                    if (type.key == PhiType.Any && type.target == PhiType.Any) {
                        writeUInt8(PhiConstants.MAP_UNTYPED_UNTYPED_UNSIZED)
                    }
                    else if (type.key == PhiType.Any) {
                        writeUInt8(PhiConstants.MAP_UNTYPED_TYPED_UNSIZED)
                        writeType(type.target)
                    }
                    else if (type.target == PhiType.Any) {
                        writeUInt8(PhiConstants.MAP_TYPED_UNTYPED_UNSIZED)
                        writeType(type.key)
                    }
                    else {
                        writeUInt8(PhiConstants.MAP_TYPED_TYPED_UNSIZED)
                        writeType(type.key)
                        writeType(type.target)
                    }
                }
                else {
                    if (type.size == 0u) {
                        writeUInt8(PhiConstants.MAP_0)
                    }
                    else if (type.size == 1u) {
                        writeUInt8(PhiConstants.MAP_1)
                        writeType(type.key)
                        writeType(type.target)
                    }
                    else {
                        if (type.key == PhiType.Any && type.target == PhiType.Any) {
                            writeUInt8(PhiConstants.MAP_UNTYPED_UNTYPED_SIZED)
                            writeSize(type.size)
                        }
                        else if (type.key == PhiType.Any) {
                            writeUInt8(PhiConstants.MAP_UNTYPED_TYPED_SIZED)
                            writeSize(type.size)
                            writeType(type.target)
                        }
                        else if (type.target == PhiType.Any) {
                            writeUInt8(PhiConstants.MAP_TYPED_UNTYPED_SIZED)
                            writeSize(type.size)
                            writeType(type.key)
                        }
                        else {
                            writeUInt8(PhiConstants.MAP_TYPED_TYPED_SIZED)
                            writeSize(type.size)
                            writeType(type.key)
                            writeType(type.target)
                        }
                    }
                }
            }
            is PhiType.Object -> {
                when (type.fields.size) {
                    0 -> writeUInt8(PhiConstants.COMPLEX_OBJECT_0)
                    1 -> writeUInt8(PhiConstants.COMPLEX_OBJECT_1)
                    2 -> writeUInt8(PhiConstants.COMPLEX_OBJECT_2)
                    3 -> writeUInt8(PhiConstants.COMPLEX_OBJECT_3)
                    4 -> writeUInt8(PhiConstants.COMPLEX_OBJECT_4)
                    else -> {
                        writeUInt8(PhiConstants.COMPLEX_OBJECT_N)
                        writeSize(type.fields.size.toUInt())
                    }
                }

                for ((key, value) in type.fields) {
                    writeKey(key)
                    writeType(value)
                }
            }
            is PhiType.Choice -> {
                when (type.subtypes.size) {
                    1 -> writeUInt8(PhiConstants.COMPLEX_CHOICE_1)
                    2 -> writeUInt8(PhiConstants.COMPLEX_CHOICE_2)
                    3 -> writeUInt8(PhiConstants.COMPLEX_CHOICE_3)
                    4 -> writeUInt8(PhiConstants.COMPLEX_CHOICE_4)
                    else -> {
                        writeUInt8(PhiConstants.COMPLEX_CHOICE_N)
                        writeSize(type.subtypes.size.toUInt())
                    }
                }
                for ((subtypeKey, subtype) in type.subtypes) {
                    writeKey(subtypeKey)
                    writeType(subtype)
                }
            }
        }
        if (DEBUG) println("type.write.end: $type")
    }
    fun writeValue(node: PhiNode, knownUpperBound: PhiType): Unit = scope(Writer.Region.VALUE) {
        if (DEBUG) println("value.write.start: $node :: $knownUpperBound")

        if (knownUpperBound == PhiType.Any) {
            writeType(node.type)
            writeValue(node, node.type)
            return@scope
        }

        when (node) {
            is PhiNode.Null     -> writeValue(null, knownUpperBound)
            is PhiNode.Byte     -> writeValue(node.value, knownUpperBound)
            is PhiNode.Short    -> writeValue(node.value, knownUpperBound)
            is PhiNode.Int      -> writeValue(node.value, knownUpperBound)
            is PhiNode.Long     -> writeValue(node.value, knownUpperBound)
            is PhiNode.Float    -> writeValue(node.value, knownUpperBound)
            is PhiNode.Double   -> writeValue(node.value, knownUpperBound)
            is PhiNode.String   -> writeValue(node.value, knownUpperBound)
            is PhiNode.List     -> writeValue(node, knownUpperBound)
            is PhiNode.Map      -> writeValue(node, knownUpperBound)
            is PhiNode.Compound -> writeValue(node, knownUpperBound)
        }

        if (DEBUG) println("value.write.end: $node :: $knownUpperBound")
    }
    fun writeValue(value: Byte, knownUpperBound: PhiType): Unit {
        if (knownUpperBound == PhiType.Byte) {
            writeUInt8(value.toUByte())
        } else {
            check(knownUpperBound == PhiType.Any)
            writeType(PhiType.Byte)
            writeUInt8(value.toInt())
        }
    }
    fun writeValue(value: Short, knownUpperBound: PhiType): Unit {
        if (knownUpperBound == PhiType.Short) {
            writeInt16(value)
        } else {
            check(knownUpperBound == PhiType.Any)
            writeType(PhiType.Short)
            writeInt16(value)
        }
    }
    fun writeValue(value: Int, knownUpperBound: PhiType): Unit {
        if (knownUpperBound == PhiType.Int) {
            writeZigZagVarInt(value)
        } else {
            check(knownUpperBound == PhiType.Any)
            writeType(PhiType.Int)
            writeZigZagVarInt(value)
        }
    }
    fun writeValue(value: Long, knownUpperBound: PhiType): Unit {
        if (knownUpperBound == PhiType.Long) {
            writeZigZagVarLong(value)
        } else {
            check(knownUpperBound == PhiType.Any)
            writeType(PhiType.Long)
            writeZigZagVarLong(value)
        }
    }
    fun writeValue(value: Float, knownUpperBound: PhiType): Unit {
        if (knownUpperBound == PhiType.Float) {
            writeFloat32(value)
        } else {
            check(knownUpperBound == PhiType.Any)
            writeType(PhiType.Float)
            writeFloat32(value)
        }
    }
    fun writeValue(value: Double, knownUpperBound: PhiType): Unit {
        if (knownUpperBound == PhiType.Double) {
            writeDouble(value)
        } else {
            check(knownUpperBound == PhiType.Any)
            writeType(PhiType.Double)
            writeDouble(value)
        }
    }
    fun writeValue(value: String, knownUpperBound: PhiType): Unit {
        if (knownUpperBound == PhiType.String) {
            writeString(value)
        } else {
            check(knownUpperBound == PhiType.Any)
            writeType(PhiType.String)
            writeString(value)
        }
    }
    fun writeValue(value: Nothing?, knownUpperBound: PhiType): Unit {
        if (knownUpperBound == PhiType.Null) {
            // do nothing
        } else {
            check(knownUpperBound == PhiType.Any)
            writeType(PhiType.Null)
        }
    }
    fun writeValue(node: PhiNode.List, knownUpperBound: PhiType): Unit {
        var boundType = knownUpperBound
        val realType = node.type
        if (boundType == PhiType.Any) {
            boundType = realType
            writeType(boundType)
        }
        check(boundType is PhiType.List)

        val realSize = node.value.size.toUInt()
        var elementBound = boundType.elementType
        if (boundType.size == null)
            writeSize(node.value.size.toUInt())
        else check(boundType.size == realSize)

        if (elementBound == PhiType.Any) {
            val realElementType = realType.elementType
            if (realSize != 0u) writeType(realElementType)
            elementBound = realElementType
        }
        for (element in node.value)
            writeValue(element, elementBound)
    }
    fun writeValue(node: PhiNode.Map, knownUpperBound: PhiType): Unit {
        if (knownUpperBound == PhiType.Any) {
            writeType(node.type)
            writeValue(node, node.type)
        } else {
            check(knownUpperBound is PhiType.Map)
            val realType = node.type
            var keyBound = knownUpperBound.key
            var targetBound = knownUpperBound.target
            val realSize = node.value.size.toUInt()
            if (knownUpperBound.size == null)
                writeSize(node.value.size.toUInt())
            else check(knownUpperBound.size == realSize)

            if (keyBound == PhiType.Any) {
                if (realSize != 0u) writeType(realType.key)
                keyBound = realType.key
            }
            if (targetBound == PhiType.Any) {
                if (realSize != 0u) writeType(realType.target)
                targetBound = realType.target
            }
            for ((key, target) in node.value) {
                writeValue(key, keyBound)
                writeValue(target, targetBound)
            }
        }
    }
    fun writeValue(node: PhiNode.Compound, knownUpperBound: PhiType): Unit {
        if (knownUpperBound == PhiType.Any) {
            writeType(node.type)
        }

        when (knownUpperBound) {
            is PhiType.Object -> {
                check(node.subtype == null)
                writeSize(node.value.size.toUInt())
                for ((key, element) in node.value) {
                    writeVarInt(knownUpperBound.keyToIndex[key]!!)
                    writeValue(element, element.type)
                }
            }
            is PhiType.Choice -> {
                check(node.subtype != null)
                writeVarInt(knownUpperBound.keyToIndex[node.subtype]!!)
                val subtype = knownUpperBound.subtypes[node.subtype]!!
                writeSize(node.value.size.toUInt())
                for ((key, element) in node.value) {
                    val fieldIndex = subtype.keyToIndex[key]!!
                    val fieldType = subtype.fields[key]!!
                    writeVarInt(fieldIndex)
                    writeValue(element, fieldType)
                }
            }
            else -> error("Unsupported type: $knownUpperBound")
        }
    }

    companion object {
        class DataOutputStreamWriter(val dos: DataOutputStream) : Writer() {
            private var writtenBytes = 0
            override fun writtenBytes() = writtenBytes
            override fun writeUInt8(v: Int) {
                require(v in 0..255)
                // sb.append("%02X ".format(v.toByte()))
                dos.writeByte(v)
                writtenBytes += 1
            }
            override fun writeUInt8(v: UByte) {
                // sb.append("%02X ".format(v.toByte()))
                dos.writeByte(v.toInt())
                writtenBytes += 1
            }
            override fun close() {
                dos.close()
            }
        }

        fun from(dos: DataOutputStream): Writer = DataOutputStreamWriter(dos)
    }
}

abstract class Reader : AutoCloseable {
    abstract fun readUInt8(): UByte
    abstract fun readBytes(): Int

    fun readInt16(): Short {
        val b1 = readUInt8().toInt()
        val b2 = readUInt8().toInt()
        return ((b1 shl 8) or b2).toShort()
    }
    fun readInt32(): Int {
        val b1 = readUInt8().toInt()
        val b2 = readUInt8().toInt()
        val b3 = readUInt8().toInt()
        val b4 = readUInt8().toInt()
        return ((b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4)
    }
    fun readInt64(): Long {
        val b1 = readUInt8().toLong() and 0xFF
        val b2 = readUInt8().toLong() and 0xFF
        val b3 = readUInt8().toLong() and 0xFF
        val b4 = readUInt8().toLong() and 0xFF
        val b5 = readUInt8().toLong() and 0xFF
        val b6 = readUInt8().toLong() and 0xFF
        val b7 = readUInt8().toLong() and 0xFF
        val b8 = readUInt8().toLong() and 0xFF
        return ((b1 shl 56) or (b2 shl 48) or (b3 shl 40) or (b4 shl 32) or
                (b5 shl 24) or (b6 shl 16) or (b7 shl 8) or b8)
    }
    fun readFloat32(): Float {
        return Float.fromBits(readInt32())
    }
    fun readFloat64(): Double {
        return Double.fromBits(readInt64())
    }
    fun readBytes(size: Int): ByteArray {
        return ByteArray(size) { readUInt8().toByte() }
    }
    fun readString(): String {
        val length = readVarInt()
        val bytes = readBytes(length.toInt())
        return String(bytes, StandardCharsets.UTF_8)
    }
    fun readZigZagVarInt(): Int = inverseSignTransform(readVarInt())
    fun readZigZagVarLong(): Long = inverseSignTransform(readVarLong())
    fun readVarInt(): UInt {
        var value = 0u
        var byteCount = 0

        while (true) {
            val b0 = this.readUInt8().toUInt()
            value = value or ((b0 and 0x7Fu) shl (byteCount++ * 7))

            if (byteCount > 5) {
                throw java.lang.RuntimeException("VarInt too big")
            }

            if ((b0 and 0x80u) != 0x80u) {
                break
            }
        }

        return value
    }
    fun readVarLong(): ULong {
        var value = 0UL
        var byteCount = 0

        while (true) {
            val b0 = this.readUInt8().toULong()
            value = value or ((b0 and 0x7FUL) shl (byteCount++ * 7))
            if (byteCount > 10) error("VarLong too big")
            if ((b0 and 128UL) != 128UL) break
        }

        return value
    }

    fun readKey(): PhiNode.Key {
        val id = readZigZagVarInt()
        if (id >= 0) return PhiNode.Key.Int(id)
        else {
            val bytes = readBytes(-id)
            return PhiNode.Key.String(bytes.toString(StandardCharsets.UTF_8))
        }
    }
    fun readSize(): UInt = readVarInt()
    fun readTopLevel(): PhiNode {
        return readValue(PhiType.Any)
    }

    ///////////////////////////////////////////////////////////////////////////

    private fun readSubtype(): PhiType.Object {
        val type = readUInt8().toInt()
        when (type) {
            PhiConstants.COMPLEX_OBJECT_0 -> return PhiType.Object(linkedMapOf())
            PhiConstants.COMPLEX_OBJECT_1 -> {
                val key = readKey()
                val value = readType()
                return PhiType.Object(linkedMapOf(key to value))
            }
            PhiConstants.COMPLEX_OBJECT_2 -> {
                val key1 = readKey()
                val value1 = readType()
                val key2 = readKey()
                val value2 = readType()
                return PhiType.Object(linkedMapOf(key1 to value1, key2 to value2))
            }
            PhiConstants.COMPLEX_OBJECT_3 -> {
                val key1 = readKey()
                val value1 = readType()
                val key2 = readKey()
                val value2 = readType()
                val key3 = readKey()
                val value3 = readType()
                return PhiType.Object(linkedMapOf(key1 to value1, key2 to value2, key3 to value3))
            }
            PhiConstants.COMPLEX_OBJECT_4 -> {
                val key1 = readKey()
                val value1 = readType()
                val key2 = readKey()
                val value2 = readType()
                val key3 = readKey()
                val value3 = readType()
                val key4 = readKey()
                val value4 = readType()
                return PhiType.Object(linkedMapOf(key1 to value1, key2 to value2, key3 to value3, key4 to value4))
            }
            PhiConstants.COMPLEX_OBJECT_N -> {
                val size = readVarInt()
                val fields = linkedMapOf<PhiNode.Key, PhiType>()
                for (i in 0u until size) {
                    val key = readKey()
                    val value = readType()
                    fields[key] = value
                }
                return PhiType.Object(fields)
            }
            else -> error("Unknown subtype: $type")
        }
    }
    fun readType(): PhiType {
        if (DEBUG) println("type.read.start")

        val result = when (readUInt8().toInt()) {
            PhiConstants.NOTHING -> error("This should not happen")
            PhiConstants.ANY     -> PhiType.Any
            PhiConstants.NULL    -> PhiType.Null
            PhiConstants.BYTE    -> PhiType.Byte
            PhiConstants.SHORT   -> PhiType.Short
            PhiConstants.INT     -> PhiType.Int
            PhiConstants.LONG    -> PhiType.Long
            PhiConstants.FLOAT   -> PhiType.Float
            PhiConstants.DOUBLE  -> PhiType.Double
            PhiConstants.STRING  -> PhiType.String

            PhiConstants.LIST_0                      -> PhiType.List(0u, PhiType.Nothing)
            PhiConstants.LIST_1                      -> PhiType.List(1u, readType())
            PhiConstants.LIST_UNTYPED_2              -> PhiType.List(2u, PhiType.Any)
            PhiConstants.LIST_TYPED_2                -> PhiType.List(2u, readType())
            PhiConstants.LIST_UNTYPED_3              -> PhiType.List(3u, PhiType.Any)
            PhiConstants.LIST_TYPED_3                -> PhiType.List(3u, readType())
            PhiConstants.LIST_UNTYPED_4              -> PhiType.List(4u, PhiType.Any)
            PhiConstants.LIST_TYPED_4                -> PhiType.List(4u, readType())
            PhiConstants.LIST_UNTYPED_SIZED          -> PhiType.List(readSize(), PhiType.Any)
            PhiConstants.LIST_TYPED_SIZED            -> PhiType.List(readSize(), readType())
            PhiConstants.LIST_UNTYPED_UNSIZED        -> PhiType.List(null, PhiType.Any)
            PhiConstants.LIST_TYPED_UNSIZED          -> PhiType.List(null, readType())

            PhiConstants.MAP_0                       -> PhiType.Map(0u, PhiType.Nothing, PhiType.Nothing)
            PhiConstants.MAP_1                       -> PhiType.Map(1u, readType(), readType())
            PhiConstants.MAP_UNTYPED_UNTYPED_SIZED   -> PhiType.Map(readSize(), PhiType.Any, PhiType.Any)
            PhiConstants.MAP_UNTYPED_TYPED_SIZED     -> PhiType.Map(readSize(), PhiType.Any, readType())
            PhiConstants.MAP_TYPED_UNTYPED_SIZED     -> PhiType.Map(readSize(), readType(), PhiType.Any)
            PhiConstants.MAP_TYPED_TYPED_SIZED       -> PhiType.Map(readSize(), readType(), readType())
            PhiConstants.MAP_UNTYPED_UNTYPED_UNSIZED -> PhiType.Map(null, PhiType.Any, PhiType.Any)
            PhiConstants.MAP_UNTYPED_TYPED_UNSIZED   -> PhiType.Map(null, PhiType.Any, readType())
            PhiConstants.MAP_TYPED_UNTYPED_UNSIZED   -> PhiType.Map(null, readType(), PhiType.Any)
            PhiConstants.MAP_TYPED_TYPED_UNSIZED     -> PhiType.Map(null, readType(), readType())

            PhiConstants.COMPLEX_OBJECT_0 -> {
                PhiType.Object(linkedMapOf())
            }
            PhiConstants.COMPLEX_OBJECT_1 -> {
                val key = readKey()
                val value = readType()
                PhiType.Object(linkedMapOf(key to value))
            }
            PhiConstants.COMPLEX_OBJECT_2 -> {
                val key1 = readKey()
                val value1 = readType()
                val key2 = readKey()
                val value2 = readType()
                PhiType.Object(linkedMapOf(key1 to value1, key2 to value2))
            }
            PhiConstants.COMPLEX_OBJECT_3 -> {
                val key1 = readKey()
                val value1 = readType()
                val key2 = readKey()
                val value2 = readType()
                val key3 = readKey()
                val value3 = readType()
                PhiType.Object(linkedMapOf(key1 to value1, key2 to value2, key3 to value3))
            }
            PhiConstants.COMPLEX_OBJECT_4 -> {
                val key1 = readKey()
                val value1 = readType()
                val key2 = readKey()
                val value2 = readType()
                val key3 = readKey()
                val value3 = readType()
                val key4 = readKey()
                val value4 = readType()
                PhiType.Object(linkedMapOf(key1 to value1, key2 to value2, key3 to value3, key4 to value4))
            }
            PhiConstants.COMPLEX_OBJECT_N -> {
                val size = readSize()
                val fields = linkedMapOf<PhiNode.Key, PhiType>()
                for (i in 0u until size) {
                    val key = readKey()
                    val value = readType()
                    fields[key] = value
                }
                PhiType.Object(fields)
            }

            PhiConstants.COMPLEX_CHOICE_1 -> {
                val key1 = readKey()
                val fields1 = readSubtype()
                PhiType.Choice(linkedMapOf(key1 to fields1))
            }
            PhiConstants.COMPLEX_CHOICE_2 -> {
                val key1 = readKey()
                val fields1 = readSubtype()
                val key2 = readKey()
                val fields2 = readSubtype()
                PhiType.Choice(linkedMapOf(key1 to fields1, key2 to fields2))
            }
            PhiConstants.COMPLEX_CHOICE_3 -> {
                val key1 = readKey()
                val fields1 = readSubtype()
                val key2 = readKey()
                val fields2 = readSubtype()
                val key3 = readKey()
                val fields3 = readSubtype()
                PhiType.Choice(linkedMapOf(key1 to fields1, key2 to fields2, key3 to fields3))
            }
            PhiConstants.COMPLEX_CHOICE_4 -> {
                val key1 = readKey()
                val fields1 = readSubtype()
                val key2 = readKey()
                val fields2 = readSubtype()
                val key3 = readKey()
                val fields3 = readSubtype()
                val key4 = readKey()
                val fields4 = readSubtype()
                PhiType.Choice(linkedMapOf(key1 to fields1, key2 to fields2, key3 to fields3, key4 to fields4))
            }
            PhiConstants.COMPLEX_CHOICE_N -> {
                val totalSubtypes = readSize()
                val subtypes = linkedMapOf<PhiNode.Key, PhiType.Object>()
                for (i in 0u until totalSubtypes) {
                    val key = readKey()
                    subtypes[key] = readSubtype()
                }
                PhiType.Choice(subtypes)
            }

            else -> {
                if (DEBUG) println("Unknown type")
                throw RuntimeException("Invalid type")
            }
        }
        if (DEBUG) println("type.read.end: $result")
        return result
    }
    fun readValue(bound: PhiType): PhiNode {
        val originalBound = bound
        if (DEBUG) println("value.read.start: $originalBound")
        var bound = bound
        if (bound == PhiType.Any) {
            bound = readType()
            val result = readValue(bound)
            if (DEBUG) println("value.read.end: $result :: $originalBound")
            return result
        }

        val result = when (bound) {
            is PhiType.Nothing -> error("trying to read Nothing")
            is PhiType.Any -> error("Any should already be handled above")

            is PhiType.Null   -> PhiNode.Null
            is PhiType.Byte   -> PhiNode.Byte(readUInt8().toByte())
            is PhiType.Short  -> PhiNode.Short(readInt16())
            is PhiType.Int    -> PhiNode.Int(readZigZagVarInt())
            is PhiType.Long   -> PhiNode.Long(readZigZagVarLong())
            is PhiType.Float  -> PhiNode.Float(readFloat32())
            is PhiType.Double -> PhiNode.Double(readFloat64())
            is PhiType.String -> PhiNode.String(readString())
            is PhiType.List -> {
                val boundElementType = bound.elementType
                val size = bound.size ?: readSize()
                val realElementType =
                    if (size == 0u) PhiType.Nothing
                    else if (boundElementType == PhiType.Any) readType()
                    else boundElementType
                return PhiNode.List(List(size.toInt()) { readValue(realElementType) })
            }
            is PhiType.Map -> {
                val keyBound = bound.key
                val targetBound = bound.target
                val size = bound.size ?: readSize()
                val realKey: PhiType
                val realTarget: PhiType
                if (size == 0u) {
                    realKey = PhiType.Nothing
                    realTarget = PhiType.Nothing
                } else {
                    realKey = if (keyBound == PhiType.Any) readType() else keyBound
                    realTarget = if (targetBound == PhiType.Any) readType() else targetBound
                }
                val result = mutableMapOf<PhiNode, PhiNode>()
                for (i in 0u until size) {
                    val key = readValue(realKey)
                    if (DEBUG) println("key = $key")
                    val target = readValue(realTarget)
                    if (DEBUG) println("key = $key, target = $target")
                    result[key] = target
                }
                return PhiNode.Map(result)
            }
            is PhiType.Object -> readValue(bound)
            is PhiType.Choice -> readValue(bound)
        }

        if (DEBUG) println("value.read.end: $result :: $bound")
        return result
    }

    fun readValue(bound: PhiType.Object): PhiNode.Compound {
        val size = readVarInt()
        val fields = mutableMapOf<PhiNode.Key, PhiNode>()
        for (i in 0u until size) {
            val fieldIndex = readVarInt()
            val fieldType = bound.fieldTypeByIndex(fieldIndex)
            val fieldKey = bound.fieldKeyByIndex(fieldIndex)
            fields[fieldKey] = readValue(fieldType)
        }
        return PhiNode.Compound(null, fields)
    }
    fun readValue(bound: PhiType.Choice): PhiNode.Compound {
        val subtypeIndex = readVarInt()
        val subtype = bound.subtypeByIndex(subtypeIndex)
        val subtypeKey = bound.subtypeKeyByIndex(subtypeIndex)

        val fields = mutableMapOf<PhiNode.Key, PhiNode>()
        val size = readSize()
        for (i in 0u until size) {
            val fieldIndex = readVarInt()
            val fieldType = subtype.fieldTypeByIndex(fieldIndex)
            val fieldKey = subtype.fieldKeyByIndex(fieldIndex)
            if (DEBUG) println("subtypeIndex=${subtypeIndex} subtypeKey=${subtypeKey} fieldType=$fieldType, fieldKey=$fieldKey")
            fields[fieldKey] = readValue(fieldType)
        }
        return PhiNode.Compound(subtypeKey, fields)
    }

    companion object {
        fun from(bytes: ByteArray): Reader = DataInputStreamReader(DataInputStream(bytes.inputStream()))

        class DataInputStreamReader(val dis: DataInputStream) : Reader() {
            var bytesRead = 0
            override fun readBytes(): Int = bytesRead
            override fun readUInt8(): UByte {
                val r = dis.readByte()
                bytesRead++
                return r.toUByte()
            }

            override fun close() {
                dis.close()
            }
        }

        class DebugDataInputStreamReader(val bytes: ByteArray, val dis: DataInputStream) : Reader() {
            fun asString(): String {
                val sb = StringBuilder()
                val r = readBytes()
                for (n in 0 until r) {
                    if (n > 0) sb.append(' ')
                    sb.append("%02X".format(bytes[n]))
                }
                sb.append("]")
                for (n in r until bytes.size) {
                    if (n > r) sb.append(' ')
                    sb.append("%02X".format(bytes[n]))
                }
                return sb.toString()
            }

            var bytesRead = 0
            override fun readUInt8(): UByte {
                val r = dis.readByte()
                bytesRead++
                return r.toUByte()
            }
            override fun readBytes(): Int = bytesRead

            override fun close() {
                dis.close()
            }

            override fun toString(): String = asString()
        }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun Writer.scope(i: Writer.Region, f: Writer.() -> Unit) {
    contract {
        callsInPlace(f, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
//    startRegion(i)
//    try {
        f()
//    } finally {
//        endRegion(i)
//    }
}

internal fun getVarIntSize(input: Int): Int {
    for (i in 1..4) {
        if ((input and (-1 shl i * 7)) == 0) {
            return i
        }
    }
    return 5
}
internal fun getVarLongSize(input: Long): Int {
    for (i in 1..8) {
        if ((input and (-1L shl i * 7)) == 0L) {
            return i
        }
    }
    return 9
}

internal fun signTransform(v0: Int): UInt {
    if (v0 < 0) return ((v0.inv() shl 1) or 1).toUInt()
    return (v0 shl 1).toUInt()
}
internal fun inverseSignTransform(v1: UInt): Int {
    val signBit = v1 and 1u
    val magnitude = v1 shr 1
    return if (signBit == 1u) magnitude.inv().toInt() else magnitude.toInt()
}
internal fun signTransform(v0: Long): ULong {
    if (v0 < 0) return ((v0.inv() shl 1) or 1).toULong()
    return (v0 shl 1).toULong()
}
internal fun inverseSignTransform(v1: ULong): Long {
    val signBit = v1 and 1UL
    val magnitude = v1 shr 1
    return if (signBit == 1UL) magnitude.inv().toLong() else magnitude.toLong()
}
