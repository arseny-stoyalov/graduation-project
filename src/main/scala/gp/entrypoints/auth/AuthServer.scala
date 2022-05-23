package gp.entrypoints.auth

import cats.Id
import cats.effect.{ExitCode, IO, IOApp}
import gp.auth.AuthService
import gp.entrypoints.logicScheduler
import gp.users.UsersService
import gp.utils.catseffect._
import org.http4s.blaze.server.BlazeServerBuilder
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

private object AuthServer extends IOApp {

  val config: AuthNodeConfig = AuthNodeConfig()

  val userService = new UsersService.InMemory

  implicit val authService: AuthService[IO] = new AuthService[IO](config.jwt, userService)

  val controller = new AuthNodeController()

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioL: Id[Logging[IO]] = Logging.Make.plain[IO].byName(getClass.getCanonicalName)

    BlazeServerBuilder[IO]
      .withExecutionContext(logicScheduler)
      .bindHttp(9091, "0.0.0.0")
      .withHttpApp(controller.routes.orNotFound)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
      .handleErrorWith { e =>
        errorCause"failed start role process" (e).as(ExitCode.Error)
      }
  }

}
