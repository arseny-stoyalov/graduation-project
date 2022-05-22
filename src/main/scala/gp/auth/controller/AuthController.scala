package gp.auth.controller

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.either._
import gp.auth.AuthService
import gp.auth.controller.AuthController.TokenCreatedResponse
import gp.users.model.User
import gp.utils.routing.tags.RouteTag
import gp.utils.routing.dsl.errors.unauthorized
import gp.utils.routing.dsl._
import gp.utils.routing.errors.{ApiError, ApiErrorLike}
import io.circe.generic.JsonCodec
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody

class AuthController(implicit as: AuthService[IO]) {

  private val tag = RouteTag.Auth

  private val testAuthRoute = "debug" ->: {
    val ep =
      endpoint.get
        .in("greet")
        .out(stringBody)

    val logic: AuthLogic[IO, User, Unit, String] =
      u => _ => EitherT.fromEither[IO](s"Hello, ${u.login}".asRight[ApiErrorLike])

    new UserAuthRoute[IO, Unit, String](ep, logic, tag)
  }

  private val loginRoute = {
    val ep =
      endpoint.post
        .in("login")
        .in(header[String]("login"))
        .in(header[String]("password"))
        .out(jsonBody[TokenCreatedResponse])
        .errorOut(oneOf[ApiError](unauthorized))

    val logic: Logic[IO, (String, String), TokenCreatedResponse] = { case (login, password) =>
      as.createToken(login, password).map(TokenCreatedResponse.apply)
    }

    new Route[IO, (String, String), TokenCreatedResponse](ep, logic, tag)
  }

  val routes: Routes[IO] = testAuthRoute ~> loginRoute

}

object AuthController {

  @JsonCodec
  case class TokenCreatedResponse(token: String)

}
