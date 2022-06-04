package gp.rows

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.either._
import cats.syntax.functor._
import gp.auth.AuthService
import gp.rows.RowController.{RowId, TableId}
import gp.rows.errors.RowError
import gp.rows.model.Row
import gp.users.model.User
import gp.utils.routing.dsl.{AuthLogic, Routes, UserAuthRoute}
import gp.utils.routing.tags.RouteTag
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

class RowController[F[_]: Async](rs: RowService[F])(implicit val as: AuthService[F]) {

  private val tag = RouteTag.Instances

  private val get = {

    val ep = endpoint.get
      .in(path[TableId]("tableId"))
      .in(path[RowId]("id"))
      .out(jsonBody[Row])

    val logic: AuthLogic[F, User, (TableId, RowId), Row] = _ => { case (tableId, id) =>
      EitherT(rs.get(id, tableId).map(_.toRight(RowError.NotFound)))
    }

    new UserAuthRoute(ep, logic, tag)
  }

  private val search = {
    type Size = Option[Int]
    type Offset = Option[Int]

    val ep = endpoint.get
      .in(path[TableId]("tableId"))
      .in(query[Size]("size"))
      .in(query[Offset]("offset"))
      .out(jsonBody[List[Row]])

    val logic: AuthLogic[F, User, (TableId, Size, Offset), List[Row]] = _ => { case (tableId, size, offset) =>
      EitherT(
        rs
          .search(size, offset, tableId)
          .map(_.asRight[RowError])
      )
    }

    new UserAuthRoute(ep, logic, tag)
  }

  private val delete = {

    val ep = endpoint.delete
      .in(path[TableId]("tableId"))
      .in(path[RowId]("id"))
      .out(statusCode(StatusCode.Accepted))

    val logic: AuthLogic[F, User, (TableId, RowId), Unit] = _ => { case (tableId, id) =>
      EitherT(rs.delete(List(id), tableId).void.map(_.asRight[RowError]))
    }

    new UserAuthRoute(ep, logic, tag)
  }

  private val add = {
    val ep = endpoint.post
      .in(path[TableId]("tableId"))
      .in(jsonBody[Row])
      .out(statusCode(StatusCode.Accepted))

    val logic: AuthLogic[F, User, (TableId, Row), Unit] = _ => { case (tableId, row) =>
      EitherT(rs.put(row, tableId).void.map(_.asRight[RowError]))
    }

    new UserAuthRoute(ep, logic, tag)
  }

  val routes: Routes[F] = get ~> search ~> delete ~> add

}

object RowController {

  type TableId = String
  type RowId = String

}
