package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.hbase.HTalkContext
import org.apache.hadoop.hbase.client.{ResultScanner, Scan => HScan}

case class Scan(table: Table) extends Searchable[Scan] {
  private[htalk] def hBaseScan: HScan = {
      val scan = new HScan()

      getFamilyMap.foreach {
        case (family, List()) => scan.addFamily(family.value)
        case (family, qualifiers) => qualifiers.foreach(scan.addColumn(family.value, _))
      }

      makeHBaseFilter() foreach scan.setFilter

      _rangeStart foreach { rk =>
        val value = if (_startExclusive) rk.value :+ 0.toByte else rk.value
        scan.setStartRow(value)
      }
      _rangeStop foreach { rk => scan.setStopRow(rk.value) }

      scan.setReversed(_reversed)

      scan
  }

  protected def toResult(implicit htalkContext: HTalkContext) = {
      val htable = htalkContext.getTable(table.toString)
      val scanner: ResultScanner = htable.getScanner(hBaseScan)

      //TODO non optimized way to get all rows need a refactor
      import scala.collection.JavaConversions._
      val iterator = {
        val it: Iterator[org.apache.hadoop.hbase.client.Result] = scanner.iterator()
        _pageSize match {
          case Some(limit) => it.take(limit)
          case _ => it
        }
      }
      
      iterator.map(Result(_))
  }

  // XXX A refactoring of the Get/Put/Scan hierarchy would allow us
  // to replace this vars with vals.
  protected[htalk] var _rangeStart = Option.empty[Rowkey]
  protected[htalk] var _rangeStop = Option.empty[Rowkey]
  private var _reversed = false
  private var _startExclusive = false

  def range(start: Rowkey, stop: Rowkey): this.type = {
    rangeStart(start)
    rangeStop(stop)
    this
  }

  def range(start: Option[Rowkey], stop: Option[Rowkey]): this.type = {
    rangeStart(start)
    rangeStop(stop)
    this
  }

  def rangeStart(start: Rowkey): this.type = {
    this._rangeStart = Some(start)
    this
  }

  def rangeStart(start: Option[Rowkey]): this.type = {
    start foreach rangeStart
    this
  }

  def rangeStop(stop: Rowkey): this.type = {
    this._rangeStop = Some(stop)
    this
  }

  def rangeStop(stop: Option[Rowkey]): this.type = {
    stop foreach rangeStop
    this
  }

  def reversed: this.type = {
    reverse(true)
  }

  def reverse(b: Boolean): this.type = {
    _reversed = b
    this
  }

  def startExclusive: this.type = {
    excludeStart(true)
  }

  def excludeStart(b: Boolean): this.type = {
    _startExclusive = b
    this
  }
}
