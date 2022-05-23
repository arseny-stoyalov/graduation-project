package gp.entrypoints.tables

import gp.config.PostgresConfig
import pureconfig.generic.auto._
import pureconfig.ConfigSource

case class TableNodeConfig(postgres: PostgresConfig)

object TableNodeConfig {

  def apply(): TableNodeConfig = {
    ConfigSource.resources("tables.conf").loadOrThrow[TableNodeConfig]
  }

}
