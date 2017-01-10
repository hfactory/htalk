package com.ubeeko.htalk.filter

import com.ubeeko.htalk.criteria.Searchable
import com.ubeeko.htalk.criteria.Timestamp
import scala.concurrent.duration.Duration

/* 
 * Copyright Ubeeko
 */

trait TimeFilterable[T <: Searchable[T]] {
  searchable: T =>
    
  var _at: Option[Timestamp] = None

  // TODO Filter on defined timestamp
  def at(ts: Timestamp): T = {
    _at = Some(ts)
    searchable 
  }

  // Bug : for time ranges : https://issues.apache.org/jira/browse/HBASE-10102
  // The oldest possible value is returned instaed of the latest
  // TODO Filter after timestamp
  def after(ts: Timestamp): T = {
    searchable
  }

  // TODO Filter before timestamp
  def before(ts: Timestamp): T = {
    searchable
  }

  // TODO Filter before timestamp
  def between(start: Timestamp, end: Timestamp): T = {
    searchable
  }

  // TODO last 2.days 
  def last(duration: Duration): T = {
    searchable
  }

  // TODO after today midnight
  def today(): T = {
    searchable
  }

}
