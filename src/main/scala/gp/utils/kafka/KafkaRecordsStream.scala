package gp.utils.kafka

import cats.effect.IO
import cats.syntax.either._
import fs2.Stream

class KafkaRecordsStream[T](val obs: Stream[IO, Either[KafkaRecord, T]]) extends AnyVal {
  def mapValid[Out](f: T => Out): Stream[IO, Either[KafkaRecord, Out]] =
    obs.map(_.map(f))

  def mapValidAsync[Out](f: T => IO[Out]): Stream[IO, Either[KafkaRecord, Out]] =
    obs.evalMap {
      _.fold(
        kr => IO.pure(kr.asLeft),
        r => f(r).map(_.asRight)
      )
    }
}
