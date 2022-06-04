package gp.users.model

import gp.auth.model.AuthModel
import gp.core.models.HasId

import java.util.UUID

case class User(
  id: UUID,
  name: String,
  password: String
) extends AuthModel
    with HasId[User] {
  override def withId(id: UUID): User = copy(id = id)
}
