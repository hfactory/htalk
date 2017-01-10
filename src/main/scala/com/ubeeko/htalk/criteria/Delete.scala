package com.ubeeko.htalk.criteria

import scala.collection.mutable.ListBuffer
import com.ubeeko.htalk.hbase.HTalkContext
import org.apache.hadoop.hbase.client.{Delete => HDelete}

sealed abstract class DeleteInformation(val rowkey: Rowkey)
case class RowDelete(override val rowkey: Rowkey) extends DeleteInformation(rowkey)
case class FamilyDelete(override val rowkey: Rowkey, family: Family) extends DeleteInformation(rowkey)
case class QualifierDelete(override val rowkey: Rowkey, family: Family, qualifier: Qualifier) extends DeleteInformation(rowkey)

case class Delete(table:Table) extends HBaseOperation with Iterable[DeleteInformation] {
  val entries = new ListBuffer[DeleteInformation]

  def iterator = entries.iterator

  def delete(rowkey: Rowkey): Delete = {
    entries += RowDelete(rowkey)
    this
  }

  def deleteFamily(rowkey: Rowkey, family: Family): Delete = {
    entries += FamilyDelete(rowkey, family)
    this
  }

  def delete(rowkey: Rowkey, family: Family, qualifier: Qualifier): Delete = {
    entries += QualifierDelete(rowkey, family, qualifier)
    this
  }

  def delete(rowkey: Rowkey, qualifier: Qualifier): Delete = {
    entries += QualifierDelete(rowkey, Family.Default, qualifier)
    this
  }

  def execute(implicit htalkContext: HTalkContext) {
    val deletes = toHBaseDelete
    if (table.autoFlushEnabled) {
      mutate(deletes)
    } else {
      val htable = htalkContext.getTable(table.toString)
	    try {
	      deletes.foreach(htable.delete)
      } finally {
        if (htable != null)
          htable.close()
      }
    }
  }

  protected[htalk] def toHBaseDelete: Seq[HDelete] = {
    val deletes = scala.collection.mutable.Map.empty[IndexedSeq[Any], HDelete]
    entries foreach { delete =>
      val row = delete.rowkey.value
      val remove = deletes.getOrElseUpdate(row.deep, new HDelete(row))
      delete match {
        case _: RowDelete => // Nothing to add
        case familyDelete: FamilyDelete =>
          remove.addFamily(familyDelete.family.value)
        case qualifierDelete: QualifierDelete =>
          remove.addColumn(qualifierDelete.family.value, qualifierDelete.qualifier.value)
      }
    }
    deletes.values.toSeq
  }
}

// Groups of Entry
object Deletes {
  def apply(values: Iterable[Delete]): Deletes = new Deletes(values)
}

class Deletes(val values: Iterable[Delete]) {
  def apply() = values
}
