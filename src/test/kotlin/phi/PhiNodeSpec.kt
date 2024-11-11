package phi

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import one.wabbit.random.gen.Gen
import one.wabbit.random.gen.foreach
import one.wabbit.random.gen.sample
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.SplittableRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class PhiNodeSpec {
    val genLong = Gen.int(Int.MIN_VALUE..Int.MAX_VALUE).map { it.toLong() }

    val genKey: Gen<PhiNode.Key> = Gen.oneOfGen(
        Gen.int(1..10).map { PhiNode.Key.Int(it) },
        Gen.string(Gen.int(1..3), Gen.range('a'..'z')).map { PhiNode.Key.String(it) }
    )

    val genPhiNode: Gen<PhiNode> by lazy {
        val self = Gen.delay { genPhiNode }
        val smallInt = Gen.int(0..3)

        Gen.oneOfGen(
            Gen.pure(PhiNode.Null),
            Gen.byte.map { PhiNode.Byte(it) },
            Gen.short.map { PhiNode.Short(it) },
            Gen.int.map { PhiNode.Int(it) },
            Gen.int.map { PhiNode.Long(it.toLong()) },
            Gen.int.map { PhiNode.Float(it.toFloat()) },
            Gen.int.map { PhiNode.Double(it.toDouble()) },
            Gen.int(-100..100).map { PhiNode.String("A$it") },

            self.repeat(smallInt).map { PhiNode.List(it) },
            (self zip self).repeat(smallInt).map { PhiNode.Map(it.toMap()) },
            (genKey zip (genKey zip self).repeat(smallInt)).map {
                PhiNode.Compound(it.first, it.second.toMap())
            }
        )
    }

    class Stats {
        var totalTypeSize = 0L
        var totalValueSize = 0L
        var maxTypeSize = 0
        var maxValueSize = 0
        var total = 0L
    }

    fun testValue(stats: Stats?, v0: PhiNode, debug: Boolean = false) {
        val t0 = v0.type
        if (debug) println("v0 = $v0 :: $t0")
        val tb = t0.toBytes()

        val t1 = PhiType.fromBytes(tb)
        check(t0 == t1) { "$t0 != $t1" }
        if (debug) println("TYPES READ CORRECTLY")

        val vb = v0.toBytes()
        if (debug) println("  " + vb.joinToString(" ") { "%02X".format(it) })
        if (debug) println("----")
        val v1 = PhiNode.fromBytes(vb)
        check(v0 == v1) { "$v0 != $v1" }

        stats?.apply {
            totalTypeSize += tb.size.toLong()
            maxTypeSize = maxTypeSize.coerceAtLeast(tb.size)
            total += 1
            totalValueSize += vb.size.toLong()
            maxValueSize = maxValueSize.coerceAtLeast(vb.size)
        }
    }

    fun testValue(stats: Stats?, seed: Long, debug: Boolean = false) {
        val v0 = genPhiNode.sample(SplittableRandom(seed))!!
        testValue(stats, v0, debug)
    }

    @OptIn(InternalSerializationApi::class)
    @Test fun testRandomPhiValues() {
        val stats = Stats()

        val rng = SplittableRandom(1)

        for (it in 1..10000) {
            val seed = rng.nextLong()
            try {
                testValue(stats, seed)
            } catch (e: Throwable) {
                println("seed = $seed")
                throw e
            }
        }

        println("total = ${stats.total}")
        println("average type size = ${stats.totalTypeSize.toDouble() / stats.total}")
        println("average value size = ${stats.totalValueSize.toDouble() / stats.total}")
        println("max type size = ${stats.maxTypeSize}")
        println("max value size = ${stats.maxValueSize}")

        // total = 10000
        // average type size = 2.4105
        // average value size = 11.7665
        // max type size = 67
        // max value size = 420

        // total = 10000
        // average type size = 1.9753
        // average value size = 10.1261
        // max type size = 50
        // max value size = 429
    }

    @Test fun testSignTransformation() {
        for (v0 in -10..10) {
            val v1 = signTransform(v0)
            val v2 = inverseSignTransform(v1)

            println("${v0.toUInt().toString(2).padStart(32, '0')} $v0")
            println("${v1.toUInt().toString(2).padStart(32, '0')} $v1")
            println("${v2.toUInt().toString(2).padStart(32, '0')} $v2")
            println()
        }
    }

    @Test fun testWriteInt64() {
        genLong.foreach(SplittableRandom(0), 1000) {
            println()
            val baos = ByteArrayOutputStream()
            val writer = Writer.from(DataOutputStream(baos))
            writer.writeZigZagVarLong(it)
            writer.close()

            val bytes = baos.toByteArray()
            println("writeVarInt($it) = ${bytes.joinToString(" ") { "%02X".format(it) }}")

            val reader = Reader.from(bytes)

            val result = reader.readZigZagVarLong()
            check(it == result) { "$it != $result" }

//            val result = reader.readValue(PhiType.Long) as Phi.Long
//            check(it == result.value) { "$it != $result" }
        }
    }

    @Test fun `regression 1`() {
        // test_effect{#0: null}
        val v0 = PhiNode.Compound(
            PhiNode.Key.String("test_effect"),
            mapOf(
                PhiNode.Key.Int(0) to PhiNode.Null
            )
        )

        val bytes = v0.toBytes()
        println(bytes.joinToString(" ") { "%02X".format(it) })
        println(bytes.joinToString(" ") { " ${it.toChar()}" })
    }

    @Test fun `regression 2`() {
        val v0 = PhiNode.Compound(
            PhiNode.Key.Int(0),
            mapOf(
                PhiNode.Key.Int(0) to PhiNode.Null,
                PhiNode.Key.Int(1) to PhiNode.Null
            )
        )

        val bytes = v0.toBytes()
        println(bytes.joinToString(" ") { "%02X".format(it) })

        val v1 = PhiNode.fromBytes(bytes)
        println(v1)
    }

    @Test fun `regression 7359648837940960612`() {
        // [#4{#6: -2121006527, #10: 89b}, #4{#10: [732801928l, 9.35068081E8d], tr: "A-25"}] :: List[#4: {#6: Int, #10: Any, tr: String}, 2]
        val v0 = PhiNode.List(
            PhiNode.Compound(
                4,
                PhiNode.Key.Int(6) to PhiNode.Int(-2121006527),
                PhiNode.Key.Int(10) to PhiNode.Byte(89)
            ),
            PhiNode.Compound(
                4,
                PhiNode.Key.Int(10) to PhiNode.List(
                    PhiNode.Long(732801928),
                    PhiNode.Double(9.35068081E8)
                ),
                PhiNode.Key.String("tr") to PhiNode.String("A-25")
            )
        )

        testValue(null, v0, debug = true)
        // testValue(null, 7359648837940960612, debug = true)
    }

    @Test fun `regression 7359648837940960612 simplified`() {
        val v0 = PhiNode.List(
            PhiNode.Compound(0, PhiNode.Key.Int(0) to PhiNode.Null),
            PhiNode.Compound(0, PhiNode.Key.Int(0) to PhiNode.Byte(1))
        )
        testValue(null, v0, debug = true)
    }
}

