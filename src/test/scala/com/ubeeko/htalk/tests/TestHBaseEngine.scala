package com.ubeeko.htalk.tests
import org.scalatest._
import com.ubeeko.htalk.criteria._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.filter.FilterList
import java.util.HashMap
import java.util.ArrayList

class HBaseEngineSpec extends FlatSpec with Matchers {

  // FIXME Move to put spec.
  "HBaseEngine" should "transform PUT" in {

    val name = "Robbie"

    val put = "user" put ("id", "name", name)
    val rsList = put.toHBasePut
    assertResult(1)(rsList.size)
    val rs = rsList.head
    assert(rs != null)

    asString(rs.getRow()) should be("id")

    val cell = (rs.get("d", "name")).get(0)
    asString(CellUtil.cloneValue(cell)) should be(name)
  }

  it should "transform multiple PUT" in {
    val name = "Robbie"

    val put = "user" put ("id", "name", name) put ("id2", "otherName", name)
    val rsList = put.toHBasePut
    assertResult(2)(rsList.size)

    assert(rsList.forall(rs => rs != null))
    
    val results = rsList.map(rs => (asString(rs.getRow), rs)).toMap

    results.keys.toSet should be(Set("id2", "id"))

    def assertContent(identifier: String, family: String, column: String, value: String) {
      val rs = results(identifier)
      assert(rs.has(family, column))
      val cell = (rs.get(family, column)).get(0)
      asString(CellUtil.cloneValue(cell)) should be(value)
    }

    assertContent("id", "d", "name", name)
    assertContent("id2", "d", "otherName", name)
  }

  it should "transform GET" in {
    val gt = "users" get "one" family ("e") qualifiers ("nom", "prenom") filter (new FilterList)
    val rs = gt hBaseGet
  }

  it should "transform SCAN" in {
    val sc = "users" get rows family ("e") qualifiers ("nom", "prenom") filter (new FilterList)
    val rs = sc hBaseScan
  }

  // FIXME Move to delete spec.
  it should "transform DELETE" in {
    val name = "Robbie"

    val delete = "user" delete ("id", "name")
    val rsList = delete.toHBaseDelete.toList
    assertResult(1)(rsList.size)
    val rs = rsList.head
    assert(rs != null)

    asString(rs.getRow()) should be("id")

    val map = rs.toMap().get("families").asInstanceOf[HashMap[String,ArrayList[HashMap[String, Object]]]]
    map.get("d").get(0).get("qualifier") should be("name")
  }

  def asString(value: Array[Byte]): String = Bytes.toString(value)
}