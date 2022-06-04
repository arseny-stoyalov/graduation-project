package gp.tables.rows

import cats.Monad
import cats.data.OptionT
import gp.tables.rows.errors.InstanceError
import gp.tables.rows.model.Row
import gp.tables.TablesService

import java.util.UUID

package object queue {

  //todo garbage
  private[rows] class Logic[F[_]](tablesService: TablesService[F])(implicit F: Monad[F]) {
    def process(tableId: UUID, row: Row): F[Either[InstanceError, Row]] =
      OptionT(tablesService.get(tableId))
        .toRight(InstanceError.NotFound: InstanceError)
        .map { _ =>
          row
        }.value

  }

}
