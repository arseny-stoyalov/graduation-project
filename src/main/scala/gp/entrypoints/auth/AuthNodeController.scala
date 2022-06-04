package gp.entrypoints.auth

import cats.MonadError
import cats.effect.kernel.Async
import cats.syntax.semigroupk._
import gp.auth.{AuthController, AuthService}
import gp.utils.routing.dsl._
import org.http4s.HttpRoutes
import sttp.tapir.swagger.http4s.SwaggerHttp4s

class AuthNodeController[F[_]](implicit as: AuthService[F], F: Async[F] with MonadError[F, Throwable]) {

 private val authRoutes = new AuthController().routes

  private val r: Routes[F] = authRoutes

  private val doc = new SwaggerHttp4s(r.doc).routes[F]

  def routes: HttpRoutes[F] = doc <+> r.http4s

}
