package gp.core

import java.util.{Date, UUID}

package object models {

  trait HasId[T <: HasId[T]] {
    def id: UUID
    def withId(id: UUID): T
  }

  trait HasCreationDate[T <: HasCreationDate[T]] {
    def created: Date
    def withCreated(date: Date): T
  }

  trait HasCreator[T <: HasCreator[T]] {
    def createdBy: UUID
    def withCreatedBy(id: UUID): T
  }

}
