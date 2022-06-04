package gp.entrypoints.tables

import cats.effect.Async
import cats.syntax.semigroupk._
import gp.auth.AuthService
import gp.tables.rows.{RowController, RowService}
import gp.tables.{TablesController, TablesService}
import gp.utils.routing.dsl.Routes
import org.http4s.HttpRoutes
import sttp.tapir.swagger.http4s.SwaggerHttp4s

class TableNodeController[F[_]: Async](ts: TablesService[F], rs: RowService[F])(implicit val as: AuthService[F]) {

  private val tableRoutes = new TablesController(ts).routes
  private val rowRoutes = new RowController[F](rs).routes

  private val r: Routes[F] = tableRoutes ~> rowRoutes

  private val doc = new SwaggerHttp4s(r.doc).routes[F]

  val routes: HttpRoutes[F] =  doc <+> r.http4s

}
