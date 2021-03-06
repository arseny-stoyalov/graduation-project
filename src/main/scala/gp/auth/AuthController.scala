package gp.auth

import cats.MonadError
import cats.data.EitherT
import cats.effect.kernel.Async
import cats.syntax.either._
import gp.auth.AuthController.TokenCreatedResponse
import gp.users.model.User
import gp.utils.routing.dsl._
import gp.utils.routing.dsl.errors.unauthorized
import gp.utils.routing.errors.{ApiError, ApiErrorLike}
import gp.utils.routing.tags.RouteTag
import io.circe.generic.JsonCodec
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody

class AuthController[F[_]](implicit as: UserAuthService[F], F: Async[F] with MonadError[F, Throwable]) {

  private val tag = RouteTag.Auth

  private val login = {
    val ep =
      endpoint.post
        .in("login")
        .in(header[String]("login"))
        .in(header[String]("password"))
        .out(jsonBody[TokenCreatedResponse])
        .errorOut(oneOf[ApiError](unauthorized))

    val logic: Logic[F, (String, String), TokenCreatedResponse] = { case (login, password) =>
      as.createToken(login, password).map(TokenCreatedResponse.apply)
    }

    new Route[F, (String, String), TokenCreatedResponse](ep, logic, tag)
  }

  val routes: Routes[F] = login

}

object AuthController {

  @JsonCodec
  case class TokenCreatedResponse(token: String)

}
