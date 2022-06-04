package gp.tables.model

import gp.columns.model.ColumnDescription
import gp.core.models.{HasCreationDate, HasCreator, HasId}
import gp.utils.formats.json.DateFormats._
import io.circe.generic.JsonCodec

import java.util.{Date, UUID}

@JsonCodec
case class Table(
  id: UUID,
  name: String,
  columns: List[ColumnDescription],
  created: Date,
  createdBy: UUID
) extends HasId[Table]
    with HasCreationDate[Table]
    with HasCreator[Table] {

  override def withId(id: UUID): Table = copy(id = id)
  override def withCreated(date: Date): Table = copy(created = date)
  override def withCreatedBy(id: UUID): Table = copy(createdBy = id)

}
