package gp.users.model

import io.circe.generic.JsonCodec

@JsonCodec
case class User(id: String, login: String, password: String)
