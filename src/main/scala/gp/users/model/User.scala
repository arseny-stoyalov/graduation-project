package gp.users.model

import gp.auth.model.AuthModel
import gp.core.models.HasId
import io.circe.generic.JsonCodec

import java.util.UUID

@JsonCodec
case class User(
  id: UUID,
  name: String,
  password: String
) extends AuthModel
    with HasId[User] {
  override def withId(id: UUID): User = copy(id = id)
}
