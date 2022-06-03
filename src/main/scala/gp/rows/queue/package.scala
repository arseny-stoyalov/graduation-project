package gp.rows

import cats.Monad
import cats.data.OptionT
import gp.rows.errors.InstanceError
import gp.rows.model.Row
import gp.tables.TablesService

package object queue {

  //todo garbage
  private[rows] class Logic[F[_]](tablesService: TablesService[F])(implicit F: Monad[F]) {
    def process(tableId: String, row: Row): F[Either[InstanceError, Row]] =
      OptionT(tablesService.get(tableId))
        .toRight(InstanceError.NotFound: InstanceError)
        .map { _ =>
          row
        }.value

  }

}
