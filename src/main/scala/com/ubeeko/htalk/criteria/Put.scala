package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.bytesconv._

import scala.collection.mutable.ListBuffer
import com.ubeeko.htalk.hbase.HTalkContext
import scala.collection.JavaConversions._
import org.apache.hadoop.hbase.client.{Put => HPut}
import org.apache.hadoop.hbase.TableNotFoundException

case class Put(table: Table) extends HBaseOperation with Iterable[CellEntry[_]] {
  val entries = new ListBuffer[CellEntry[_]]

  def iterator = entries.iterator

  def apply(position: Int): CellEntry[_] = entries(position)

  def put[T](cell: Cell, value: T)(implicit conv: BytesConv[T]): Put = {
    entries += ((cell, value))
    this
  }

  def put[T](cell: Cell, value: Option[T])(implicit conv: BytesConv[T]): Put = {
    value foreach { v => entries += ((cell, v)) }
    this
  }

  def put[T](rowkey: Rowkey, qualifier: Qualifier, value: T)(implicit conv: BytesConv[T]): Put =
    put(Cell(rowkey, qualifier), value)

  def put[T](rowkey: Rowkey, qualifier: Qualifier, value: Option[T])(implicit conv: BytesConv[T]): Put =
    put(Cell(rowkey, qualifier), value)

  def put[T](rowkey: Rowkey, family: Family, qualifier: Qualifier, value: T)(implicit conv: BytesConv[T]): Put =
    put(Cell(rowkey, family, qualifier), value)

  def put[T](rowkey: Rowkey, family: Family, qualifier: Qualifier, value: Option[T])(implicit conv: BytesConv[T]): Put =
    put(Cell(rowkey, family, qualifier), value)

  def advancedPut[T](rowkey: Rowkey, family: Family, qualifier: Qualifier, ts: Timestamp, value: T)
                    (implicit conv: BytesConv[T]): Put = {
    entries += ((ExplicitlyTimestampedCell(rowkey, qualifier, family, ts), value))
    this
  }

  def advancedPut[T](rowkey: Rowkey, family: Family, qualifier: Qualifier, ts: Timestamp, value: Option[T])
                    (implicit conv: BytesConv[T]): Put = {
    value foreach { v =>
      entries += ((ExplicitlyTimestampedCell(rowkey, qualifier, family, ts), v))
    }
    this
  }

  def execute(implicit htalkContext: HTalkContext): Unit = {
    val puts = toHBasePut
    if (table.autoFlushEnabled) {
      mutate(puts)
    } else {
      val htable = htalkContext.getTable(table.toString)
      if (htable == null)
        throw new TableNotFoundException(table.toString)
      htable.put(puts)
    }
  }

  protected[htalk] def toHBasePut: Seq[HPut] = {
    val puts = scala.collection.mutable.Map.empty[(IndexedSeq[Any], Option[Timestamp]), HPut]
    entries foreach { ce =>
      val row = ce.cell.rowkey.value
      def createPut = ce.cell match {
        case etc: ExplicitlyTimestampedCell => new HPut(row, etc.timestamp)
        case _ => new HPut(row)
      }
      val ts = ce.cell match {
        case etc: ExplicitlyTimestampedCell => Some(etc.timestamp)
        case _ => None
      }
      // As explained in hf-17 issue, it is not recommended to set the timestamp manually
      // It is still possible but only using an advancedPut insteadOf a put
      // Don't know yet what the impact of this modification is on the rest of the code but unit tests are still ok.
      val build = puts.getOrElseUpdate((row.deep, ts), createPut)
      val family: Array[Byte] = ce.cell.family.value
      val qualifier: Array[Byte] = ce.cell.qualifier.value
      val value: Array[Byte] = ce.content

      build.addColumn(family, qualifier, value)
    }
    puts.values.toSeq
  }
}

object Puts {
  def apply(values: Iterable[Put]): Puts = new Puts(values)
}

class Puts(val values: Iterable[Put]) {
  def apply() = values
}
