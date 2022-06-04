package gp.tables.instances

import cats.effect.kernel.Async
import cats.syntax.flatMap._
import cats.syntax.functor._
import doobie.Update
import doobie.implicits._
import doobie.postgres.circe.json.implicits._
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import gp.tables.TablesStorage
import gp.tables.model.Table
import gp.tables.model.formats.storage.StorageTable
import gp.utils.formats.postgres._

import java.util.UUID

/** Make creation of table record and instance table in one transaction
  */
trait InstanceHandler[F[_]] {

  def create(table: Table): F[Unit]
  def delete(id: UUID): F[Unit]

}

object InstanceHandler {

  def instanceName(tableId: UUID) = s"instance_${tableId.toString.replaceAll("-", "_")}"

  class Postgres[F[_]: Async](implicit transactor: Transactor[F]) extends InstanceHandler[F] {

    override def create(table: Table): F[Unit] =
      (insertTableRecord(table).void >> createInstance(table.id).void)
        .transact(transactor)

    override def delete(id: UUID): F[Unit] =
      (deleteTableRecord(id).void >> dropInstance(id).void)
        .transact(transactor)

    private def createInstance(tableId: UUID) =
      (fr"create table" ++ Fragment.const(instanceName(tableId)) ++
        fr"""(
             id uuid primary key,
             entity json,
             created timestamp,
             createdBy uuid
             )""").update.run

    private def dropInstance(tableId: UUID) =
      (fr"drop table" ++ Fragment.const(instanceName(tableId))).update.run

    private def insertTableRecord(table: Table) = {
      Update[StorageTable](s"""insert into ${TablesStorage.tableName} (id, name, columns, created, createdBy)
      |values (?::uuid, ?, ?, ?, ?::uuid)""".stripMargin)
        .toUpdate0(StorageTable.fromTable(table))
        .run
    }

    private def deleteTableRecord(id: UUID) =
      Update[UUID](s"delete from ${TablesStorage.tableName} where id = ?::uuid")
        .toUpdate0(id)
        .run
  }

}
