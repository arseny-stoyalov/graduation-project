package gp.auth.model

import io.circe.generic.JsonCodec

import java.util.UUID

trait AuthModel

object AuthModel {

  @JsonCodec
  case class UserToken(userId: UUID)

}
