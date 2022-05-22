package gp.utils

import cats.effect.IO
import tofu.Delay
import tofu.logging.Logging

package object catseffect {

  implicit val ioDelay: Delay[IO] = new Delay[IO] {
    override def delay[A](a: => A): IO[A] = IO.delay(a)
  }

  implicit val ioLogging: Logging.Make[IO] =
    Logging.Make.plain[IO]

}
