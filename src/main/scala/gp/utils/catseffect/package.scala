package gp.utils

import cats.effect.IO
import tofu.Delay
import tofu.logging.Logging

import scala.concurrent.duration.FiniteDuration

package object catseffect {

  implicit class IOExtension[T](val io: IO[T]) extends AnyVal {

    def onErrorRestartWithDelay(
      delay: FiniteDuration,
      log: Throwable => IO[Unit]
    ): IO[T] =
      io.handleErrorWith { e =>
        log(e) >> IO.sleep(delay) >> io.onErrorRestartWithDelay(delay, log)
      }

  }

  implicit val ioDelay: Delay[IO] = new Delay[IO] {
    override def delay[A](a: => A): IO[A] = IO.delay(a)
  }

  implicit val ioLogging: Logging.Make[IO] =
    Logging.Make.plain[IO]

}
