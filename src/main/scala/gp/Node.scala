package gp

import com.typesafe.config.{Config, ConfigFactory}
import enumeratum.{Enum, EnumEntry}
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.auto._
import pureconfig.generic.semiauto._

sealed trait Node extends EnumEntry

object Node extends Enum[Node] {
  case object Tables extends Node
  case object Auth extends Node

  implicit private val nodeRoleHint: ConfigReader[Node] = deriveEnumerationReader[Node](identity[String] _)

  def obtain: Node = {
    val raw = ConfigFactory.load()
    ConfigSource.fromConfig(raw)
      .loadOrThrow[RoleAccessor]
      .node
  }

  private case class RoleAccessor(node: Node)

  override def values = findValues
}

