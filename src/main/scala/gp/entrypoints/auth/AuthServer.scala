package gp.entrypoints.auth

import cats.Id
import cats.effect.{ExitCode, IO, IOApp}
import gp.auth.AuthService
import gp.users.UsersService
import gp.users.model.User
import org.http4s.blaze.server.BlazeServerBuilder
import tofu.Delay
import tofu.logging.Logging
import tofu.syntax.logging._

import java.util.concurrent.ForkJoinPool
import scala.concurrent.ExecutionContext

object AuthServer extends IOApp {

  //-------------utils--------------
  implicit val ioDelay: Delay[IO] = new Delay[IO] {
    override def delay[A](a: => A): IO[A] = IO.delay(a)
  }

  implicit val ioLogging: Logging.Make[IO] =
    Logging.Make.plain[IO]

  //--------------------------------

  private lazy val parallelism = math.max(java.lang.Runtime.getRuntime.availableProcessors(), 4)

  private val logicScheduler = ExecutionContext
    .fromExecutor {
      new ForkJoinPool(parallelism)
    }

  private val config = AuthConfig()

  override def run(args: List[String]): IO[ExitCode] = {
    //todo delete me
    import com.github.t3hnar.bcrypt._

    implicit val ioL: Id[Logging[IO]] = Logging.Make.plain[IO].byName(getClass.getCanonicalName)

    val userService = new UsersService.InMemory
    implicit val authService: AuthService[IO] = new AuthService[IO](config.jwt, userService)

    val controller = new AuthController()

    val server = for {
      started <- IO.delay(System.currentTimeMillis())
      _ <- info"system started in ${System.currentTimeMillis() - started} ms"
      _ <- info"system cpu count: ${Runtime.getRuntime.availableProcessors()}"

      builder = BlazeServerBuilder[IO](logicScheduler)
        .bindHttp(9091, "0.0.0.0")
        .withHttpApp(controller.routes.orNotFound)
    } yield builder

    server.flatMap(_.resource.use(_ => IO.never)).as(ExitCode.Success)
      .handleErrorWith { e =>
        errorCause"failed start role process" (e).as(ExitCode.Error)
      }
      .evalOn(logicScheduler)

  }

}
