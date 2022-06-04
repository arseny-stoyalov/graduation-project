package gp.tables.rows

import gp.tables.rows.model.Row
import gp.tables.rows.queue.RowActionProducer

import java.util.UUID

class RowService[F[_]](storage: RowStorage[F], producer: RowActionProducer[F]) {

  def get(id: UUID, tableId: UUID): F[Option[Row]] = storage.get(id, tableId)
  def search(size: Option[Int], offset: Option[Int], tableId: UUID): F[List[Row]] =
    storage.search(size, offset, tableId)

  def put(row: Row, tableId: UUID): F[Unit] = producer.put(row, tableId)
  def delete(id: UUID, tableId: UUID): F[Unit] = producer.delete(id, tableId)

  private[rows] def directPut(row: Row, tableId: UUID): F[Int] = storage.insert(row, tableId)
  private[rows] def directDelete(id: UUID, tableId: UUID): F[Int] = storage.delete(id, tableId)

}
