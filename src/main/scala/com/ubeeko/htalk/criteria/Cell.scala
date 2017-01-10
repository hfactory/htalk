package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.bytesconv._

class Cell(val rowkey: Rowkey, val family: Family, val qualifier: Qualifier) {
  override def toString = "Cell"
}

object Cell {
  def apply(rowkey: Rowkey, qualifier: Qualifier): Cell = {
    new Cell(rowkey, Family.Default, qualifier)
  }
  def apply(rowkey: Rowkey, family: Family, qualifier: Qualifier): Cell = {
    new Cell(rowkey, family, qualifier)
  }
}

class ExplicitlyTimestampedCell(rowkey: Rowkey, family: Family, qualifier: Qualifier, val timestamp: Timestamp)
    extends Cell(rowkey, family, qualifier)


object ExplicitlyTimestampedCell {
  def apply(Rowkey: Rowkey, qualifier: Qualifier, family: Family, timestamp: Timestamp): Cell = {
    new ExplicitlyTimestampedCell(Rowkey, family, qualifier, timestamp)
  }
}

case class CellEntry[T](cell: Cell, value: T)(implicit conv: BytesConv[T]) {
  def key: Array[Byte] = cell.rowkey.value
  def content: Array[Byte] = conv(value)
}
