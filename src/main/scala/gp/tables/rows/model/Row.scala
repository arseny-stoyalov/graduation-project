package gp.tables.rows.model

import gp.columns.values.Value
import gp.core.models.{HasCreationDate, HasCreator, HasId}

import java.util.{Date, UUID}

case class Row(
  id: UUID,
  entity: Map[String, Value],
  created: Date,
  createdBy: UUID
) extends HasId[Row]
    with HasCreationDate[Row]
    with HasCreator[Row] {

  override def withId(id: UUID): Row = copy(id = id)
  override def withCreated(date: Date): Row = copy(created = date)
  override def withCreatedBy(id: UUID): Row = copy(createdBy = id)

}

object Row {

  def fromEntity(e: Map[String, Value]) = Row(null, e, null, null)

}
