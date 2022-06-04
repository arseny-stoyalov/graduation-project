package gp.services.model

import io.circe.generic.JsonCodec
import io.scalaland.chimney.dsl._

package object formats {

  object external {

    @JsonCodec
    case class InputService(
      name: String
    ) {
      def asService: Service =
        this
          .into[Service]
          .withFieldConst(_.id, null)
          .withFieldConst(_.created, null)
          .withFieldConst(_.createdBy, null)
          .withFieldConst(_.apiKey, null)
          .transform
    }

  }

}
