package gp.auth.model

import io.circe.generic.JsonCodec

import java.util.UUID

//todo This is designed kinda poorly
trait AuthModel

object AuthModel {

  @JsonCodec
  case class UserToken(userId: UUID)

}