package gp.tables.rows

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.either._
import cats.syntax.functor._
import gp.auth.UserAuthService
import RowController.{RowId, TableId}
import gp.services.ServicesService
import gp.services.model.Service
import gp.tables.rows.errors.RowError
import gp.tables.rows.model.Row
import gp.tables.rows.model.formats.external.OutputRow
import gp.users.model.User
import gp.utils.routing.dsl.{AuthLogic, Routes, ServiceAuthedRoute, UserAuthedRoute}
import gp.utils.routing.tags.RouteTag
import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

import java.util.UUID

class RowController[F[_]: Async](rs: RowService[F])(implicit as: UserAuthService[F], ss: ServicesService[F]) {

  private val tag = RouteTag.Instances

  private val get = {

    val ep = endpoint.get
      .in(path[TableId]("tableId"))
      .in(path[RowId]("id"))
      .out(jsonBody[OutputRow])

    val logic: AuthLogic[F, User, (TableId, RowId), OutputRow] = _ => { case (tableId, id) =>
      EitherT(rs.get(id, tableId).map(_.map(OutputRow.fromRow).toRight(RowError.NotFound: RowError)))
    }

    new UserAuthedRoute(ep, logic, tag)
  }

  private val search = {
    type Size = Option[Int]
    type Offset = Option[Int]

    val ep = endpoint.get
      .in(path[TableId]("tableId"))
      .in(query[Size]("size"))
      .in(query[Offset]("offset"))
      .out(jsonBody[List[OutputRow]])

    val logic: AuthLogic[F, User, (TableId, Size, Offset), List[OutputRow]] = _ => { case (tableId, size, offset) =>
      EitherT(
        rs
          .search(size, offset, tableId)
          .map(_.map(OutputRow.fromRow).asRight[RowError])
      )
    }

    new UserAuthedRoute(ep, logic, tag)
  }

  private val delete = {

    val ep = endpoint.delete
      .in(path[TableId]("tableId"))
      .in(path[RowId]("id"))
      .out(statusCode(StatusCode.Accepted))

    val logic: AuthLogic[F, User, (TableId, RowId), Unit] = _ => { case (tableId, id) =>
      EitherT(rs.delete(id, tableId).void.map(_.asRight[RowError]))
    }

    new UserAuthedRoute(ep, logic, tag)
  }

  private val add = {
    val ep = endpoint.post
      .in(path[TableId]("tableId"))
      .in(jsonBody[Map[String, Json]])
      .out(statusCode(StatusCode.Accepted))

    val logic: AuthLogic[F, Service, (TableId, Map[String, Json]), Unit] = implicit service => { case (tableId, row) =>
      EitherT(rs.put(row, tableId).void.map(_.asRight[RowError]))
    }

    new ServiceAuthedRoute(ep, logic, tag)
  }

  val routes: Routes[F] = get ~> search ~> delete ~> add

}

object RowController {

  type TableId = UUID
  type RowId = UUID

}
