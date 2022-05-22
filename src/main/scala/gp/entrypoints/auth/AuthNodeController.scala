package gp.entrypoints.auth

import cats.effect.IO
import cats.syntax.semigroupk._
import gp.auth.AuthService
import gp.auth.controller.AuthController
import gp.utils.routing.dsl._
import org.http4s.HttpRoutes
import sttp.tapir.swagger.http4s.SwaggerHttp4s

class AuthNodeController(implicit as: AuthService[IO]) {

 private val authRoutes = new AuthController().routes

  private val r: Routes[IO] = authRoutes

  private val doc = new SwaggerHttp4s(r.doc, List("auth", "docs")).routes[IO]

  def routes: HttpRoutes[IO] = doc <+> r.http4s

}
