package gp.services

import cats.Monad
import cats.data.EitherT
import cats.syntax.functor._
import cats.syntax.either._
import cats.effect.kernel.Async
import gp.auth.UserAuthService
import gp.services.errors.ServiceError
import gp.services.model.Service
import gp.services.model.formats.external.InputService
import gp.users.model.User
import gp.utils.routing.dsl.{AuthLogic, Routes, UserAuthedRoute}
import gp.utils.routing.tags.RouteTag
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._

import java.util.UUID

class ServicesController[F[_]: Async: Monad](servicesService: ServicesService[F])(implicit as: UserAuthService[F]) {

  private val tag = RouteTag.Services

  private val get = {
    val ep = endpoint.get
      .in(path[UUID]("id"))
      .out(jsonBody[Service])

    val logic: AuthLogic[F, User, UUID, Service] = implicit user =>
      id => EitherT(servicesService.get(id).map(_.toRight(ServiceError.NotFound)))

    new UserAuthedRoute(ep, logic, tag)
  }

  private val search = {
    type Size = Option[Int]
    type Offset = Option[Int]

    val ep = endpoint.get
      .in(query[Size]("size"))
      .in(query[Offset]("offset"))
      .out(jsonBody[List[Service]])

    val logic: AuthLogic[F, User, (Size, Offset), List[Service]] = implicit user => {
      case (size, offset) =>
        EitherT(
          servicesService
            .search(size, offset)
            .map(_.asRight[ServiceError])
        )
    }

    new UserAuthedRoute(ep, logic, tag)
  }

  private val delete = {
    val ep = endpoint.delete
      .in(path[UUID]("id"))
      .out(statusCode(StatusCode.Accepted))

    val logic: AuthLogic[F, User, UUID, Unit] = implicit user =>
      id => EitherT(servicesService.delete(id).void.map(_.asRight[ServiceError]))

    new UserAuthedRoute(ep, logic, tag)
  }

  private val add = {
    val ep = endpoint.post
      .in(jsonBody[InputService])
      .out(jsonBody[Service])

    val logic: AuthLogic[F, User, InputService, Service] = implicit user =>
      input => EitherT(servicesService.add(input.asService).map(_.asRight[ServiceError]))

    new UserAuthedRoute(ep, logic, tag)
  }

  private val updateApiKey = {
    val ep = endpoint.patch
      .in(path[UUID]("id"))
      .in("key")
      .in("refresh")
      .out(jsonBody[Service])

    val logic: AuthLogic[F, User, UUID, Service] = implicit user =>
      id => EitherT(servicesService.updateApiKey(id))

    new UserAuthedRoute(ep, logic, tag)
  }

  val routes: Routes[F] = get ~> search ~> add ~> delete ~> updateApiKey

}
