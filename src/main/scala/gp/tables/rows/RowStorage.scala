package gp.tables.rows

import cats.Monad
import cats.effect.kernel.Async
import doobie.Update
import doobie.implicits._
import doobie.postgres.circe.json.implicits._
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import gp.tables.instances.InstanceHandler
import gp.tables.rows.model.Row
import gp.utils.formats.postgres._

import java.util.UUID

trait RowStorage[F[_]] {
  def get(id: UUID, tableId: UUID): F[Option[Row]]
  def search(size: Option[Int], offset: Option[Int], tableId: UUID): F[List[Row]]
  def insert(row: Row, tableId: UUID): F[Int]
  def delete(id: UUID, tableId: UUID): F[Int]
}

object RowStorage {

  class Postgres[F[_]: Async](implicit transactor: Transactor[F], F: Monad[F]) extends RowStorage[F] {

    override def get(id: UUID, tableId: UUID): F[Option[Row]] =
      (fr"select * from" ++ tableNameFragment(tableId) ++ fr"where id = $id::uuid")
        .query[Row]
        .option
        .transact(transactor)

    override def search(size: Option[Int], offset: Option[Int], tableId: UUID): F[List[Row]] =
      (fr"select * from" ++ tableNameFragment(tableId) ++ fr"limit ${size.getOrElse(10)} offset ${offset.getOrElse(0)}")
        .query[Row]
        .stream
        .compile
        .toList
        .transact(transactor)

    override def insert(row: Row, tableId: UUID): F[Int] =
      Update[Row](s"""insert into ${InstanceHandler.instanceName(tableId)} (id, entity, created, createdBy)
           |values (?::uuid, ?, ?, ?::uuid)""".stripMargin)
        .toUpdate0(row)
        .run
        .transact(transactor)

    override def delete(id: UUID, tableId: UUID): F[Int] =
      Update[UUID](s"delete from ${InstanceHandler.instanceName(tableId)} where id = ?::uuid")
        .toUpdate0(id)
        .run
        .transact(transactor)

    private def tableNameFragment(tableId: UUID) = Fragment.const(InstanceHandler.instanceName(tableId))
  }

}
