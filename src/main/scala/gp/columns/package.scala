package gp

import enumeratum._


package object columns {

  sealed trait ColumnType extends EnumEntry

  object ColumnType extends CirceEnum[ColumnType] with Enum[ColumnType] {
    case object String extends ColumnType
    case object Number extends ColumnType

    override def values: IndexedSeq[ColumnType] = findValues
  }

}
