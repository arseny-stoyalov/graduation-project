package gp.entrypoints.tables

import cats.Id
import cats.effect.{ExitCode, IO, IOApp, Resource}
import gp.columns.ColumnDescription
import gp.utils.catseffect._
import natchez.Trace.Implicits.noop
import skunk._
import skunk.codec.all._
import skunk.implicits._
import tofu.logging.Logging

private object TableServer extends IOApp {

  val config: TableNodeConfig = TableNodeConfig()
  val controller: TableNodeController[IO] = new TableNodeController[IO]()

  val session: Resource[IO, Session[IO]] =
    Session.single(
      host = config.postgres.host,
      port = config.postgres.port,
      user = config.postgres.user,
      database = config.postgres.database,
      password = config.postgres.password
    )

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioL: Id[Logging[IO]] = Logging.Make.plain[IO].byName(getClass.getCanonicalName)

    val string = "sdafjlksdf"
    val cs = List(ColumnDescription("text1"), ColumnDescription("text2"))
    session.use { s =>
      for {
        _ <- s.execute(sql"create table test_table_#$string (name varchar)".command)
        _ <- IO.println(s"Table added")
      } yield ExitCode.Success
    }
  }

}
