package gp.entrypoints.tables

import cats.Id
import cats.effect.{ExitCode, IO, IOApp}
import doobie._
import gp.utils.catseffect._
import tofu.logging.Logging

private object TableServer extends IOApp {

  val config: TableNodeConfig = TableNodeConfig()
  val controller: TableNodeController[IO] = new TableNodeController[IO]()

  implicit private val transactor: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    config.postgres.url,
    config.postgres.user,
    config.postgres.password
  )

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioL: Id[Logging[IO]] = Logging.Make.plain[IO].byName(getClass.getCanonicalName)

    IO(ExitCode.Success)

  }

}
