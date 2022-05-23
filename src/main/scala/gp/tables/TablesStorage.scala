package gp.tables

import cats.effect.Resource
import skunk.Session

trait TablesStorage[F[_]] {

}

object TablesStorage {

  class Postgres[F[_]](session: Resource[F, Session[F]]) extends TablesStorage[F] {

  }

}
