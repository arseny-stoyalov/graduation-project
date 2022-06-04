package gp.tables

import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.Monad
import gp.rows.RowService
import gp.tables.TablesService.Log
import gp.tables.model.Table
import tofu.logging.LoggingCompanion
import tofu.syntax.logging._

//todo search with pagination
class TablesService[F[_]](storage: TablesStorage[F], rowService: RowService[F])(implicit F: Monad[F], L: Log[F]) {

  def init(): F[Unit] = storage.create()

  def search(size: Option[Int], offset: Option[Int]): F[List[Table]] = storage.search(size, offset)

  def get(id: String): F[Option[Table]] = storage.get(id)

  //todo take external model, generate id, init instance table
  def add(table: Table): F[Unit] = storage.insert(table) >>
    rowService.init(table.id.toString).flatMap {
      case Left(err) => warn"Table instance was not created ${err.toString}"
      case _ => F.unit
    }

  def delete(id: String): F[Int] = storage.delete(id)

}

object TablesService extends LoggingCompanion[TablesService]
