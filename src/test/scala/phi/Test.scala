package phi1

import phi.PhiNode

import magnolia1.*
import kotlin.Pair
import org.jetbrains.annotations.NotNull

case class Key[A](name: String)

class Holder[A](val key: Key[A], val value: A)

class Registry[T]:
  private val byKey = collection.mutable.Map.empty[String, Holder[T]]
  private val byValue = collection.mutable.Map.empty[T, Holder[T]]

  def byKey(name: Key[T]): Holder[T] | Null =
    byKey.get(name.name).orNull

  def byValue(value: T): Option[Holder[T]] =
    byValue.get(value)

  def register(name: Key[T], value: T): Unit =
    byKey(name.name) = Holder(name, value)
    byValue(value) = Holder(name, value)

object Registries:
  val EFFECT_TYPES = Key[Registry[EffectType[?]]]("EFFECT_TYPES")

trait CodecContext:
  def registry[T](name: Key[Registry[T]] @NotNull): Registry[T]

case class PhiCodec[T](reader: PhiReader[T], writer: PhiWriter[T]):
  def encode(ctx: CodecContext, value: T): PhiNode =
    reader.encode(ctx, value)
  def decode(ctx: CodecContext, value: PhiNode): T =
    writer.decode(ctx, value)

//trait PhiReader[T]:
//  def encode(ctx: CodecContext, value: T): PhiNode

trait PhiWriter[T]:
  def decode(ctx: CodecContext, value: PhiNode): T

trait EffectType[T]:
  def codec: PhiCodec[T]

trait SomeEffect:
  type T
  val effectType: EffectType[T]
  val value: T

object SomeEffect:
  def apply[T0](type0: EffectType[T0], value0: T0): SomeEffect =
    new SomeEffect:
      type T = T0
      val effectType = type0
      val value = value0

//  val codec = new PhiCodec[SomeEffect]:
//    def encode(ctx: CodecContext, value: SomeEffect): PhiNode =
//      val registry = ctx.registry(Registries.EFFECT_TYPES)
//      val holder = registry.byValue(value.`type`).get
//      val codec: PhiCodec[value.T] = value.`type`.codec
//      val node = codec.encode(ctx, value.value)
//      new PhiNode.Compound(
//        PhiNode.Key.String(holder.key.name),
//        new Pair(PhiNode.Key.Int(0), node)
//      )
//
//    def decode(ctx: CodecContext, value: Phi): SomeEffect =
//      val registry = ctx.registry(Registries.EFFECT_TYPES)
//      val key = value.asInstanceOf[PhiNode.Compound].getType.asInstanceOf[PhiNode.Key.String].getValue
//      val holder = registry.byKey(Key(key)).get
//      val codec: PhiCodec[_] = holder.value.codec
//      val node: PhiNode = value.asInstanceOf[PhiNode.Compound].getValue.get(PhiNode.Key.Int(0))
//      SomeEffect(holder.value, codec.decode(ctx, node))
//

trait Print[T] {
  extension (x: T) def print: String
}

object Print extends AutoDerivation[Print]:
  def join[T](ctx: CaseClass[Print, T]): Print[T] = value =>
    ctx.params.map { param =>
      param.typeclass.print(param.deref(value))
    }.mkString(s"${ctx.typeInfo.short}(", ",", ")")

  override def split[T](ctx: SealedTrait[Print, T]): Print[T] = value =>
    ctx.choose(value) { sub => sub.typeclass.print(sub.cast(value)) }

  given Print[Int] = _.toString
