package gp.tables.model

import gp.columns.model.ColumnDescription
import io.circe.syntax._
import io.circe.{DecodingFailure, Json}
import io.scalaland.chimney.dsl._

import java.util.UUID

package object formats {

  case class StorageTable(
    id: UUID,
    name: String,
    columns: Json,
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
