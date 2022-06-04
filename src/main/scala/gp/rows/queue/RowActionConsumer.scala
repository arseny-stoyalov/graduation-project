package gp.rows.queue

import cats.effect.IO
import cats.syntax.either._
import fs2.Stream
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, KafkaConsumer}
import gp.rows.{Action, RowService}
import gp.rows.errors.InstanceError
import gp.rows.model.Row
import gp.tables.TablesService
import gp.utils.catseffect._
import gp.utils.kafka.implicits._
import gp.utils.kafka.{KafkaRecord, KafkaTopic}
import io.circe.parser._
import tofu.logging.LoggingCompanion
import tofu.syntax.location.logging._

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

trait RowActionConsumer[F[_]] {
  def start: F[Unit]
}

object RowActionConsumer extends LoggingCompanion[RowActionConsumer] {

  class Kafka(topic: KafkaTopic, tablesService: TablesService[IO], rowService: RowService[IO], ioEC: ExecutionContext)(
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
            case w: Action.Write =>
              logic
                .process(w.tableId, w.row)
                .flatMap { x =>
                  x.fold(
                    err => warn"Row processing finished with ${err.toString}",
                    r => write(w.tableId, r)
                  )
                }
                .as(r.kafkaRecord)

            case d: Action.Delete =>
              tablesService
                .get(d.tableId)
                .flatMap { x =>
                  x.fold(warn"Unable to delete a row, because table was not found")(_ =>
                    rowService.directDelete(d.ids, d.tableId).void
                  )
                }
                .as(r.kafkaRecord)

            case e: Action.Erase =>
              tablesService
                .get(e.tableId)
                .flatMap { x =>
                  x.fold(warn"Unable to delete an unrepresented table")(_ =>
                    rowService.directErase(e.tableId).void
                  ) //todo handle error and delete table record with rows table via one transaction
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
      .withGroupId("single-reader")
      .withMaxPollRecords(100)
      .withMaxPollInterval(1000.millis)
      .withSessionTimeout(6001.millis)
      .withMaxPrefetchBatches(54428800)
      .withAutoOffsetReset(AutoOffsetReset.Latest)
      .withEnableAutoCommit(false)

    private lazy val logic = new Logic[IO](tablesService)

    private def parseRecord(bytes: Array[Byte]): Either[Throwable, Action] =
      parse(new String(bytes, StandardCharsets.UTF_8))
        .flatMap(_.as[Action])
        .leftMap(_.fillInStackTrace())

    //todo row entity type check + sane return type
    private def write(tableId: String, row: Row): IO[Either[InstanceError, Int]] =
      rowService.directPut(row, tableId).map(_.asRight[InstanceError])

  }

  private[rows] case class ConsumerContext(action: Action, kafkaRecord: KafkaRecord)

}
