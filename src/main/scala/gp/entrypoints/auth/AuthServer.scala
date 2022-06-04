package gp.entrypoints.auth

import cats.Id
import cats.effect.{ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import gp.auth.UserAuthService
import gp.entrypoints.logicScheduler
import gp.services.{ServicesService, ServicesStorage}
import gp.users.UsersService
import gp.utils.catseffect._
import org.http4s.blaze.server.BlazeServerBuilder
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

private object AuthServer extends IOApp {

  val config: AuthNodeConfig = AuthNodeConfig()

  implicit private val transactor: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    config.postgres.url,
    config.postgres.user,
    config.postgres.password
  )

  val userService = new UsersService.InMemory
  implicit val authService: UserAuthService[IO] = new UserAuthService[IO](config.jwt, userService)

  val servicesStorage = new ServicesStorage.Postgres[IO]()
  implicit val servicesService: ServicesService[IO] = new ServicesService[IO](servicesStorage)

  val controller = new AuthNodeController()

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioL: Id[Logging[IO]] = Logging.Make.plain[IO].byName(getClass.getCanonicalName)

    servicesService.init() >>
      BlazeServerBuilder[IO]
        .withExecutionContext(logicScheduler)
        .bindHttp(config.port, "localhost")
        .withHttpApp(controller.routes.orNotFound)
        .resource
        .use(_ => IO.never)
        .as(ExitCode.Success)
        .handleErrorWith { e =>
          errorCause"failed start role process" (e).as(ExitCode.Error)
        }
  }

}
