package gp

import cats.effect.IO
import org.http4s.dsl.impl._
import org.http4s.dsl.io.{GET, Ok, Root}
import org.http4s.{HttpRoutes, Response}

object AliveRoute {
  def apply: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root / "alive" =>
    IO(Response(status = Ok).withEntity("alive"))
  }
}
