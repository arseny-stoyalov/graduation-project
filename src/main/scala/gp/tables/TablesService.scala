package gp.tables

import cats.Monad
import cats.syntax.functor._
import gp.core.generators.{CreationMetaDataGenerator, IdGenerator}
import gp.tables.TablesService.Log
import gp.tables.instances.InstanceHandler
import gp.tables.model.Table
import gp.users.model.User
import tofu.logging.LoggingCompanion

import java.util.UUID

class TablesService[F[_]](storage: TablesStorage[F], handler: InstanceHandler[F])(implicit F: Monad[F], L: Log[F]) {

  def init(): F[Unit] = storage.init()

  def search(size: Option[Int], offset: Option[Int]): F[List[Table]] = storage.search(size, offset)

  def get(id: UUID): F[Option[Table]] = storage.get(id)

  def add(table: Table)(implicit user: User): F[Table] = {
    val initialized = (IdGenerator.generate[Table] _ andThen CreationMetaDataGenerator.generate[Table])(table)

    handler.create(initialized)
      .as(initialized)
  }

  def delete(id: UUID): F[Unit] = handler.delete(id)

}

object TablesService extends LoggingCompanion[TablesService]
