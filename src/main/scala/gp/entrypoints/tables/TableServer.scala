package gp.entrypoints.tables

import cats.Id
import cats.effect.{ExitCode, IO, IOApp}
import doobie._
import gp.auth.AuthService
import gp.entrypoints.logicScheduler
import gp.tables.{TablesService, TablesStorage}
import gp.users.UsersService
import gp.utils.catseffect._
import org.http4s.blaze.server.BlazeServerBuilder
import tofu.logging.Logging
import tofu.syntax.logging._

private object TableServer extends IOApp {

  val config: TableNodeConfig = TableNodeConfig()

  implicit private val transactor: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    config.postgres.url,
    config.postgres.user,
    config.postgres.password
  )

  //auth
  val userService = new UsersService.InMemory
  implicit val authService: AuthService[IO] = new AuthService[IO](config.jwt, userService)

  //tables
  val tablesStorage = new TablesStorage.Postgres[IO]()
  val tablesService = new TablesService[IO](tablesStorage)

  val controller: TableNodeController[IO] = new TableNodeController[IO](tablesService)

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioL: Id[Logging[IO]] = Logging.Make.plain[IO].byName(getClass.getCanonicalName)

    tablesService.init() >>
    BlazeServerBuilder[IO]
      .withExecutionContext(logicScheduler)
      .bindHttp(config.port, "0.0.0.0")
      .withHttpApp(controller.routes.orNotFound)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
      .handleErrorWith { e =>
        errorCause"failed start role process" (e).as(ExitCode.Error)
      }

  }

}
