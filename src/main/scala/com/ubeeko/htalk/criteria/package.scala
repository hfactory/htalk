package com.ubeeko.htalk

import java.util.Date

import com.ubeeko.htalk.bytesconv._

/* 
 * Copyright Ubeeko
 */

package object criteria {

  implicit def toBytes(value: String): Array[Byte] = bytesFrom[String](value)

  implicit def toQualifier(value: String): Qualifier = Qualifier(value)

  // TODO  implicit def toContent(value: Array[Byte]): Content = new Content(value.getBytes())

  implicit def toTable(value: String): Table = new Table(value)

  implicit def toFamily(value: String): Family = new Family(value)

  implicit def toTimestamp(value: Date): Timestamp = Timestamp(value.getTime)
  implicit def toTimestamp(value: Long): Timestamp = Timestamp(value)
  implicit def toLong(value: Timestamp): Long = value.time

  implicit def toPuts(values: Iterable[Put]): Puts = new Puts(values)
  implicit def toPuts(products: Product): Puts = {
    val iterable = products.productIterator.toIterable.map(_.asInstanceOf[Put])
    new Puts(iterable)
  }
  implicit def toPuts(value: Put): Puts = {
    new Puts(Iterable(value))
  }

  implicit def toDeletes(values: Iterable[Delete]): Deletes = new Deletes(values)
  implicit def toDeletes(products: Product): Deletes = {
    val iterable = products.productIterator.toIterable.map(_.asInstanceOf[Delete])
    new Deletes(iterable)
  }
  implicit def toDeletes(value: Delete): Deletes = {
    new Deletes(Iterable(value))
  }

  implicit def toCellEntry[T](t: Tuple2[Cell, T])(implicit conv: BytesConv[T]): CellEntry[T] =
    new CellEntry(t._1, t._2)

  def reverseTimeKey = Long.MaxValue - new Date().getTime

}
