package gp.rows.model

import gp.utils.formats.json.DateFormats._
import io.circe.Json
import io.circe.generic.JsonCodec

import java.util.{Date, UUID}

@JsonCodec
case class Row(
  id: UUID,
  entity: Json,
  created: Date,
  createdBy: UUID
)