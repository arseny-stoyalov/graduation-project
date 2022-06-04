package gp.utils.routing

import sttp.tapir.{endpoint, EndpointInput}

package object tags {

  trait RouteTag {
    def name: String

    def input: EndpointInput[Unit] = endpoint.input / name
    def tags: List[String] = List(name)
  }

  object RouteTag {

    case object Base extends RouteTag {
      override def name: String = ""
    }

    case object Auth extends RouteTag {
      override def name: String = "auth"
    }

    case object Tables extends RouteTag {
      override def name: String = "tables"
    }

    case object Instances extends RouteTag {
      override def name: String = "instances"
    }

  }

}
