package gp.tables

import cats.Monad
import gp.tables.model.Table

//todo search with pagination
class TablesService[F[_]](storage: TablesStorage[F])(implicit F: Monad[F]) {

  def init(): F[Unit] = storage.create()

  def search(size: Option[Int], offset: Option[Int]): F[List[Table]] = storage.search(size, offset)

  def get(id: String): F[Option[Table]] = storage.get(id)

  //todo take external model, generate id
  def add(table: Table) = storage.insert(table)

  def delete(id: String) = storage.delete(id)

}