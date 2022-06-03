package gp.entrypoints.tables

import cats.effect.Async
import cats.syntax.semigroupk._
import gp.auth.AuthService
import gp.tables.{TablesController, TablesService}
import gp.utils.routing.dsl.Routes
import org.http4s.HttpRoutes
import sttp.tapir.swagger.http4s.SwaggerHttp4s

class TableNodeController[F[_]: Async](ts: TablesService[F])(implicit val as: AuthService[F]) {

  private val tableRoutes = new TablesController(ts).routes

  private val r: Routes[F] = tableRoutes

  private val doc = new SwaggerHttp4s(r.doc, List("tables", "docs")).routes[F]

  val routes: HttpRoutes[F] =  doc <+> r.http4s

}
