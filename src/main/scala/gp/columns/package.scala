package gp

import cats.syntax.functor._
import enumeratum._
import io.circe._
import io.circe.syntax._

package object columns {

  sealed trait ColumnType extends EnumEntry

  object ColumnType extends CirceEnum[ColumnType] with Enum[ColumnType] {
    case object String extends ColumnType
    case object Int extends ColumnType

    override def values: IndexedSeq[ColumnType] = findValues

  }

  object values {

    sealed trait Value {
      type T
      def value: T
    }

    object Value {

      implicit val encoder: Encoder[Value] = Encoder.instance {
        case string @ StringValue(_) => string.asJson
        case int @ IntValue(_) => int.asJson
      }

      implicit val decoder: Decoder[Value] =
        List[Decoder[Value]](
          Decoder[StringValue].widen,
          Decoder[IntValue].widen,
        ).reduceLeft(_ or _)

    }

    case class StringValue(value: String) extends Value {
      override type T = String
    }

    object StringValue {
      implicit val decoder: Decoder[StringValue] = Decoder[String].map(StringValue.apply)
      implicit val encoder: Encoder[StringValue] = Encoder[String].contramap(_.value)
    }

    case class IntValue(value: Int) extends Value {
      override type T = Int
    }

    object IntValue {
      implicit val decoder: Decoder[IntValue] = Decoder[Int].map(IntValue.apply)
      implicit val encoder: Encoder[IntValue] = Encoder[Int].contramap(_.value)
    }

  }

}
