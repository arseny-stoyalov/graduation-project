package gp.services

import cats.data.OptionT
import cats.{Functor, Monad}
import cats.syntax.functor._
import cats.syntax.flatMap._
import gp.core.generators.{CreationMetaDataGenerator, IdGenerator}
import gp.services.ServicesService.ApiKey
import gp.services.errors.ServiceError
import gp.services.model.Service
import gp.users.model.User

import java.util.UUID

class ServicesService[F[_]: Monad](storage: ServicesStorage[F]) {

  def init(): F[Unit] = storage.init()

  def get(id: UUID)(implicit user: User): F[Option[Service]] = storage.get(id, user.id)

  def getByApiKey(apiKey: ApiKey): F[Either[ServiceError, Service]] =
    storage
      .getByApiKey(apiKey)
      .map(_.toRight(ServiceError.NotFound))

  def search(size: Option[Int], offset: Option[Int])(implicit user: User): F[List[Service]] =
    storage.search(size, offset, user.id)

  def add(service: Service)(implicit user: User): F[Service] = {

    val initialize = (IdGenerator.generate[Service] _)
      .andThen(CreationMetaDataGenerator.generate[Service])
      .andThen(_.withApiKey(generateApiKey))

    val initialized = initialize(service)
    storage
      .insert(initialized)
      .as(initialized)
  }

  def delete(id: UUID)(implicit user: User): F[Unit] = storage.delete(id, user.id)

  def updateApiKey(id: UUID)(implicit user: User): F[Either[ServiceError, Service]] =
    get(id)
      .flatMap { op =>
        OptionT
          .fromOption[F](op)
          .semiflatMap { s =>
            val updated = s.withApiKey(generateApiKey)
            storage.update(updated).as(updated)
          }
          .toRight(ServiceError.NotFound: ServiceError)
          .value
      }

  private def generateApiKey: String = UUID.randomUUID().toString.replaceAll("-", "")

}

object ServicesService {
  type ApiKey = String
}
