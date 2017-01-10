package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.hbase.HTalkContext
import org.apache.hadoop.hbase.client.{Get => HGet}

case class Get(table: Table, rowkey: Rowkey) extends Searchable[Get] {
  private[htalk] def hBaseGet: HGet = {
    val row = rowkey.value
    val get = new HGet(row)

    getFamilyMap.foreach {
      case (family, List()) => get.addFamily(family.value)
      case (family, qualifiers) => qualifiers.foreach(get.addColumn(family.value, _))
    }
    
    _at foreach { ts => get.setTimeStamp(ts) }

    makeHBaseFilter() foreach get.setFilter

    get
  }

  protected def toResult(implicit htalkContext: HTalkContext) = {
    val htable = htalkContext.getTable(table.toString)

    val result = htable.get(hBaseGet)
    List(new Result(result)).iterator
  }
}
