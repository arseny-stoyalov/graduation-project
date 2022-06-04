package gp

import enumeratum.{Enum, EnumEntry}
import gp.rows.model.Row
import gp.utils.routing.errors.{ApiError, ApiErrorLike}
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.JsonCodec
import io.circe.syntax._

import java.nio.charset.StandardCharsets

package object rows {

  object errors {

    sealed trait RowError extends ApiErrorLike

    object RowError {

      case object NotFound extends RowError {
        override def asApiError: ApiError = ApiError.NotFound()
      }

    }

    //todo TableError
    sealed trait InstanceError extends ApiErrorLike

    object InstanceError {

      case object NotFound extends InstanceError {
        override def asApiError: ApiError = ApiError.NotFound()
      }

      case class CreateFail(msg: String) extends InstanceError {
        override def asApiError: ApiError = ApiError.UnprocessableEntity(msg)
      }

      case class DropFail(msg: String) extends InstanceError {
        override def asApiError: ApiError = ApiError.UnprocessableEntity(msg)
      }

      case class IncompatibleTypes() extends InstanceError {
        override def asApiError: ApiError = ApiError.UnprocessableEntity("") //todo type errors
      }

    }

  }

  sealed private[rows] trait Action extends EnumEntry {
    def toBytes = (this: Action).asJson(Action.encoder).noSpaces.getBytes(StandardCharsets.UTF_8)
  }

  private[rows] object Action extends Enum[Action] {
    @JsonCodec
    case class Write(tableId: String, row: Row) extends Action {
      override val entryName: String = "Write"
    }

    @JsonCodec
    case class Delete(tableId: String, ids: List[String]) extends Action {
      override val entryName: String = "Delete"
    }

    @JsonCodec
    case class Erase(tableId: String) extends Action {
      override val entryName: String = "Erase"
    }

    implicit val decoder: Decoder[Action] = (c: HCursor) =>
      c.get[String]("type").flatMap {
        case "Delete" => c.as[Delete]
        case "Write" => c.as[Write]
        case "Erase" => c.as[Erase]
      }

    implicit val encoder: Encoder[Action] = (a: Action) => {
      val `type` =
        Json.obj("type" -> Json.fromString(a.entryName))
      val body = a match {
        case d: Delete => d.asJson
        case w: Write => w.asJson
        case e: Erase => e.asJson
      }

      body.deepMerge(`type`)
    }

    override def values: IndexedSeq[Action] = findValues
  }

}
