package gp.entrypoints.auth

import cats.MonadError
import cats.effect.kernel.Async
import cats.syntax.semigroupk._
import gp.AliveRoute
import gp.auth.{AuthController, UserAuthService}
import gp.services.{ServicesController, ServicesService}
import gp.users.{UsersController, UsersService}
import gp.utils.routing.dsl._
import org.http4s.HttpRoutes
import sttp.tapir.swagger.http4s.SwaggerHttp4s

class AuthNodeController[F[_]](implicit
  as: UserAuthService[F],
  us: UsersService[F],
  ss: ServicesService[F],
  F: Async[F] with MonadError[F, Throwable]
) {

  private val authRoutes = new AuthController().routes
  private val servicesRoutes = new ServicesController(ss).routes
  private val usersRoutes = new UsersController(us).routes

  private val r: Routes[F] = authRoutes ~> servicesRoutes ~> usersRoutes

  private val doc = new SwaggerHttp4s(r.doc, List("auth", "docs")).routes[F]

  def routes: HttpRoutes[F] = doc <+> r.http4s <+> AliveRoute.apply

}
