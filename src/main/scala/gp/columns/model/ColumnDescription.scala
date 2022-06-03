package gp.columns.model

import gp.columns.ColumnType
import io.circe.generic.JsonCodec

@JsonCodec
case class ColumnDescription(id: String, `type`: ColumnType)
