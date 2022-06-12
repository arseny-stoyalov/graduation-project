package gp.entrypoints.tables

import com.typesafe.config.ConfigFactory
import gp.config.{JWTConfig, PostgresConfig}
import pureconfig.generic.auto._
import pureconfig.ConfigSource

case class TableNodeConfig(port: Int, postgres: PostgresConfig, jwt: JWTConfig, bootstrapServer: String)

object TableNodeConfig {

  def apply(): TableNodeConfig =
    ConfigSource
      .fromConfig(ConfigFactory.load())
      .loadOrThrow[TableNodeConfig]

}
