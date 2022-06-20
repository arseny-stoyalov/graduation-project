package gp.tables.rows.queue

import cats.effect.IO
import cats.syntax.either._
import fs2.Stream
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, KafkaConsumer}
import gp.tables.rows.{Action, RowService}
import gp.tables.rows.errors.InstanceError
import gp.tables.rows.model.Row
import gp.tables.TablesService
import gp.utils.catseffect._
import gp.utils.kafka.implicits._
import gp.utils.kafka.{KafkaRecord, KafkaTopic}
import io.circe.parser._
import tofu.logging.LoggingCompanion
import tofu.syntax.location.logging._

import java.nio.charset.StandardCharsets
import java.util.{Date, UUID}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

trait RowActionConsumer[F[_]] {
  def start: F[Unit]
}

object RowActionConsumer extends LoggingCompanion[RowActionConsumer] {

  class Kafka(topic: KafkaTopic, groupId: Int, tablesService: TablesService[IO], rowService: RowService[IO], ioEC: ExecutionContext)(
    implicit L: RowActionConsumer.Log[IO]
  ) extends RowActionConsumer[IO] {

    override def start: IO[Unit] =
      stream.compile.drain
        .evalOn(ioEC)
        .onErrorRestartWithDelay(1.second, e => errorCause"event consuming failed" (e))
        .start
        .void

    private val stream: Stream[IO, Unit] =
      KafkaConsumer
        .stream(consumerSettings)
        .subscribeTo(topic.name)
        .records
        .map { msg =>
          val kr = msg.asInstanceOf[KafkaRecord]
          parseRecord(msg.record.value)
            .map(action => ConsumerContext(action, kr))
            .leftMap(_ => kr)
        }
        .mapValidAsync(r =>
          r.action match {
            case Action.Write(tableId, untyped, sentBy, produced) =>
              val started = new Date().getTime
              logic
                .process(tableId, untyped)(sentBy)
                .flatMap { x =>
                  x.fold(
                    err => warn"Row processing finished with ${err.toString}",
                    r => processingTimeLogged(produced, started)(write(tableId, r))
                  )
                }
                .as(r.kafkaRecord)

            case Action.Delete(tableId, id) =>
              tablesService
                .get(tableId)
                .flatMap { x =>
                  x.fold(warn"Unable to delete a row, because table was not found")(_ =>
                    rowService.directDelete(id, tableId).void
                  )
                }
                .as(r.kafkaRecord)

          }
        )
        .map(_.fold(identity, identity) -> ())
        .groupWithin(500, 1.millisecond)
        .map(_.toList)
        .batchedCommit(m => debug"$m", m => warn"$m")(ioEC)

    private lazy val consumerSettings = ConsumerSettings[IO, Unit, Array[Byte]]
      .withBootstrapServers(topic.bootstrapServers.mkString(","))
      .withGroupId(s"reader$groupId")
      .withMaxPollRecords(500)
      .withMaxPollInterval(6000.millis)
      .withSessionTimeout(8000.millis)
      .withMaxPrefetchBatches(1000)
      .withAutoOffsetReset(AutoOffsetReset.Latest)
      .withEnableAutoCommit(false)

    private lazy val logic = new Logic[IO](tablesService)

    private def parseRecord(bytes: Array[Byte]): Either[Throwable, Action] =
      parse(new String(bytes, StandardCharsets.UTF_8))
        .flatMap(_.as[Action])
        .leftMap(_.fillInStackTrace())

    private def write(tableId: UUID, row: Row): IO[Unit] =
      rowService.directPut(row, tableId).void

    private def processingTimeLogged(produced: Long, started: Long)(f: IO[Unit]) =
      for {
        res <- f
        _ <- debug"Processing row took ${new Date().getTime - produced} ms total, ${new Date().getTime - started} ms in consumer"
      } yield res

  }

  private[rows] case class ConsumerContext(action: Action, kafkaRecord: KafkaRecord)

}
