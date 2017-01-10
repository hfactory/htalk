package com.ubeeko.htalk.filter

import org.apache.hadoop.hbase.filter.{Filter => HFilter}

/* 
 * Copyright Ubeeko
 * @author Éric Le Blouc'h
 */

trait Filter {
  protected[ubeeko] def getFilter: HFilter
}