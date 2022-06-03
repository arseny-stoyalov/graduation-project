package gp.rows

import cats.syntax.functor._
import cats.syntax.either._
import cats.effect.kernel.Async
import doobie.Update
import doobie.postgres.circe.json.implicits._
import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import gp.rows.errors.InstanceError
import gp.rows.model.Row

trait RowStorage[F[_]] {
  def create(tableId: String): F[Either[InstanceError, Int]]
  def drop(tableId: String): F[Either[InstanceError, Int]]
  def get(id: String, tableId: String): F[Option[Row]]
  def insert(row: Row, tableId: String): F[Int]
  def delete(ids: List[String], tableId: String): F[Int]
}

object RowStorage {

  class Postgres[F[_]: Async](implicit transactor: Transactor[F]) extends RowStorage[F] {

    override def create(tableId: String): F[Either[InstanceError, Int]] =
      (fr"create table" ++ tableNameFragment(tableId) ++
        fr"""(
             id varchar(20),
             entity json not null,
             created date not null
             )""").update.run.attemptSql
        .transact(transactor)
        .map(_.leftMap(ex => InstanceError.CreateFail(ex.getMessage): InstanceError))

    override def drop(tableId: String): F[Either[InstanceError, Int]] =
      (fr"drop table" ++ tableNameFragment(tableId)).update.run.attemptSql
        .transact(transactor)
        .map(_.leftMap(ex => InstanceError.DropFail(ex.getMessage): InstanceError))

    override def get(id: String, tableId: String): F[Option[Row]] =
      (fr"select * from" ++ tableNameFragment(tableId) ++ fr"where id = $id")
        .query[Row]
        .option
        .transact(transactor)

    override def insert(row: Row, tableId: String): F[Int] =
      Update[Row](s"insert into ${tableName(tableId)} (id, entity, created) values (?, ?, ?)")
        .toUpdate0(row)
        .run
        .transact(transactor)

    override def delete(ids: List[String], tableId: String): F[Int] =
      Update[String](s"delete from ${tableName(tableId)} where id = ?")
        .updateMany(ids)
        .transact(transactor)

    private def tableName(tableId: String) = s"instance_$tableId"
    private def tableNameFragment(tableId: String) = Fragment.const(tableName(tableId))
  }

}
