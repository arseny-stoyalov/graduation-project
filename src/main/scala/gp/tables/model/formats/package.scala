package gp.tables.model

import gp.columns.model.ColumnDescription
import io.circe.syntax.EncoderOps
import io.circe.{DecodingFailure, Json}
import io.scalaland.chimney.dsl._

package object formats {

  case class StorageTable(
    id: String,
    name: String,
    columns: Json,
    createdBy: String
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
