package com.ubeeko.htalk.filter

import com.ubeeko.htalk.bytesconv._
import org.apache.hadoop.hbase.filter.{Filter => HFilter, _}
import com.ubeeko.htalk.criteria._

trait Filterable[T <: Searchable[T]] { this: T =>
  var _filter: Option[HFilter] = None
  protected var _pageSize: Option[Int] = None

  def filter(f: HFilter): T = {
    addFilter(f)
    this
  }

  def filter(f: Option[HFilter]): T = {
    f foreach addFilter
    this
  }

  private def addFilter(f: HFilter): Unit =
    _filter match {
      case Some(fl: FilterList) => fl.addFilter(f)
      case Some(g)              => _filter = Some(new FilterList(g, f))
      case None                 => _filter = Some(f)
    }

  // We don't check if there's already a PageFilter in _filter.
  // We assume that one will use the 'limit' directive of HTalk rather
  // than PageFilter. (Should we enforce that?)
  protected def makeHBaseFilter(): Option[HFilter] = {
    val pageFilter = _pageSize map (new PageFilter(_))
    (_filter, pageFilter) match {
      case (None   , None   ) => None
      case (None   , p      ) => p
      case (f      , None   ) => f
      case (Some(f), Some(p)) => Some(new FilterList(f, p))
    }
  }

  def limit(count: Int): T = {
    limit(Some(count))
    this
  }

  def limit(count: Option[Int]): T = {
    _pageSize = count
    this
  }

  def value[V](value: V, op: CompareFilter.CompareOp = CompareFilter.CompareOp.EQUAL)(implicit conv: BytesConv[V]): T = {
    val valueFilter = new ValueFilter(op, new BinaryComparator(conv(value)))
    addFilter(valueFilter)
    this
  }

  def columnValue[V](qualifier: Qualifier, value: V, op: CompareFilter.CompareOp = CompareFilter.CompareOp.EQUAL)
                    (implicit conv: BytesConv[V]): T = {
    import com.ubeeko.htalk.criteria._
    columnValue(Family.Default, qualifier, value, op)
  }

  def columnValue[V](family: Family, qualifier: Qualifier, value: V)(implicit conv: BytesConv[V]): T = {
    columnValue(family, qualifier, value, CompareFilter.CompareOp.EQUAL)
  }

  def columnValue[V](family: Family, qualifier: Qualifier, value: V, op: CompareFilter.CompareOp)
                    (implicit conv: BytesConv[V]): T = {
    val columnValueFilter = new SingleColumnValueFilter(family.value, qualifier.value, op, conv(value))
    columnValueFilter.setFilterIfMissing(true)

    addFilter(columnValueFilter)

    this
  }

  /*TODO fuzzy filter on value
  def fuzzy(value: Any): T = {
    this
  }*/

  // TODO filter on First key only
  def firstKeyOnly: T = {
    addFilter(new FirstKeyOnlyFilter())
    this
  }

}
