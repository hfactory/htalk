/**
 *
 */
package com.ubeeko.htalk.filter

import com.ubeeko.htalk.criteria._
import org.apache.hadoop.hbase.filter.{Filter => HFilter, SingleColumnValueFilter}
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp

/**
 * Copyright Ubeeko
 * @author Éric Le Blouc'h
 *
 */
case class ColumnFilter(column: String, value:String) extends Filter {
  def getFilter : HFilter = {
    new SingleColumnValueFilter("d", column, CompareOp.EQUAL, value)
  }
}