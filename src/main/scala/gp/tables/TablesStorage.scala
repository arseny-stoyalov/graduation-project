package gp.tables

import cats.Monad
import cats.syntax.flatMap._
import cats.effect.kernel.Async
import doobie.Update
import doobie.implicits._
import doobie.postgres.circe.json.implicits._
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import gp.tables.model.Table
import gp.tables.model.formats.StorageTable
import tofu.logging.LoggingCompanion
import tofu.syntax.logging._

trait TablesStorage[F[_]] {

  def create(): F[Unit]
  def get(id: String): F[Option[Table]]
  def insert(table: Table): F[Int]
  def delete(ids: List[String]): F[Int]

}

object TablesStorage extends LoggingCompanion[TablesStorage] {

  class Postgres[F[_]: Async: Monad: Log](implicit val transactor: Transactor[F]) extends TablesStorage[F] {
    lazy val tableName: String = "tables"
    lazy val tableNameFragment: Fragment = Fragment.const(tableName)

    override def create(): F[Unit] =
      (fr"create table" ++ tableNameFragment ++
        fr"""(
             id varchar(20),
             columns json not null,
             createdBy varchar(20) not null
             )""").update.run.attemptSql
        .transact(transactor)
        .flatMap {
          case Left(_) => info"Tables relation not created"
          case Right(_) => info"Tables relation created"
        }

    override def get(id: String): F[Option[Table]] =
      (fr"select * from" ++ tableNameFragment ++ fr"where id = $id")
        .query[StorageTable]
        .option
        .map(_.flatMap(_.asTable.toOption)) //todo error log
        .transact(transactor)

    override def insert(table: Table): F[Int] =
      Update[StorageTable](s"insert into $tableName (id, columns, createdBy) values (?, ?, ?)")
        .toUpdate0(StorageTable.fromTable(table))
        .run
        .transact(transactor)

    override def delete(ids: List[String]): F[Int] =
      Update[String](s"delete from $tableName where id = ?")
        .updateMany(ids)
        .transact(transactor)
  }

}
