package gp.entrypoints.auth

import cats.data.EitherT
import cats.syntax.either._
import cats.syntax.semigroupk._
import cats.effect.IO
import gp.auth.AuthService
import gp.utils.routing.dsl._
import gp.users.model.User
import gp.utils.routing.dsl.errors.unauthorized
import gp.utils.routing.dsl.{AuthLogic, Route, RouteClass, UserAuthRoute}
import gp.utils.routing.errors.{ApiError, ApiErrorLike}
import org.http4s.HttpRoutes
import sttp.tapir._
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.swagger.http4s.SwaggerHttp4s

class AuthController(implicit as: AuthService[IO]) {

  private val testAuthRoute = {
    val ep =
      endpoint.get
        .in("alive")
        .out(stringBody)

    val logic: AuthLogic[IO, User, Unit, String] = u => _ => EitherT.fromEither[IO](s"Hello, $u".asRight[ApiErrorLike])

    new UserAuthRoute[IO, Unit, String](ep, logic, RouteClass.Auth)
  }

  private val loginRoute = {
    val ep =
      endpoint.post
        .in("login")
        .in(header[String]("login"))
        .in(header[String]("password"))
        .out(stringBody)
        .errorOut(oneOf[ApiError](unauthorized))

    val logic: Logic[IO, (String, String), String] = {case (login, password) => as.createToken(login, password)}

    new Route[IO, (String, String), String](ep, logic, RouteClass.Auth)
  }

  private val r: Routes[IO] = testAuthRoute ~> loginRoute

  private val doc = new SwaggerHttp4s(r.doc).routes[IO]

  def routes: HttpRoutes[IO] = doc <+> r.http4s

}
