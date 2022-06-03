package gp.config

case class PostgresConfig(host: String, port: Int, database: String, user: String, password: String) {

  def url = s"jdbc:postgresql://$host:$port/$database"

}
