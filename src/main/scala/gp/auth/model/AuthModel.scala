package gp.auth.model

import io.circe.generic.JsonCodec

trait AuthModel

object AuthModel {

  @JsonCodec
  case class UserToken(userId: String) extends AuthModel

}
