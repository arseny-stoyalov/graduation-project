package gp.rows.queue

import cats.effect.IO
import fs2.kafka._
import gp.rows.Action
import gp.rows.model.Row
import gp.utils.kafka.KafkaTopic

trait RowActionProducer[F[_]] {
  def put(tableId: String, row: Row): F[Unit] = {
    val action = Action.Write(tableId, row).toBytes
    send(action)
  }

  def delete(tableId: String, ids: List[String]): F[Unit] = {
    val action = Action.Delete(tableId, ids).toBytes
    send(action)
  }

  def erase(tableId: String): F[Unit] = {
    val action = Action.Erase(tableId).toBytes
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
