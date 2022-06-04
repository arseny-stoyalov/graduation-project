package gp.tables.model

import gp.columns.model.ColumnDescription
import gp.utils.formats.json.DateFormats._
import io.circe.generic.JsonCodec
import io.circe.syntax._
import io.circe.{DecodingFailure, Json}
import io.scalaland.chimney.dsl._

import java.util.{Date, UUID}

package object formats {

  object external {

    @JsonCodec
    case class InputTable(
      name: String,
      columns: List[ColumnDescription]
    ) {
      def asTable: Table =
        this.into[Table]
          .withFieldConst(_.id, null)
          .withFieldConst(_.created, null)
          .withFieldConst(_.createdBy, null)
          .transform
    }

  }

  object storage {

    case class StorageTable(
      id: UUID,
      name: String,
      columns: Json,
      created: Date,
      createdBy: UUID
    ) {
      def asTable: Either[DecodingFailure, Table] =
        columns
          .as[List[ColumnDescription]]
          .map { decodedColumns =>
            this
              .into[Table]
              .withFieldConst(_.columns, decodedColumns)
              .transform
          }
    }

    object StorageTable {
      def fromTable(table: Table): StorageTable =
        table
          .into[StorageTable]
          .withFieldComputed(_.columns, _.columns.asJson)
          .transform
    }

  }

}
