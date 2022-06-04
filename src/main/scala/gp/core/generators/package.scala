package gp.core

import gp.auth.model.AuthModel
import gp.core.models.{HasCreationDate, HasCreator, HasId}

import java.util.{Date, UUID}

package object generators {

  object IdGenerator {
    def generate[T <: HasId[T]](e: T): T = e.withId(Option(e.id).fold(UUID.randomUUID())(identity))
  }

  object CreationMetaDataGenerator {
    def generate[T <: HasCreationDate[T] with HasCreator[T]](e: T)(implicit creator: AuthModel with HasId[_]): T = {
      val withDate = e.withCreated(Option(e.created).fold(new Date())(identity))
      withDate.withCreatedBy(Option(e.createdBy).fold(creator.id)(identity))
    }
  }

}
