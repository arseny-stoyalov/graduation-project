package gp.users

import cats.Monad
import cats.data.EitherT
import cats.effect.kernel.Async
import cats.syntax.either._
import cats.syntax.functor._
import gp.auth.UserAuthService
import gp.users.errors.UserError
import gp.users.model.User
import gp.utils.routing.dsl.{AuthLogic, Routes, UserAuthedRoute}
import gp.utils.routing.tags.RouteTag
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{endpoint, statusCode}

class UsersController[F[_]: Async: Monad](usersService: UsersService[F])(implicit as: UserAuthService[F]) {

  private val tag = RouteTag.Users

  private val add = {
    val ep = endpoint.post
      .in(jsonBody[User])
      .out(statusCode(StatusCode.Accepted))

    val logic: AuthLogic[F, User, User, Unit] = _ =>
      input => EitherT(usersService.put(input.id, input).void.map(_.asRight[UserError]))

    new UserAuthedRoute(ep, logic, tag)
  }

  val routes: Routes[F] = add

}
