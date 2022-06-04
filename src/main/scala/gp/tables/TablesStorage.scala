package gp.tables

import cats.Monad
import cats.data.OptionT
import cats.effect.kernel.Async
import cats.syntax.flatMap._
import cats.syntax.traverse._
import cats.syntax.functor._
import cats.syntax.option._
import doobie.implicits._
import doobie.postgres.circe.json.implicits._
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import gp.tables.model.Table
import gp.tables.model.formats.storage.StorageTable
import gp.utils.formats.postgres._
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

  class Postgres[F[_]: Async: Log](implicit transactor: Transactor[F], F: Monad[F]) extends TablesStorage[F] {
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
        .transact(transactor)
        .flatMap { stOp =>
          OptionT
            .fromOption[F](stOp.map(s => OptionT(decodeStorageTable(s))))
            .flatten
            .value
        }

    override def search(size: Option[Int], offset: Option[Int]): F[List[Table]] =
      (fr"select * from" ++ tableNameFragment ++ fr"limit ${size.getOrElse(10)} offset ${offset.getOrElse(0)}")
        .query[StorageTable]
        .stream
        .compile
        .toList
        .transact(transactor)
        .flatMap { stList =>
          stList.traverse(decodeStorageTable).map(_.flatten)
        }

    private def decodeStorageTable(s: StorageTable): F[Option[Table]] =
      s.asTable match {
        case Right(t) => F.pure(t.some)
        case Left(_) =>
          error"Table ${s.id.toString} contains unprocessable column descriptions".as(none[Table])
      }
  }

}
