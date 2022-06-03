package gp.rows.model

import io.circe.Json
import io.circe.generic.JsonCodec
import gp.utils.formats.json.DateFormats._

import java.util.Date

@JsonCodec
case class Row(
  id: String,
  entity: Json,
  created: Date,
  createdBy: String
)
