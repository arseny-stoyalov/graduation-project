package gp.services

import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.Monad
import cats.effect.kernel.Async
import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import doobie.util.update.Update
import gp.services.model.Service
import gp.utils.formats.postgres._
import tofu.logging.LoggingCompanion
import tofu.syntax.logging._

import java.util.UUID

trait ServicesStorage[F[_]] {
  def init(): F[Unit]
  def get(id: UUID, userId: UUID): F[Option[Service]]
  def getByApiKey(apiKey: String): F[Option[Service]]
  def search(size: Option[Int], offset: Option[Int], userId: UUID): F[List[Service]]
  def insert(service: Service): F[Unit]
  def update(service: Service): F[Unit]
  def delete(id: UUID, userId: UUID): F[Unit]
}

object ServicesStorage extends LoggingCompanion[ServicesStorage] {

  lazy val tableName: String = "services"

  class Postgres[F[_]: Async: Log](implicit transactor: Transactor[F], F: Monad[F]) extends ServicesStorage[F] {
    lazy val tableNameFragment: Fragment = Fragment.const(tableName)

    override def init(): F[Unit] =
      (fr"create table" ++ tableNameFragment ++
        fr"""(
             id uuid primary key,
             name text,
             apiKey text unique,
             created timestamp,
             createdBy uuid
             )""").update.run.attemptSql
        .transact(transactor)
        .flatMap {
          case Left(_) => info"Tables relation not created"
          case Right(_) => info"Tables relation created"
        }

    override def get(id: UUID, userId: UUID): F[Option[Service]] =
      (fr"select * from" ++ tableNameFragment ++ fr"where id = $id::uuid and createdBy = $userId::uuid")
        .query[Service]
        .option
        .transact(transactor)

    override def getByApiKey(apiKey: String): F[Option[Service]] =
      (fr"select * from" ++ tableNameFragment ++ fr"where apiKey = $apiKey::uuid")
        .query[Service]
        .option
        .transact(transactor)

    override def search(size: Option[Int], offset: Option[Int], userId: UUID): F[List[Service]] =
      (fr"select * from" ++ tableNameFragment ++
        fr"where createdBy = $userId::uuid limit ${size.getOrElse(10)} offset ${offset.getOrElse(0)}")
        .query[Service]
        .stream
        .compile
        .toList
        .transact(transactor)

    override def insert(service: Service): F[Unit] =
      Update[Service](s"""insert into $tableName (id, name, apiKey, created, createdBy)
           |values (?::uuid, ?, ?, ?, ?::uuid)""".stripMargin)
        .toUpdate0(service)
        .run
        .transact(transactor)
        .void

    override def update(service: Service): F[Unit] =
      Update[Service](s"update $tableName set apiKey = ${service.apiKey} where id = ${service.id}")
        .toUpdate0(service)
        .run
        .transact(transactor)
        .void

    override def delete(id: UUID, userId: UUID): F[Unit] =
      Update[(UUID, UUID)](s"delete from $tableName where id = ?::uuid and createdBy = ?::uuid")
        .toUpdate0(id -> userId)
        .run
        .transact(transactor)
        .void
  }

}
