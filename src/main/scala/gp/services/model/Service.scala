package gp.services.model

import gp.auth.model.AuthModel
import gp.core.models.{HasCreationDate, HasCreator, HasId}
import io.circe.generic.JsonCodec
import gp.utils.formats.json.DateFormats._

import java.util.{Date, UUID}

@JsonCodec
case class Service(
  id: UUID,
  name: String,
  apiKey: String,
  created: Date,
  createdBy: UUID
) extends AuthModel
    with HasId[Service]
    with HasCreationDate[Service]
    with HasCreator[Service] {

  override def withId(id: UUID): Service = copy(id = id)
  override def withCreated(date: Date): Service = copy(created = date)
  override def withCreatedBy(id: UUID): Service = copy(createdBy = id)

  def withApiKey(apiKey: String) = copy(apiKey = apiKey)

}
