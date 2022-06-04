package gp.tables.rows.model

import gp.columns.values.Value
import gp.utils.formats.json.DateFormats._
import io.circe.{DecodingFailure, Json}
import io.circe.generic.JsonCodec
import io.circe.syntax._
import io.scalaland.chimney.dsl._

import java.util.{Date, UUID}

package object formats {

  object external {

    @JsonCodec
    case class OutputRow(
      id: UUID,
      entity: Json,
      created: Date,
      createdBy: UUID
    )

    object OutputRow {
      def fromRow(r: Row): OutputRow =
        r.into[OutputRow]
          .withFieldConst(_.entity, r.entity.view.mapValues(value => value.asJson(Value.encoder)).toMap.asJson)
          .transform
    }

  }

  object storage {

    @JsonCodec
    case class StorageRow(
      id: UUID,
      entity: Json,
      created: Date,
      createdBy: UUID
    ) {
      def asRow: Either[DecodingFailure, Row] =
        entity.as[Map[String, Value]].map { e =>
          this.into[Row]
            .withFieldConst(_.entity, e)
            .transform
        }

    }

    object StorageRow {
      def fromRow(r: Row): StorageRow =
        r.into[StorageRow]
          .withFieldConst(_.entity, r.entity.view.mapValues(value => value.asJson(Value.encoder)).toMap.asJson)
          .transform
    }

  }

}
