package gp.entrypoints.tables

import cats.Id
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.catsSyntaxApplicativeId
import doobie._
import doobie.implicits._
import gp.entrypoints.logicScheduler
import gp.utils.catseffect._
import org.http4s.blaze.server.BlazeServerBuilder
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

private object TableServer extends IOApp {

  val controller: TableNodeController[IO] = new TableNodeController[IO]()

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioL: Id[Logging[IO]] = Logging.Make.plain[IO].byName(getClass.getCanonicalName)

    val program1 = 42.pure[ConnectionIO]

    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql://localhost:5433/yugabyte",
      "yugabyte",
      ""
    )

    program1.transact(xa)
      .map(println)
      .as(ExitCode.Success)

//    BlazeServerBuilder[IO](logicScheduler)
//      .bindHttp(9091, "0.0.0.0")
//      .withHttpApp(controller.routes.orNotFound)
//      .resource
//      .use(_ => IO.never)
//      .as(ExitCode.Success)
//      .handleErrorWith { e =>
//        errorCause"failed start role process" (e).as(ExitCode.Error)
//      }
  }

}