class PhiNodeSerializationSpec {
    @Serializable sealed interface TestADT {
        @Phi.Id(1) @Serializable data class A(val a: List<TestADT>) : TestADT
        @Phi.Id(2) @Serializable data class B(val b: Map<String, Byte?>) : TestADT
        @Phi.Id(3) @Serializable data class C(val c: Int?) : TestADT
        @Serializable sealed interface SubTestADT : TestADT
        @Serializable data object D : SubTestADT
        @Phi.Id(5) @Serializable data object E : SubTestADT
    }

    @Serializable @JvmInline value class TestInline(val value: List<TestADT>)

    val genTestADT: Gen<TestADT> by lazy {
        val self = Gen.delay { genTestADT }
        val smallInt = Gen.int(0..3)

        Gen.oneOfGen(
            Gen.pure(TestADT.D),
            Gen.pure(TestADT.E),
            Gen.int(-100..100).map { TestADT.C(it) },
            self.repeat(smallInt).map { TestADT.A(it) },
            (Gen.int.map { it.toString() } zip Gen.byte.nullable())
                .repeat(smallInt).map { TestADT.B(it.toMap()) }
        )
    }

    val genTestInline = Gen.repeat(Gen.int(0..3), genTestADT).map { TestInline(it) }

    @Test fun testPerformance() {
        val random = SplittableRandom(0)
        genTestInline.foreach(random, 1000000) { v0 ->
            val p0 = Phi.toPhiNode(v0)
            val bytes = p0.toBytes()
            val p1 = PhiNode.fromBytes(bytes)
            val v1 = Phi.fromPhiNode<TestInline>(p1)
            assertEquals(v0, v1)
        }
    }
}
