package gp

import cats.Id
import cats.effect.{ExitCode, IO, IOApp}
import gp.entrypoints.auth.AuthServer
import gp.utils.catseffect._
import gp.entrypoints.tables.TableServer
import tofu.logging.Logging
import tofu.syntax.logging._

import java.util.concurrent.{ForkJoinPool, SynchronousQueue, ThreadPoolExecutor, TimeUnit}
import scala.concurrent.ExecutionContext

object Main extends IOApp {

  private lazy val parallelism = math.max(java.lang.Runtime.getRuntime.availableProcessors(), 4)

  private val logicScheduler = ExecutionContext
    .fromExecutor {
      new ForkJoinPool(parallelism)
    }

  private val ioScheduler = ExecutionContext
    .fromExecutor {
      new ThreadPoolExecutor(
        0,
        Int.MaxValue,
        60,
        TimeUnit.SECONDS,
        new SynchronousQueue[Runnable](false)
      )
    }

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioL: Id[Logging[IO]] = Logging.Make.plain[IO].byName(getClass.getCanonicalName)
    val start = Node.obtain match {
      case Node.Tables => new TableServer(logicScheduler, ioScheduler).run.as(ExitCode.Success)
      case Node.Auth => new AuthServer(logicScheduler).run.as(ExitCode.Success)
      case _ => IO.pure(ExitCode.Success)
    }

    start
      .handleErrorWith { e =>
        errorCause"failed start role process" (e).as(ExitCode.Error)
      }
      .evalOn(logicScheduler)
  }


}
