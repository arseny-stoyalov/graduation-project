package gp.tables

import cats.Monad
import cats.syntax.flatMap._
import cats.effect.kernel.Async
import doobie.implicits._
import doobie.postgres.circe.json.implicits._
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import gp.utils.formats.postgres._
import gp.tables.model.Table
import gp.tables.model.formats.storage.StorageTable
import tofu.logging.LoggingCompanion
import tofu.syntax.logging._

import java.util.UUID

trait TablesStorage[F[_]] {

  def init(): F[Unit]
  def get(id: UUID): F[Option[Table]]
  def search(size: Option[Int], offset: Option[Int]): F[List[Table]]

}

object TablesStorage extends LoggingCompanion[TablesStorage] {

  lazy val tableName: String = "tables"

  class Postgres[F[_]: Async: Monad: Log](implicit val transactor: Transactor[F]) extends TablesStorage[F] {
    lazy val tableNameFragment: Fragment = Fragment.const(tableName)

    override def init(): F[Unit] =
      (fr"create table" ++ tableNameFragment ++
        fr"""(
             id uuid primary key,
             name text,
             columns json not null,
             created timestamp,
             createdBy uuid
             )""").update.run.attemptSql
        .transact(transactor)
        .flatMap {
          case Left(_) => info"Tables relation not created"
          case Right(_) => info"Tables relation created"
        }

    override def get(id: UUID): F[Option[Table]] =
      (fr"select * from" ++ tableNameFragment ++ fr"where id = $id::uuid")
        .query[StorageTable]
        .option
        .map(_.flatMap(_.asTable.toOption)) //todo error log
        .transact(transactor)

    override def search(size: Option[Int], offset: Option[Int]): F[List[Table]] =
      (fr"select * from" ++ tableNameFragment ++ fr"limit ${size.getOrElse(10)} offset ${offset.getOrElse(0)}")
        .query[StorageTable]
        .stream
        .compile
        .toList
        .map(_.flatMap(_.asTable.toOption)) //todo error log
        .transact(transactor)
  }

}
