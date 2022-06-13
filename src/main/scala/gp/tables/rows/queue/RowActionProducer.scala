package gp.tables.rows.queue

import cats.effect.IO
import fs2.kafka._
import gp.services.model.Service
import gp.tables.rows.Action
import gp.tables.rows.model.Row
import gp.utils.kafka.KafkaTopic
import io.circe.Json

import java.util.{Date, UUID}

trait RowActionProducer[F[_]] {
  def put(row: Map[String, Json], tableId: UUID)(implicit service: Service): F[Unit] = {
    val action = Action.Write(tableId, row, service, new Date().getTime).toBytes
    send(action)
  }

  def delete(id: UUID, tableId: UUID): F[Unit] = {
    val action = Action.Delete(tableId, id).toBytes
    send(action)
  }

  protected def send(a: Array[Byte]): F[Unit]
}

object RowActionProducer {
  class Kafka(topic: KafkaTopic) extends RowActionProducer[IO] {

    override def send(a: Array[Byte]): IO[Unit] =
      KafkaProducer
        .resource(producerSettings)
        .use { producer =>
          producer
            .produce(
              ProducerRecords.one(ProducerRecord(topic.name, (), a))
            )
            .flatten
        }
        .void

    private val producerSettings = ProducerSettings[IO, Unit, Array[Byte]]
      .withBootstrapServers(topic.bootstrapServers.mkString(","))
      .withRetries(3)
      .withAcks(Acks.One)

  }

}
