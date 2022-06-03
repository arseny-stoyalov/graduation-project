package gp.utils.kafka

import gp.utils.catseffect.ioLogging
import cats.Id
import cats.effect.IO
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig, NewTopic}
import tofu.logging.Logging
import tofu.syntax.logging._

import java.util.Properties
import scala.jdk.CollectionConverters._

class KafkaAdminClient(bootstrapServers: List[String]) {
  private lazy val admin = {
    val props = new Properties()
    props.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.mkString(","))

    AdminClient.create(props)
  }

  def createTopic(name: String, partitions: Int, replicas: Short): IO[Unit] =
    createTopics(partitions, replicas)(name)

  def createTopics(partitions: Int, replicas: Short)(names: String*): IO[Unit] = {
    implicit val logsContext: Id[Logging[IO]] = ioLogging.byName(getClass.getCanonicalName)
    IO.blocking(
      admin.createTopics(names.map(new NewTopic(_, partitions, replicas)).asJava).all().get()
    ) >> info"topics (${names.size}) created: ${names.take(5).mkString(", ")} with partitions [$partitions] replicas: [$replicas]"
  }

  def topicsExist(topics: String*): IO[List[String]] =
    IO.blocking(admin.listTopics().names().get())
      .map(_.asScala)
      .map(existing => topics.filter(existing.contains))
      .map(_.toList)

  def close = admin.close()
}
