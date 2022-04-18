package gp

import doobie._
import doobie.implicits._
import cats.effect.IO

import cats.effect.unsafe.implicits.global

object Main {

  def main(args: Array[String]): Unit = {

    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", // driver classname
      "jdbc:postgresql://localhost:5432/admin", // connect URL (driver-specific)
      "admin", // user
      "123" // password
    )

    val simpleSelect = sql"select 42".query[Int].unique.transact(xa)

    simpleSelect
      .map(num => println(num))
      .unsafeRunSync()
  }
}
