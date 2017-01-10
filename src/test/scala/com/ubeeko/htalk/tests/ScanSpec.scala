package com.ubeeko.htalk.tests

import org.scalatest._
import com.ubeeko.htalk.criteria._
import org.apache.hadoop.hbase.filter.FilterList
import com.ubeeko.htalk.hbase.HTalkContext
import com.ubeeko.htalk.hbase.TestHBaseManager

class ScanSpec extends FlatSpec with Matchers {

  val userTable: Table = "user"

  case class User(val firstname: String, val name: Option[String])

  implicit val htalkcontext = TestCommons.newContext()

  htalkcontext.createTable("user", Seq("p", "u"))
  "user" put("eleblouch", "p", "firstname", "Éric") put("eleblouch", "p", "name", "Le Blouc'h") put(
      "dcollard", "p", "firstname", "Damien") put("dcollard", "u", "uid", 1001) execute

  "Scan" should "be able to scan all rows" in {
    val select = "user" get rows count

    select.rows should be(2)
    select.cells should be(4)
  }

  it should "scan with families" in {
    val selectP = userTable get rows family("p") count

    selectP.rows should be(2)
    selectP.cells should be(3)

    val selectU = userTable get rows family("u") count
    
    selectU.rows should be(1)
    selectU.cells should be(1)
  }

  it should "scan with qualifiers" in {
    val selectF = userTable get rows family("p") qualifiers("firstname") count

    selectF.rows should be(2)
    selectF.cells should be(2)

    val selectA = userTable get rows family("p") qualifiers("firstname", "name") count

    selectA.rows should be(2)
    selectA.cells should be(3)

    val selectN = userTable get rows family("p") qualifiers("name") count
    
    selectN.rows should be(1)
    selectN.cells should be(1)
  }

  it should "scan using filters" in {
    val scanner = userTable get rows filter (new FilterList)
  }

  it should "be composable to entity" in {

    val composer = (rs: Result) => {
      
      User(rs.getValue("p", "firstname").as[String], rs.getValue("p", "name").asOpt[String])
    }

    val select = (userTable get rows) ~ composer

    select should have size 2
    select should contain(User("Éric", Some("Le Blouc'h")))
    select should contain(User("Damien", None))
  }

  // XXX Scan.getStartRow does not work with the fake implementation 
  ignore should "scan all with a range start at first rowkey" in {
    val select = "user" get rows rangeStart("dcollard") count

    select.rows should be(2)
    select.cells should be(4)
  }

  // XXX Scan.getStartRow does not work with the fake implementation 
  ignore should "exclude first row with a exclusive range start at first rowkey" in {
    val select = "user" get rows rangeStart("dcollard") excludeStart(true) count

    select.rows should be(1)
    select.cells should be(2)
  }
}
