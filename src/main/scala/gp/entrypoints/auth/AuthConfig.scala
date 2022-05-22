package gp.entrypoints.auth

import com.typesafe.config.ConfigFactory
import gp.config.JWTConfig
import pureconfig.ConfigSource.fromConfig
import pureconfig.generic.auto._

case class AuthConfig(jwt: JWTConfig)

object AuthConfig {

  def apply(): AuthConfig = {
    val c = ConfigFactory.load("auth.conf")

    fromConfig(c)
      .loadOrThrow[AuthConfig]
  }

}
