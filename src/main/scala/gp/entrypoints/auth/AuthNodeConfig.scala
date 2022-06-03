package gp.entrypoints.auth

import gp.config.JWTConfig
import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class AuthNodeConfig(port: Int, jwt: JWTConfig)

object AuthNodeConfig {

  def apply(): AuthNodeConfig = {
    ConfigSource.resources("auth.conf")
      .loadOrThrow[AuthNodeConfig]
  }

}
