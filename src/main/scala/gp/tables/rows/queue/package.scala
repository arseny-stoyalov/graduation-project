package gp.tables.rows

import cats.Monad
import cats.data.OptionT
import cats.syntax.either._
import gp.columns.ColumnType
import gp.columns.model.ColumnDescription
import gp.columns.values._
import gp.core.generators.{CreationMetaDataGenerator, IdGenerator}
import gp.services.model.Service
import gp.tables.TablesService
import gp.tables.rows.errors.InstanceError
import gp.tables.rows.model.Row
import io.circe.Json

import java.util.UUID

package object queue {

  private[rows] class Logic[F[_]](tablesService: TablesService[F])(implicit F: Monad[F]) {
    def process(tableId: UUID, untypedRow: Map[String, Json])(implicit
      auth: Service
    ): F[Either[InstanceError, Row]] =
      OptionT(tablesService.get(tableId))
        .toRight(InstanceError.NotFound: InstanceError)
        .subflatMap { table =>
          def typeRow(desc: ColumnDescription, untyped: Option[Json]): Either[InstanceError, Value] =
            untyped
              .toRight(InstanceError.ColumnMissing(desc.id): InstanceError)
              .flatMap { v =>
                val decoded = desc.`type` match {
                  case ColumnType.String => v.as[StringValue]
                  case ColumnType.Int => v.as[IntValue]
                }
                decoded
                  .map(d => d: Value)
                  .leftMap(_ => InstanceError.IncompatibleTypes(desc.id, desc.`type`.entryName): InstanceError)
              }

          table.columns
            .foldLeft(Map.empty[String, Value].asRight[InstanceError]) { case (collected, next) =>
              for {
                c <- collected
                typed <- typeRow(next, untypedRow.get(next.id))
              } yield c + (next.id -> typed)
            }
            .map { typedEntity =>
              val initialize = IdGenerator.generate[Row] _ andThen CreationMetaDataGenerator.generate[Row]
              initialize(Row.fromEntity(typedEntity))
            }
        }
        .value

  }

}
