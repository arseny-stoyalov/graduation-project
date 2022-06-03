package gp.utils.kafka

import cats.effect.IO
import fs2.Stream
import fs2.kafka.CommittableOffsetBatch
import org.apache.kafka.clients.consumer.CommitFailedException

import scala.concurrent.ExecutionContext

class KafkaStream[T](val stream: Stream[IO, Seq[(KafkaRecord, T)]]) extends AnyVal {
  def batchedCommit(debug: String => IO[Unit], warn: String => IO[Unit])(implicit
    ioEC: ExecutionContext
  ): Stream[IO, T] =
    stream
      .map(createOffsetBatches)
      .flatMap { batches =>
        Stream.emits[IO, (CommittableOffsetBatch[IO], Seq[T])](batches).evalMap { case (batch, chunk) =>
          val s = System.currentTimeMillis()
          batch.commit
            .evalOn(ioEC)
            .flatMap { _ =>
              debug(s"commit [${batch.offsets}] in ${System.currentTimeMillis() - s}ms").as(chunk)
            }
            .handleErrorWith { case _: CommitFailedException =>
              warn("commit cannot be completed since the group has already rebalanced").as(chunk)
            }
        }
      }
      .flatMap(batch => Stream.apply(batch: _*))

  private def createOffsetBatches(chunk: Seq[(KafkaRecord, T)]): Seq[(CommittableOffsetBatch[IO], Seq[T])] =
    chunk
      .groupBy { case (kafkaRecord, _) => topicAndPartition(kafkaRecord) }
      .values
      .toList
      .map { topicMessages =>
        CommittableOffsetBatch.fromFoldable(topicMessages.map(_._1.offset)) -> topicMessages.map(_._2)
      }
}
