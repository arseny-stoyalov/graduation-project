package gp.utils.kafka

import cats.effect.IO
import fs2.Stream

class KafkaSyntax {
  implicit def kafkaRecordObservableSyntax[Out](stream: Stream[IO, Either[KafkaRecord, Out]]): KafkaRecordsStream[Out] =
    new KafkaRecordsStream[Out](stream)

  implicit def kafkaObservableSyntax[T](stream: Stream[IO, Seq[(KafkaRecord, T)]]): KafkaStream[T] =
    new KafkaStream[T](stream)
}
