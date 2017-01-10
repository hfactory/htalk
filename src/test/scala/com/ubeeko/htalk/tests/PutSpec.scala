package com.ubeeko.htalk.tests

import com.ubeeko.htalk.bytesconv._
import org.scalatest._
import com.ubeeko.htalk.criteria._
import java.util.Date
import org.apache.hadoop.hbase.util.Bytes
import com.ubeeko.htalk.hbase.HTalkContext
import com.ubeeko.htalk.hbase.TestHBaseManager
import com.ubeeko.htalk.hbase.TestHBaseManager._
import java.util.UUID

class PutSpec extends FlatSpec with Matchers {

  "A Put" should "multiple put and handle call chaining" in {

    val putTable: Table = "put"

    var rs = putTable put ("rowid", "qualifier", "value")
    rs.size should be(1)

    val cell = Cell("rowid", "qualifier")
    var rs2 = putTable put (cell, "value")
    rs2.size should be(2)

    checkPutOnEmptyTable(rs2) conformsTo {
      htalkcontext =>
      val rows = assertTableAndGetRows(htalkcontext, "put", 1)
      val cellsRowId = rows("rowid")
      cellsRowId should have size 2
      val cell1 = cellsRowId.head
      row(cell1) should equal("rowid")
      family(cell1) should equal(Family.Default.value)
      qualifier(cell1) should equal("qualifier")
      cellValue(cell1) should equal("value")
      val cell2 = cellsRowId.tail.head
      row(cell2) should equal("rowid")
      family(cell2) should equal(Family.Default.value)
      qualifier(cell2) should equal("qualifier")
      cellValue(cell2) should equal("value")
    }

    var chained = "chain" put ("rowid", "qualifier", "value") put ("rowid2", "f2", "qualifier2", "value2")
    chained.size should be(2)

    checkPutOnEmptyTable(chained) conformsTo {
      htalkcontext =>
      val rows = assertTableAndGetRows(htalkcontext, "chain", 2)
      val cellsRowId = rows("rowid")
      cellsRowId should have size 1
      val cell1 = cellsRowId.head
      row(cell1) should equal("rowid")
      family(cell1) should equal(Family.Default.value)
      qualifier(cell1) should equal("qualifier")
      cellValue(cell1) should equal("value")
      val cellsRowId2 = rows("rowid2")
      cellsRowId2 should have size 1
      val cell2 = cellsRowId2.head
      row(cell2) should equal("rowid2")
      family(cell2) should equal("f2")
      qualifier(cell2) should equal("qualifier2")
      cellValue(cell2) should equal("value2")
    }
  }

  trait Assertable {
    def conformsTo(assert: HTalkContext => Unit): Unit
  }

  def checkPutOnEmptyTable(put: Put) = {
    implicit val htalkcontext = TestCommons.newContext()

    htalkcontext.createTable(put.table.toString, put.entries.map(cell => cell.cell.family.value).toSet.toSeq, true)
    put execute

    new Assertable {
      def conformsTo(assert: HTalkContext => Unit) = assert(htalkcontext)
    }
  }

  it should "insert Cell with default family" in {
    checkPutOnEmptyTable("user" put ("rowid", "qualifier", "value1")) conformsTo {
      htalkcontext =>
      val cells = assertTableAndGetRows(htalkcontext, "user", 1)("rowid")
      val cell = cells.head
      row(cell) should equal("rowid")
      family(cell) should equal(Family.Default.value)
      qualifier(cell) should equal("qualifier")
      cellValue(cell) should equal("value1")
    }
  }

  it should "insert Cell in specific family" in {
    checkPutOnEmptyTable("user" put ("rowid2", "f2", "qualifier2", "value2")) conformsTo {
      htalkcontext =>
      val cells = assertTableAndGetRows(htalkcontext, "user", 1)("rowid2")
      val cell = cells.head
      row(cell) should equal("rowid2")
      family(cell) should equal("f2")
      qualifier(cell) should equal("qualifier2")
      cellValue(cell) should equal("value2")
    }
  }

  it should "insert Cell with timestamp" in {
    checkPutOnEmptyTable("user" advancedPut ("rowid3", "f3", "qualifier3", new Date().getTime(), "value3")
        ) conformsTo {
      htalkcontext =>
      val cells = assertTableAndGetRows(htalkcontext, "user", 1)("rowid3")
      val cell = cells.head
      row(cell) should equal("rowid3")
      family(cell) should equal("f3")
      qualifier(cell) should equal("qualifier3")
      cellValue(cell) should equal("value3")
    }
  }

  it should "not insert a cell with undefined optional value" in {
    val v: Option[Int] = None
    checkPutOnEmptyTable("user" put ("rowidNone", "optInt", v)) conformsTo { htalkcontext =>
      assertTableAndGetRows(htalkcontext, "user", 0)
    }
  }

  it should "insert cell with defined optional value" in {
    val v = Some(154)
    checkPutOnEmptyTable("user" put ("rowidSome154", "optInt", v)) conformsTo { htalkcontext =>
      val cell = assertTableAndGetRows(htalkcontext, "user", 1)("rowidSome154").head
      row(cell)       should equal("rowidSome154")
      qualifier(cell) should equal("optInt")
      typedCellValue[Int](cell) should equal(v.get)
    }
  }

  it should "not delete existing cell when putting undefined optional value" in {
    implicit val htalkcontext = TestCommons.newContext()

    val table = "some-none"
    htalkcontext.createTable(table, ignoreExisting = true)

    // First, put a row with a defined optional.
    val someInt = Some(123)
    table put ("Some123", "optInt", someInt) execute

    // Then, put the same row with the optional *undefined*.
    val noneInt: Option[Int] = None
    table put ("Some123", "optInt", noneInt) execute

    // The value initially put is still there, it hasn't been deleted by putting
    // the undefined optional.
    val r = table.get(rows).execute.head
    r.getValue("optInt").as[Int] should equal(someInt.get)
  }

  it should "contains Cell values and Table" in {
    val value1 = "TestValue"
    val table = "user"
    val chained = table put ("rowid", "qualifier", value1) put ("rowid2", "qualifier", "value")
    Bytes.toString(chained(0).content) should be(value1)
    chained.table.name should be("user")
  }

  it should "contains convert UUID values" in {
    val value1: UUID = UUID.randomUUID()
    val table = "user"
    val chained = table put ("rowid", "qualifier", value1) put ("rowid2", "qualifier", "value")
    bytesTo[UUID](chained(0).content) should be(value1)
    chained.table.name should be("user")
  }

  it should "convert multiple Put to Puts" in {
    val p1 = "user" put ("id", "user", "Robbie")
    val p2 = "domain" put ("id", "domain", "Alpha")

    val rs: Puts = (p1, p2)
    assert(rs.isInstanceOf[Puts] == true)
  }

}
