package gp.tables.model

import gp.columns.model.ColumnDescription
import io.circe.generic.JsonCodec

import java.util.UUID

@JsonCodec
case class Table(id: UUID, name: String, columns: List[ColumnDescription], createdBy: UUID)
