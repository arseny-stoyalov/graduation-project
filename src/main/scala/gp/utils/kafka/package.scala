package gp.utils

import cats.effect.{IO, Resource}
import fs2.kafka.CommittableConsumerRecord
import org.apache.kafka.common.TopicPartition

package object kafka {

  case class KafkaTopic(name: String, bootstrapServers: List[String]) {
    def create: IO[Unit] = {
      val cli =
        Resource.make[IO, KafkaAdminClient](IO.blocking(new KafkaAdminClient(bootstrapServers)))(c =>
          IO.blocking(c.close)
        )
      cli.use(c =>
        c.topicsExist(name).flatMap { existing =>
          if (!existing.contains(name)) {
            c.createTopic(name, 1, 1.toShort)
          } else IO.unit
        }
      )
    }
  }

  object implicits extends KafkaSyntax

  type KafkaRecord = CommittableConsumerRecord[IO, Any, Array[Byte]]
  private[kafka] def topicAndPartition(kafkaMessage: KafkaRecord): TopicPartition =
    kafkaMessage.offset.topicPartition
}
