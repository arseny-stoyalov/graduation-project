package gp.entrypoints.auth

import cats.effect.IO
import doobie.util.transactor.Transactor
import gp.auth.UserAuthService
import gp.services.{ServicesService, ServicesStorage}
import gp.users.UsersService
import gp.utils.catseffect._
import org.http4s.blaze.server.BlazeServerBuilder

import scala.concurrent.ExecutionContext

class AuthServer(logicScheduler: ExecutionContext) {

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

  def run: IO[Unit] =
    servicesService.init() >>
      BlazeServerBuilder[IO]
        .withExecutionContext(logicScheduler)
        .bindHttp(config.port, "0.0.0.0")
        .withHttpApp(controller.routes.orNotFound)
        .resource
        .use(_ => IO.never)

}
