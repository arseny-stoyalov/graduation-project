package gp.tables.model

import gp.columns.model.ColumnDescription
import io.circe.generic.JsonCodec

@JsonCodec
case class Table(id: String, name: String, columns: List[ColumnDescription], createdBy: String)
