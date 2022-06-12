package gp.entrypoints.auth

import com.typesafe.config.ConfigFactory
import gp.config.{JWTConfig, PostgresConfig}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class AuthNodeConfig(port: Int, jwt: JWTConfig, postgres: PostgresConfig)

object AuthNodeConfig {

  def apply(): AuthNodeConfig = {
    ConfigSource.fromConfig(ConfigFactory.load())
      .loadOrThrow[AuthNodeConfig]
  }

}
