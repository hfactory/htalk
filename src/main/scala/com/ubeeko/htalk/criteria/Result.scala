package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.bytesconv._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.{Cell => HCell}
import scala.collection.JavaConverters._

case class QualifierNotFoundException(qualifier: String) extends Exception {
  override def getMessage = s"Qualifier '$qualifier' not found"
}

case class Result(result: org.apache.hadoop.hbase.client.Result) {

  def apply[Q](family: Family, qualifier: Q)(implicit convQ: BytesConv[Q]): ValueGetter[Q] =
    getValue(family, qualifier)

  def apply[Q](qualifier: Q)(implicit convQ: BytesConv[Q]): ValueGetter[Q] = getValue(qualifier)

  def getValue[Q](family: Family, qualifier: Q)(implicit convQ: BytesConv[Q]): ValueGetter[Q] =
    new ValueGetter(qualifier, family)

  def getValue[Q](qualifier: Q)(implicit convQ: BytesConv[Q]): ValueGetter[Q] = getValue(Family.Default, qualifier)

  class ValueGetter[Q](val qualifier: Q, val family: Family = Family.Default)(implicit convQ: BytesConv[Q]) {
    def asOpt[R](implicit convR: BytesConv[R]): Option[R] =
      value(convQ(qualifier), family) map convR.apply

    def as[R](implicit convR: BytesConv[R]): R =
      asOpt[R] match {
        case Some(v) => v
        case None => throw new QualifierNotFoundException(qualifier.toString)
      }
  }

  private def value(qualifier: Array[Byte], family: Family = Family.Default): Option[Array[Byte]] = {
    result.getColumnLatestCell(family.value, qualifier) match {
      case cname: HCell => Some(CellUtil.cloneValue(cname))
      case _ => None
    }
  }

  def getRow[R](implicit convR: BytesConv[R]): R = convR(result.getRow)

  def getCells(family: Family = Family.Default): Map[Qualifier, Array[Byte]] = {
    assert(result != null)
    result.getMap match {
      case null => Map.empty
      case m =>
        m.get(Bytes.toBytes(family.value)) match {
          case null => Map.empty
          case familyMap =>
            (familyMap.keySet().asScala map {
              key: Array[Byte] => Qualifier(key) -> familyMap.get(key).firstEntry().getValue
            }).toMap
        }
    }
  }

  def getCellsCount = result.size()

  def isEmpty = result.isEmpty()

  def nonEmpty = !isEmpty
}