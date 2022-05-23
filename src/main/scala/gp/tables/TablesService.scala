package gp.tables

import cats.Monad
import gp.tables.model.Table

class TablesService[F[_]](implicit F: Monad[F]) {

  def createTable(): F[Table] = F.pure(Table(""))

  def getTable(id: String): F[Option[Table]] = F.pure(None)

}
