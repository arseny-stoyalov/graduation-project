package gp

import cats.Monad
import org.http4s.dsl.impl._
import org.http4s.dsl.io.{GET, Ok, Root}
import org.http4s.{HttpRoutes, Response}

object AliveRoute {
  def apply[F[_]](implicit F: Monad[F]): HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "alive" =>
    F.pure(Response(status = Ok).withEntity("alive"))
  }
}
