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
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody

class TablesController[F[_]: Async: Monad](tablesService: TablesService[F])(implicit as: AuthService[F]) {

  private val tag = RouteTag.Tables

  private val getTable = {
    val ep = endpoint
      .get
      .in(path[String]("id"))
      .out(jsonBody[Option[Table]])

    val logic: AuthLogic[F, User, String, Option[Table]] = _ => id =>  EitherT(tablesService.getTable(id).map(_.asRight[TableError]))

    new UserAuthRoute(ep, logic, tag)
  }

  val routes: Routes[F] = getTable

}
