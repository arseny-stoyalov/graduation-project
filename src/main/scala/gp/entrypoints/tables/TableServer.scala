package gp.entrypoints.tables

import cats.Id
import cats.effect.{ExitCode, IO, IOApp}
import doobie._
import gp.auth.UserAuthService
import gp.entrypoints.{ioScheduler, logicScheduler}
import gp.services.{ServicesService, ServicesStorage}
import gp.tables.instances.InstanceHandler
import gp.tables.rows.queue.RowActionProducer
import gp.tables.rows.RowStorage
import gp.tables.rows.{RowService, RowStorage}
import gp.tables.rows.queue.{RowActionConsumer, RowActionProducer}
import gp.tables.{TablesService, TablesStorage}
import gp.users.UsersService
import gp.utils.catseffect._
import gp.utils.kafka.KafkaTopic
import org.http4s.blaze.server.BlazeServerBuilder
import tofu.logging.Logging
import tofu.syntax.logging._

private object TableServer extends IOApp {

  val config: TableNodeConfig = TableNodeConfig()

  implicit private val transactor: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    config.postgres.url,
    config.postgres.user,
    config.postgres.password
  )

  //auth
  val userService = new UsersService.InMemory
  implicit val authService: UserAuthService[IO] = new UserAuthService[IO](config.jwt, userService)

  val servicesStorage = new ServicesStorage.Postgres[IO]()
  implicit val servicesService: ServicesService[IO] = new ServicesService[IO](servicesStorage)

  //instances
  val instancesHandler = new InstanceHandler.Postgres[IO]()

  //rows
  val rowActionTopic: KafkaTopic = KafkaTopic("row-actions", List(config.bootstrapServer))

  val rowActionProducer = new RowActionProducer.Kafka(rowActionTopic)
  val rowStorage = new RowStorage.Postgres[IO]()
  val rowService = new RowService[IO](rowStorage, rowActionProducer)

  //tables
  val tablesStorage = new TablesStorage.Postgres[IO]()
  val tablesService = new TablesService[IO](tablesStorage, instancesHandler)

  //rows consumer
  val rowActionConsumer = new RowActionConsumer.Kafka(rowActionTopic, tablesService, rowService, ioScheduler)

  val controller: TableNodeController[IO] = new TableNodeController[IO](tablesService, rowService)

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioL: Id[Logging[IO]] = Logging.Make.plain[IO].byName(getClass.getCanonicalName)

    tablesService.init() >>
      rowActionTopic.create >>
      rowActionConsumer.start >>
      BlazeServerBuilder[IO]
        .withExecutionContext(logicScheduler)
        .bindHttp(config.port, "localhost")
        .withHttpApp(controller.routes.orNotFound)
        .resource
        .use(_ => IO.never)
        .as(ExitCode.Success)
        .handleErrorWith { e =>
          errorCause"failed start role process" (e).as(ExitCode.Error)
        }

  }

}
