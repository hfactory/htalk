package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.hbase.HTalkContext
import com.ubeeko.htalk.filter.Filterable
import com.ubeeko.htalk.filter.TimeFilterable
import org.apache.hadoop.hbase.filter.KeyOnlyFilter
import org.apache.hadoop.hbase.filter.FilterList
import org.apache.hadoop.hbase.client.Mutation
import org.apache.hadoop.hbase.TableNotFoundException
import scala.collection.JavaConversions._

trait HBaseOperation {
  val table: Table
  def mutate(mutations: Seq[Mutation])(implicit htalkContext: HTalkContext): Unit = {
    val mutator = htalkContext.getBufferedMutator(table.toString)
    if (mutator == null)
      throw new TableNotFoundException(table.toString)
    try {
      mutator.mutate(mutations)
      mutator.flush()
    } finally {
      mutator.close()
    }

  }
}

case class Count(rows: Long, cells: Long)

trait Searchable[T <: Searchable[T]] extends HBaseOperation with Filterable[T] with TimeFilterable[T] {
  this: T =>

  private var _families: Map[Family, List[Array[Byte]]] = Map()  
  private var _currentFamily = Family.Default

  def qualifiers(qualifiers: Array[Byte]*): T = {
    this._families = this._families updated (this._currentFamily, qualifiers ++: this._families.getOrElse(this._currentFamily, List()))
    this
  }

  def family(newFamily: Family): T = {
    this._currentFamily = newFamily
    this._families = this._families updated (newFamily, this._families.getOrElse(newFamily, List()))
    this
  }

  protected def getFamilyMap = _families

  //TODO use the RowCounterEndpoint coprocessor here for better performance
  def count(implicit htalkContext: HTalkContext): Count = {
    // FIXME This adds a KeyOnlyFilter to the list EVERY TIME this method is called!
    // Add the KeyOnlyFilter to transfer less data
    this._filter = this._filter match {
      case Some(filter) => Some(new FilterList(FilterList.Operator.MUST_PASS_ALL, filter, new KeyOnlyFilter))
      case None => Some(new FilterList(new KeyOnlyFilter))
    }
    val results = executeAsIterator
    val (rows, cells) = results.foldLeft((0L, 0L)) {
      case ((rowCount, cellCount), res) => (rowCount + 1, cellCount+res.getCellsCount)
    }

    Count(rows, cells)
  }

  // TODO use a lazy iterator instead of a in memory Iterable ???
  def ~[T](f: Result => T)(implicit htalkContext: HTalkContext): Iterable[T] = {
    execute.map(f)
  }

  protected def toResult(implicit htalkContext: HTalkContext): Iterator[Result]
  
  def execute(implicit htalkContext: HTalkContext): Iterable[Result] = {
    executeAsIterator(htalkContext).toIterable
  }
  
  def executeAsIterator(implicit htalkContext: HTalkContext): Iterator[Result] = toResult(htalkContext)

}
