package gp.tables

import cats.Monad
import cats.data.EitherT
import cats.effect.kernel.Async
import cats.syntax.either._
import cats.syntax.functor._
import gp.auth.AuthService
import gp.tables.errors.TableError
import gp.tables.model.Table
import gp.users.model.User
import gp.utils.routing.dsl.{AuthLogic, Routes, UserAuthRoute}
import gp.utils.routing.tags.RouteTag
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody

class TablesController[F[_]: Async: Monad](tablesService: TablesService[F])(implicit as: AuthService[F]) {

  private val tag = RouteTag.Tables

  private val get = {
    val ep = endpoint.get
      .in(path[String]("id"))
      .out(jsonBody[Table])

    val logic: AuthLogic[F, User, String, Table] = _ =>
      id => EitherT(tablesService.get(id).map(_.toRight(TableError.NotFound)))

    new UserAuthRoute(ep, logic, tag)
  }

  private val search = {
    type Size = Option[Int]
    type Offset = Option[Int]

    val ep = endpoint.get
      .in(query[Size]("size"))
      .in(query[Offset]("offset"))
      .out(jsonBody[List[Table]])

    val logic: AuthLogic[F, User, (Size, Offset), List[Table]] = _ => {
      case (size, offset) =>
        EitherT(
          tablesService
            .search(size, offset)
            .map(_.asRight[TableError])
        )
    }

    new UserAuthRoute(ep, logic, tag)
  }

  private val delete = {
    val ep = endpoint.delete
      .in(path[String]("id"))
      .out(statusCode(StatusCode.Accepted))

    val logic: AuthLogic[F, User, String, Unit] = _ =>
      id => EitherT(tablesService.delete(id).void.map(_.asRight[TableError]))

    new UserAuthRoute(ep, logic, tag)
  }

  private val add = {
    val ep = endpoint.post
      .in(jsonBody[Table])
      .out(statusCode(StatusCode.Accepted))

    val logic: AuthLogic[F, User, Table, Unit] = _ =>
      table => EitherT(tablesService.add(table).void.map(_.asRight[TableError]))

    new UserAuthRoute(ep, logic, tag)
  }

  val routes: Routes[F] = get ~> search ~> add ~> delete

}
