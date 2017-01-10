package com.ubeeko.htalk.tests
import org.scalatest._
import com.ubeeko.htalk.criteria._

class TestFilterSpec extends FlatSpec with Matchers {

  //TODO
  "Filter" should "select Family" in {
    val precise = "user" get "one" family (Family("E"))
  }

  //TODO
  "Filter" should "filter on value" in {
    val precise = "user" get "one" value ("qqch")
  }

  //TODO
  "Filter" should "filter on column value" in {
    val precise = "user" get "one" columnValue ("column", "value")
  }

  //TODO
  "Filter" should "return first key only" in {
    val precise = "user" get "one" firstKeyOnly
  }

  /*TODO
  "Filter" should "offer fuzzy" in {
    val precise = "user" get "one" fuzzy ("qqch")
  }*/

}