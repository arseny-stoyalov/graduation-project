package gp.tables

import enumeratum.{Enum, EnumEntry}
import gp.tables.rows.model.Row
import gp.utils.routing.errors.{ApiError, ApiErrorLike}
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.JsonCodec
import io.circe.syntax._

import java.nio.charset.StandardCharsets
import java.util.UUID

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
    case class Write(tableId: UUID, row: Row) extends Action {
      override val entryName: String = "Write"
    }

    @JsonCodec
    case class Delete(tableId: UUID, id: UUID) extends Action {
      override val entryName: String = "Delete"
    }

    implicit val decoder: Decoder[Action] = (c: HCursor) =>
      c.get[String]("type").flatMap {
        case "Delete" => c.as[Delete]
        case "Write" => c.as[Write]
      }

    implicit val encoder: Encoder[Action] = (a: Action) => {
      val `type` =
        Json.obj("type" -> Json.fromString(a.entryName))
      val body = a match {
        case d: Delete => d.asJson
        case w: Write => w.asJson
      }

      body.deepMerge(`type`)
    }

    override def values: IndexedSeq[Action] = findValues
  }

}
