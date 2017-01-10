package com.ubeeko.htalk.tests.bootstrap

import com.ubeeko.htalk.bytesconv._
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import net.liftweb.json._
import com.ubeeko.htalk.bootstrap.JSONData._
import com.ubeeko.htalk.criteria.Rowkey
import com.ubeeko.htalk.criteria.Timestamp
import com.ubeeko.htalk.criteria.Family

class TestJSONDataSpec extends FlatSpec with Matchers {
  implicit val formats = DefaultFormats

  "Types" must "be extracted from the JSON" in {
    val JSONRepr = parse("""{"types":{
          "id":"int", "name":"string"
        }}""")
    val types = getTypes(JSONRepr)
    types should contain(("id", "int"))
    types should contain(("name", "string"))
  }

  "A JSON String" must "be parsed to a JSONData object" in {
    val JSONRepr = parse("""{"types":{
          "id":"int", "name":"string", "weight":"long"
        },
        "table":{
	      "name":"testName",
          "rows":[
	        {"id":3, "c":[{"q":"name", "v":"Test"}]},
	        {"id":"4", "ts": 10, "c":[
	          {"f":"fam", "q":"name", "v":5},
	          {"f":"fam", "q":"weight", "v":53},
	          {"f":"d", "q":"double", "v":3.3} 
        ]}]}}""")
    getTypes(JSONRepr).foreach {
      case (name, typeName) =>
        fixType(name, typeName)
    }
    val table = getTable(JSONRepr)
    getName(table) should be(Some("testName"))
    val rows = getRows(table)
    rows.length should be(2)
    val row1 = rows(0)
    getRowId(row1).value should be(Rowkey(3).value)
    val row1Cols = getColumns(row1)
    row1Cols.length should be(1)
    val row1Col = row1Cols(0)
    getFamily(row1Col) should be("d")
    getQualifier(row1Col) should be("name")
    getValue(row1Col) should be(bytesFrom[String]("Test"))
    val row2 = rows(1)
    getRowId(row2).value should be(Rowkey(4).value)
    getTimestamp(row2) should be(Some(10L: Timestamp))
    val row2Cols = getColumns(row2)
    row2Cols.length should be(3)
    val row2Col1 = row2Cols(0)
    getFamily(row2Col1) should be("fam")
    getQualifier(row2Col1) should be("name")
    getValue(row2Col1) should be(bytesFrom[String]("5"))
    val row2Col2 = row2Cols(1)
    getFamily(row2Col2) should be("fam")
    getQualifier(row2Col2) should be("weight")
    getValue(row2Col2) should be(bytesFrom[Long](53L))
    val row2Col3 = row2Cols(2)
    getFamily(row2Col3) should be("d")
    getQualifier(row2Col3) should be("double")
    getValue(row2Col3) should be(bytesFrom[Double](3.3))
    clear
  }

  it must "be parsed even without types information" in {
    val JSONRepr = parse("""{"table":{
        "name":"test",
        "rows":[
        {"id":3, "c":[{"q":"name", "v":"Test"}]},
        {"id":"rowId", "c":[
          {"f":"fam", "q":"Int", "v":5},
          {"f":"d", "q":"double", "v":3.3} 
        ]}]}}""")
    val table = getTable(JSONRepr)
    getName(table) should be(Some("test"))
    val rows = getRows(table)
    rows.length should be(2)
    val row1 = rows(0)
    getRowId(row1).value should be(Rowkey(BigInt(3)).value)
    val row1Cols = getColumns(row1)
    row1Cols.length should be(1)
    val row1Col = row1Cols(0)
    getFamily(row1Col) should be("d")
    getQualifier(row1Col) should be("name")
    getValue(row1Col) should be(bytesFrom[String]("Test"))
    val row2 = rows(1)
    getRowId(row2).value should be(Rowkey("rowId").value)
    val row2Cols = getColumns(row2)
    row2Cols.length should be(2)
    val row2Col1 = row2Cols(0)
    getFamily(row2Col1) should be("fam")
    getQualifier(row2Col1) should be("Int")
    getValue(row2Col1) should be(bytesFrom[BigInt](BigInt(5)))
    val row2Col2 = row2Cols(1)
    getFamily(row2Col2) should be("d")
    getQualifier(row2Col2) should be("double")
    getValue(row2Col2) should be(bytesFrom[Double](3.3))
  }

  it must "be parsed with families and the default family must be present" in {
    val JSONRepr = parse("""{"table":{
        "name":"test",
        "families":[
          {"name":"testFam"},
          {"name":"fam"}
        ]}}""")
    val table = getTable(JSONRepr)
    getName(table) should be(Some("test"))
    val families = getFamilyNames(table)
    families.size should be(3)
    families should contain(Family.Default)
    families should contain("fam": Family)
    families should contain("testFam": Family)
  }
}