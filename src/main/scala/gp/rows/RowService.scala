package gp.rows

import gp.rows.errors.InstanceError
import gp.rows.model.Row
import gp.rows.queue.RowActionProducer

class RowService[F[_]](storage: RowStorage[F], producer: RowActionProducer[F]) {

  def init(tableId: String): F[Either[InstanceError, Int]] = storage.create(tableId)
  def get(id: String, tableId: String): F[Option[Row]] = storage.get(id, tableId)
  def search(size: Option[Int], offset: Option[Int], tableId: String): F[List[Row]] =
    storage.search(size, offset, tableId)

  def put(row: Row, tableId: String): F[Unit] = producer.put(tableId, row)
  def delete(ids: List[String], tableId: String): F[Unit] = producer.delete(tableId, ids)
  def erase(tableId: String): F[Unit] = producer.erase(tableId)

  private[rows] def directPut(row: Row, tableId: String): F[Int] = storage.insert(row, tableId)
  private[rows] def directDelete(ids: List[String], tableId: String): F[Int] = storage.delete(ids, tableId)
  private[rows] def directErase(tableId: String): F[Either[InstanceError, Int]] = storage.drop(tableId)

}
