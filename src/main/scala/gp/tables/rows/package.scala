package gp.tables

import enumeratum.{Enum, EnumEntry}
import gp.services.model.Service
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

    sealed trait InstanceError extends ApiErrorLike

    object InstanceError {

      case class ColumnMissing(id: String) extends InstanceError {
        override def asApiError: ApiError = ApiError.UnprocessableEntity(s"Missing column $id")
      }

      case class IncompatibleTypes(columnId: String, expectedType: String) extends InstanceError {
        override def asApiError: ApiError =
          ApiError.UnprocessableEntity(s"Column $columnId should be of type $expectedType")
      }

      case object NotFound extends InstanceError {
        override def asApiError: ApiError = ApiError.NotFound()
      }

    }

  }

  sealed private[rows] trait Action extends EnumEntry {
    def toBytes = (this: Action).asJson(Action.encoder).noSpaces.getBytes(StandardCharsets.UTF_8)
  }

  private[rows] object Action extends Enum[Action] {
    @JsonCodec
    case class Write(tableId: UUID, row: Map[String, Json], sentBy: Service, started: Long) extends Action {
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
