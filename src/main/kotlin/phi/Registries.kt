//package phi
//
//import kotlinx.serialization.*
//import kotlinx.serialization.descriptors.*
//import kotlinx.serialization.encoding.Decoder
//import kotlinx.serialization.encoding.Encoder
//import kotlinx.serialization.modules.SerializersModule
//import kotlinx.serialization.modules.polymorphic
//import kotlin.reflect.KClass
//import kotlin.reflect.KType
//
//class Registries(val value: Registry<Registry<*>> = Registry()) {
//    operator fun <T> get(key: Key<Registry<T>>): Registry<T> =
//        value.get(key as Key<Registry<*>>) as Registry<T>
//
//    init {
//        value[Registries.REGISTRY] = value
//        value[Registries.EFFECT_TYPES as Key<Registry<*>>] = Registry<EffectType<*>>()
//    }
//
//    companion object {
//        val REGISTRY = Key<Registry<*>>("REGISTRY")
//        val EFFECT_TYPES = Key<Registry<EffectType<*>>>("EFFECT_TYPES")
//    }
//}
//
//data class Key<T>(val value: String)
//
//data class Holder<T>(val key: Key<T>, val value: T)
//
//open class Registry<T> {
//    private val byKey = mutableMapOf<Key<T>, Holder<T>>()
//    private val byValue = mutableMapOf<T, Holder<T>>()
//
//    operator fun set(key: Key<T>, value: T) {
//        byKey[key] = Holder(key, value)
//        byValue[value] = Holder(key, value)
//    }
//    operator fun get(key: Key<T>): T = byKey[key]?.value ?: error("No value for key $key")
//    fun getKeyByValue(value: T): Key<T> = byValue[value]?.key ?: error("No key for value $value")
//
//    operator fun iterator(): Iterator<Holder<T>> = byKey.values.iterator()
//
//    companion object {
//
//    }
//}
//
//
//interface PhiCodec<T : Any> {
//    data class Context(val registries: Registries) {
//        @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
//        val serializersModule: SerializersModule by lazy {
//            SerializersModule {
//                val effectSerializer = object : KSerializer<Effect<*>> {
//                    override val descriptor: SerialDescriptor get() {
//                        val effectTypes = registries[Registries.EFFECT_TYPES]
//                        return buildSerialDescriptor("Effect", PolymorphicKind.SEALED) {
//                            element<String>("name")
//                            element("value", buildSerialDescriptor("value", SerialKind.CONTEXTUAL) {
//                                for (effectType in effectTypes) {
//                                    element(
//                                        effectType.key.value,
//                                        buildSerialDescriptor(effectType.key.value, SerialKind.CONTEXTUAL)
//                                    )
//                                    // effectType.value.codec.serialDescriptor
//                                }
//                            })
//                        }
//                    }
//
//                    override fun deserialize(decoder: Decoder): Effect<*> {
//                        val effectTypes = registries[Registries.EFFECT_TYPES]
//                        val structure = decoder.beginStructure(descriptor)
//                        val name = structure.decodeStringElement(descriptor, 0)
//                        val type = effectTypes[Key(name)] as EffectType<Any>
//                        val serializer = serializersModule.serializer(type.codec.kClass as KType)
//                        val node = structure.decodeSerializableElement(descriptor, 1, serializer)
//                        structure.endStructure(descriptor)
//                        return node as Effect<*>
//                    }
//
//                    override fun serialize(encoder: Encoder, value: Effect<*>) {
//                        val effectTypes = registries[Registries.EFFECT_TYPES]
//                        val c = encoder.beginStructure(descriptor)
//                        c.encodeStringElement(descriptor, 0, effectTypes.getKeyByValue(value.type).value)
//                        val valueCodec = value.type.codec
//                        val serializer = serializersModule.serializer(value.type.codec.kClass as KType)
//                        c.encodeSerializableElement<Any>(
//                            descriptor, 1,
//                            serializer as KSerializer<Any>,
//                            value as Any
//                        )
//                        c.endStructure(descriptor)
//                    }
//                }
//
////                polymorphic(Effect.Some::class) {
////                    object : KSerializer<Effect.Some<*>> {
////                        override val descriptor: SerialDescriptor =
////                            effectSerializer.descriptor
////                        override fun deserialize(decoder: Decoder): Effect.Some<*> {
////                            val r = effectSerializer.deserialize(decoder)
////                            return Effect.Some(r)
////                        }
////                        override fun serialize(encoder: Encoder, value: Effect.Some<*>) {
////                            effectSerializer.serialize(encoder, value.get())
////                        }
////                    }
////                }
//                contextual(Effect::class) { effectSerializer }
//            }
//        }
//    }
//
////    val serialDescriptor: SerialDescriptor
////    val serializer: KSerializer<T>
//
//    val kClass: KClass<T>
//
//    fun fromTag(ctx: Context, tag: PhiNode): T
//    fun toTag(ctx: Context, value: T): PhiNode
//
//    companion object {
//        @OptIn(InternalSerializationApi::class)
//        inline fun <reified T : Any> fromSerializable(): PhiCodec<T> {
//            val serializer = T::class.serializer()
//            return object : PhiCodec<T> {
//                override val kClass: KClass<T> = T::class
////                override val serialDescriptor = serializer.descriptor
////                override val serializer = serializer
//                override fun fromTag(ctx: PhiCodec.Context, tag: PhiNode): T =
//                    Phi.fromPhiNode(serializer, tag, ctx.serializersModule)
//                override fun toTag(ctx: PhiCodec.Context, value: T): PhiNode =
//                    Phi.toPhiNode<T>(serializer, value, ctx.serializersModule)
//            }
//        }
//    }
//}
//
//interface EffectType<T : Any> {
//    val codec: PhiCodec<T>
//}
//interface Effect<T : Any> {
//    val type: EffectType<T>
//    fun asEffect(): T
//    fun toPhi(ctx: PhiCodec.Context): PhiNode = type.codec.toTag(ctx, asEffect())
//}
//
//@Serializable
//data class TestEffect(@Phi.Id(0) val value: Int) : Effect<TestEffect> {
//    override val type: EffectType<TestEffect> get() = TYPE
//    override fun asEffect(): TestEffect = this
//
//    companion object {
//        val CODEC: PhiCodec<TestEffect> = PhiCodec.fromSerializable()
//        val TYPE = object : EffectType<TestEffect> {
//            override val codec: PhiCodec<TestEffect> = CODEC
//        }
//    }
//}
