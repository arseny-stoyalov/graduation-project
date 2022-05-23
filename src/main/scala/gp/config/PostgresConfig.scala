package gp.config

case class PostgresConfig(host: String, port: Int, database: String, user: String, password: Option[String])
