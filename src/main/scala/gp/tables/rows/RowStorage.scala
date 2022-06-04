package gp.tables.rows

import cats.Monad
import cats.data.OptionT
import cats.syntax.option._
import cats.syntax.traverse._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.effect.kernel.Async
import doobie.Update
import doobie.implicits._
import doobie.postgres.circe.json.implicits._
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import gp.tables.instances.InstanceHandler
import gp.tables.rows.model.Row
import gp.tables.rows.model.formats.storage.StorageRow
import gp.utils.formats.postgres._
import tofu.logging.LoggingCompanion
import tofu.syntax.logging._

import java.util.UUID

trait RowStorage[F[_]] {
  def get(id: UUID, tableId: UUID): F[Option[Row]]
  def search(size: Option[Int], offset: Option[Int], tableId: UUID): F[List[Row]]
  def insert(row: Row, tableId: UUID): F[Int]
  def delete(id: UUID, tableId: UUID): F[Int]
}

object RowStorage extends LoggingCompanion[RowStorage] {

  class Postgres[F[_]: Async: Log](implicit transactor: Transactor[F], F: Monad[F]) extends RowStorage[F] {

    override def get(id: UUID, tableId: UUID): F[Option[Row]] =
      (fr"select * from" ++ tableNameFragment(tableId) ++ fr"where id = $id::uuid")
        .query[StorageRow]
        .option
        .transact(transactor)
        .flatMap { srOp =>
          OptionT
            .fromOption[F](srOp.map(s => OptionT(decodeStorageRow(s, tableId))))
            .flatten
            .value
        }

    override def search(size: Option[Int], offset: Option[Int], tableId: UUID): F[List[Row]] =
      (fr"select * from" ++ tableNameFragment(tableId) ++ fr"limit ${size.getOrElse(10)} offset ${offset.getOrElse(0)}")
        .query[StorageRow]
        .stream
        .compile
        .toList
        .transact(transactor)
        .flatMap { srList =>
          srList.traverse(decodeStorageRow(_, tableId)).map(_.flatten)
        }

    override def insert(row: Row, tableId: UUID): F[Int] =
      Update[StorageRow](s"""insert into ${InstanceHandler.instanceName(tableId)} (id, entity, created, createdBy)
           |values (?::uuid, ?, ?, ?::uuid)""".stripMargin)
        .toUpdate0(StorageRow.fromRow(row))
        .run
        .transact(transactor)

    override def delete(id: UUID, tableId: UUID): F[Int] =
      Update[UUID](s"delete from ${InstanceHandler.instanceName(tableId)} where id = ?::uuid")
        .toUpdate0(id)
        .run
        .transact(transactor)

    private def tableNameFragment(tableId: UUID) = Fragment.const(InstanceHandler.instanceName(tableId))

    private def decodeStorageRow(s: StorageRow, tableId: UUID): F[Option[Row]] =
      s.asRow match {
        case Right(t) => F.pure(t.some)
        case Left(_) =>
          error"Row ${s.id.toString} of table ${tableId.toString} contains unprocessable column values".as(none[Row])
      }
  }

}
