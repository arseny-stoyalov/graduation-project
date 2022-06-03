package gp.utils.formats

import doobie.{Get, Put}

import java.util.UUID

package object postgres {

  implicit val uuidGet: Get[UUID] = Get[String].map(UUID.fromString)
  implicit val uuidPut: Put[UUID] = Put[String].contramap(_.toString)

}
