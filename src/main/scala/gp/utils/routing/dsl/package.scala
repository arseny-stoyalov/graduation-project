package gp.utils.routing

import cats.MonadError
import cats.data.EitherT
import cats.effect.kernel.Async
import cats.syntax.semigroupk._
import gp.auth.AuthService
import gp.auth.AuthService.JWT
import gp.users.model.User
import gp.utils.routing.dsl.errors.unauthorized
import gp.utils.routing.errors.{ApiError, ApiErrorLike}
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir.EndpointOutput.OneOfVariant
import sttp.tapir._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.{PartialServerEndpoint, ServerEndpoint}
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{Endpoint, EndpointInput, auth, oneOfVariant}
import sttp.client3.impl.cats.implicits.monadError

package object dsl {

  case class Routes[F[_]: Async](inner: List[RouteBase[F]]) {
    final def ~>(other: Routes[F]): Routes[F] = Routes(inner ::: other.inner)

    final def ->:(prefix: String): Routes[F] = Routes(inner.map(_.prependPath(prefix)))

    final def doc: String =
      OpenAPIDocsInterpreter().toOpenAPI(endpoints.map(_.endpoint), "grad-project", "1.0.0").toYaml

    final lazy val endpoints: List[ServerEndpoint[Any, F]] = inner.map(_.e)

    final lazy val http4s: HttpRoutes[F] =
      endpoints
        .map(ep => Http4sServerInterpreter[F]().toRoutes(ep))
        .foldLeft(HttpRoutes.empty[F])((a, b) => a <+> b)
  }

  implicit def routeToRoutes[F[_]: Async](route: RouteBase[F]): Routes[F] = Routes(List(route))

  abstract class RouteBase[F[_]: Async] {
    private[dsl] type RouteType <: RouteBase[F]
    private[dsl] def e: ServerEndpoint[Any, F]

    def prependPath(path: String): RouteType
  }

  class Route[F[_]: Async, I, O](
    ep: Endpoint[Unit, I, ApiError, O, Any],
    logic: Logic[F, I, O],
    override val rc: RouteClass
  ) extends RouteBase
      with HasRouteClass {

    override type RouteType = Route[F, I, O]

    override def e: ServerEndpoint[Any, F] =
      ep.prependIn(rc.input)
        .tags(rc.tags)
        .serverLogic(i => logic(i).leftMap(_.asApiError).value)

    override def prependPath(path: String): Route[F, I, O] = new Route[F, I, O](ep.prependIn(path), logic, rc)
  }

  class UserAuthRoute[F[_], I, O](
    ep: Endpoint[Unit, I, Unit, O, Any],
    logic: AuthLogic[F, User, I, O],
    override val rc: RouteClass
  )(implicit protected val as: AuthService[F], F: Async[F] with MonadError[F, Throwable])
      extends RouteBase
      with HasRouteClass {

    override type RouteType = UserAuthRoute[F, I, O]

    override def e: ServerEndpoint[Any, F] =
      ep
        .securityIn(authedEndpoint.securityInput)
        .prependErrorOut(authedEndpoint.errorOutput)
        .serverSecurityLogic(authedEndpoint.securityLogic(monadError(F)))
        .prependIn(rc.input)
        .tags(rc.tags)
        .serverLogic(token => i => logic(token)(i).leftMap(_.asApiError).value)

    override def prependPath(path: String): UserAuthRoute[F, I, O] =
      new UserAuthRoute[F, I, O](ep.prependIn(path), logic, rc)

    private lazy val authedEndpoint: PartialServerEndpoint[JWT, User, Unit, ApiError, Unit, Any, F] =
      endpoint
        .securityIn(auth.apiKey(header[JWT]("token")))
        .errorOut(oneOf[ApiError](unauthorized))
        .serverSecurityLogic[User, F](token => as.getUser(token).leftMap(_.asApiError).value)

  }

  trait RouteClass {
    def input: EndpointInput[Unit]

    def tags: List[String]
  }

  object RouteClass {
    case object Auth extends RouteClass {
      override val input: EndpointInput[Unit] = "auth"
      override val tags: List[String] = List("auth")
    }
  }

  trait HasRouteClass {
    protected def rc: RouteClass
  }

  object errors {
    val unauthorized: OneOfVariant[ApiError.Unauthorized] =
      oneOfVariant(StatusCode.Unauthorized, jsonBody[ApiError.Unauthorized])

    val notFound: OneOfVariant[ApiError.NotFound] =
      oneOfVariant(StatusCode.NotFound, jsonBody[ApiError.NotFound])

    val unprocessableEntity: OneOfVariant[ApiError.UnprocessableEntity] =
      oneOfVariant(StatusCode.UnprocessableEntity, jsonBody[ApiError.UnprocessableEntity])
  }

  type Logic[F[_], I, O] = I => EitherT[F, _ <: ApiErrorLike, O]
  type AuthLogic[F[_], A, I, O] = A => I => EitherT[F, _ <: ApiErrorLike, O]
}