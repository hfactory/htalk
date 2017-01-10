package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.bytesconv._

class Table(val name: String) {

  // FIXME Get rid of these!
  lazy val _put: Put = new Put(this)
  lazy val _delete: Delete = new Delete(this)

  private var _autoFlush: Boolean = true

  def autoFlush(enabled: Boolean): this.type = {
    _autoFlush = enabled
    this
  }

  def autoFlushEnabled: Boolean = _autoFlush

  def put[T](cell: Cell, value: T)(implicit conv: BytesConv[T]): Put = {
    _put.put(cell, value)
  }

  def put[T](cell: Cell, value: Option[T])(implicit conv: BytesConv[T]): Put =
    _put.put(cell, value)

  def put[T](rowkey: Rowkey, qualifier: Qualifier, value: T)(implicit conv: BytesConv[T]): Put = {
    _put.put(rowkey, qualifier, value)
  }

  def put[T](rowkey: Rowkey, qualifier: Qualifier, value: Option[T])(implicit conv: BytesConv[T]): Put =
    _put.put(Cell(rowkey, qualifier), value)

  def put[T](rowkey: Rowkey, family: Family, qualifier: Qualifier, value: T)(implicit conv: BytesConv[T]): Put = {
    _put.put(rowkey, family, qualifier, value)
  }

  def put[T](rowkey: Rowkey, family: Family, qualifier: Qualifier, value: Option[T])(implicit conv: BytesConv[T]): Put =
    _put.put(Cell(rowkey, family, qualifier), value)

  def advancedPut[T](rowkey: Rowkey, family: Family, qualifier: Qualifier, ts: Timestamp, value: T)
                    (implicit conv: BytesConv[T]): Put = {
    _put.advancedPut(rowkey, family, qualifier, ts, value)
  }

  def advancedPut[T](rowkey: Rowkey, family: Family, qualifier: Qualifier, ts: Timestamp, value: Option[T])
                    (implicit conv: BytesConv[T]): Put =
    _put.advancedPut(rowkey, family, qualifier, ts, value)

  def delete(rowkey: Rowkey): Delete = {
    _delete.delete(rowkey)
  }

  def delete(rowkey: Rowkey, qualifier: Qualifier): Delete = {
    _delete.delete(rowkey, qualifier)
  }

  def delete(rowkey: Rowkey, family: Family, qualifier: Qualifier): Delete = {
    _delete.delete(rowkey, family, qualifier)
  }

  def deleteFamily(rowkey: Rowkey, family: Family): Delete = {
    _delete.deleteFamily(rowkey, family)
  }
  
  def get(rowkey: Rowkey): Get = {
    Get(this, rowkey)
  }

  def get(rows: AllRows) = {
    Scan(this)
  }

  override def toString = name
}

object Table {
  def apply(value: String) = new Table(value)
}

sealed trait AllRows

case object rows extends AllRows
