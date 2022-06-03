package gp.entrypoints.tables

import gp.config.{JWTConfig, PostgresConfig}
import pureconfig.generic.auto._
import pureconfig.ConfigSource

case class TableNodeConfig(port: Int, postgres: PostgresConfig, jwt: JWTConfig)

object TableNodeConfig {

  def apply(): TableNodeConfig = {
    ConfigSource.resources("tables.conf").loadOrThrow[TableNodeConfig]
  }

}
