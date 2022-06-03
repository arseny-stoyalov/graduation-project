package gp.rows

import gp.rows.errors.InstanceError
import gp.rows.model.Row
import gp.rows.queue.RowActionProducer

class RowService[F[_]](storage: RowStorage[F], producer: RowActionProducer[F]) {

  def queuedPut(row: Row, tableId: String): F[Unit] = producer.put(tableId, row)
  def queuedDelete(ids: List[String], tableId: String): F[Unit] = producer.delete(tableId, ids)
  def queuedErase(tableId: String): F[Unit] = producer.erase(tableId)

  private[rows] def directPut(row: Row, tableId: String): F[Int] = storage.insert(row, tableId)
  private[rows] def directDelete(ids: List[String], tableId: String): F[Int] = storage.delete(ids, tableId)
  private[rows] def directErase(tableId: String): F[Either[InstanceError, Int]] = storage.drop(tableId)

}
