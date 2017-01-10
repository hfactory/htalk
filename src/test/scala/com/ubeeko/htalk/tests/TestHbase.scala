package com.ubeeko.htalk.tests

import com.ubeeko.htalk.bytesconv._
import com.ubeeko.htalk.criteria._
import com.ubeeko.htalk.filter.TimeFilter
import com.ubeeko.htalk.hbase.HBaseManager

import org.apache.hadoop.hbase.filter.SingleColumnValueFilter
import org.apache.hadoop.hbase.filter.FuzzyRowFilter
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp
import org.apache.hadoop.hbase.filter.RowFilter
import org.apache.hadoop.hbase.filter.RegexStringComparator
import org.apache.hadoop.hbase.util.Pair

import org.scalatest._

import scala.collection.JavaConversions._
import scala.compat.Platform
import org.apache.hadoop.hbase.TableExistsException

class TestHbase extends FlatSpec with Matchers with BeforeAndAfterAll with TestCommons {

  lazy val now = Platform.currentTime

  private val userTable = "testhbase-user-" + randomSuffix(4)

  private def createTable(): Unit = htalkContext.createTable(userTable)
  private def deleteTable(): Unit = htalkContext.deleteTable(userTable)

  override def beforeAll(): Unit = createTable()

  override def afterAll(): Unit = {
    deleteTable()
    super.afterAll()
  }

  def userFromResult(rs: Result): User =
    User(rs.getRow[String], rs.getValue("name").as[String], rs.getValue("age").as[Int])

  // For use by test cases that require an empty table with 2 column families.
  trait CleanTableWithF {
    deleteTable()
    htalkContext.createTable(userTable, families = Seq(HBaseManager.defaultFamilyName, "f"))
  }

  // For use by test cases that require an empty table.
  trait CleanTable {
    deleteTable()
    createTable()
  }

  "createHTable" should "raise TableExistsException if the table already exists when ignoreExisting is false (default)" in {
    intercept[TableExistsException] {
      htalkContext.createTable(userTable)
    }
  }

  it should "succeed even if the table exists when ignoreExisting is true" in {
    htalkContext.createTable(userTable, ignoreExisting = true)
  }

  it should "succeed even if the table exists when using a prefix" in {
    htalkContextWithPrefix.createTable(userTable)
    htalkContextWithPrefix.deleteTable(userTable)
  }

  "A Puts" should "insert values into HBase" in {
    // Get time
    now
    (userTable
      put ("user-id1", "name", "ghislain") put ("user-id1", "age", 37)
      put ("user-id2", "name", "eric") put ("user-id2", "age", 36)
      put ("user-id3", "name", "damien") put ("user-id3", "age", 40)) execute
  }

  "A get" should "read values from HBase with gets" in {
    val firstUser = (userTable get "user-id1" qualifiers ("name", "age")) ~ userFromResult
    val secondUser = (userTable get "user-id2" qualifiers ("name", "age")) ~ userFromResult
    assertResult(1)(firstUser.size)
    assertResult("ghislain")(firstUser.head.name)
    assertResult(37)(firstUser.head.age)
    assertResult(1)(secondUser.size)
    assertResult("eric")(secondUser.head.name)
    assertResult(36)(secondUser.head.age)
  }

  it should "get all rows with range start at first rowkey" in {
    val select = userTable get rows rangeStart("user-id1") count

    select.rows should be(3)
    select.cells should be(6)
  }

  it should "exclude first row with a exclusive range start at first rowkey" in {
    val select = userTable get rows rangeStart("user-id1") excludeStart(true) count

    select.rows should be(2)
    select.cells should be(4)
  }


  it should "read the number of rows specified by paging" in {
    val page1 = (userTable get rows limit 1) ~ userFromResult
    assertResult(1)(page1.size)
    assertResult("ghislain")(page1.head.name)

    // XXX Ideally for each page we should determine its start from the rowkey
    // of the last result of the previous page.
    val page2 = (userTable get rows rangeStart "user-id2" limit 1) ~ userFromResult
    assertResult(1)(page2.size)
    assertResult("eric")(page2.head.name)

    val page3 = (userTable get rows rangeStart "user-id3" limit 1) ~ userFromResult
    assertResult(1)(page3.size)
    assertResult("damien")(page3.head.name)
  }

  "A count" should "count rows and cells" in {
    val counter = userTable get rows count

    counter.rows should be(3L)
    counter.cells should be(6L)
  }

  "A scan" should "read values from HBase with scan" in {
    val users = (userTable get rows) ~ userFromResult
    assertResult(3)(users.size)
    val ghislain :: eric :: damien :: Nil = users.toList
    assertResult("ghislain")(ghislain.name)
    assertResult(37)(ghislain.age)
    assertResult("eric")(eric.name)
    assertResult(36)(eric.age)
    assertResult("damien")(damien.name)
    assertResult(40)(damien.age)
  }

  it should "read with column filtering from HBase" in {
    val user = (userTable get rows columnValue("name", "eric")) ~ userFromResult
    assertResult(1)(user.size)
    val user1 = user.head
    assertResult("eric")(user1.name)
    assertResult(36)(user1.age)
    val next = user.tail
    assertResult(Iterable.empty[User])(next)
  }

  it should "read filtered values from HBase" in {
    val filterRobbie = new SingleColumnValueFilter("d", "name", CompareOp.EQUAL, "ghislain")
    val user = (userTable get rows filter (filterRobbie)) ~ userFromResult
    assertResult(1)(user.size)
    val user1 = user.head
    assertResult("ghislain")(user1.name)
    assertResult(37)(user1.age)
    val next = user.tail
  }

  it should "read values passing all filters" in {
    val name = new SingleColumnValueFilter("d", "name", CompareOp.EQUAL, "eric")
    val age  = new SingleColumnValueFilter("d", "age", CompareOp.LESS, bytesFrom(40))
    val users = (userTable get rows filter name filter age) ~ userFromResult
    assertResult(1)(users.size)
    val user1 :: Nil = users.toList
    assertResult("eric")(user1.name)
  }

  it should "read fuzzy filtered rows from HBase" in {
    val filterRabbie = new FuzzyRowFilter(Seq(new Pair("user_id1": Array[Byte], Array[Byte](0, 0, 0, 0, 1, 0, 0, 0))))
    val user = (userTable get rows filter (filterRabbie)) ~ userFromResult
    assertResult(1)(user.size)
    val user1 = user.head
    assertResult("ghislain")(user1.name)
    assertResult(37)(user1.age)
  }

  it should "read only rows in given range from HBase" in {
    val startId = "user-id"
    val stopId  = "user-id2"

    def checkResult(users: Iterable[User]) = {
      assertResult(1)(users.size)
      val user1 = users.head
      assertResult("ghislain")(user1.name)
      assertResult(37)(user1.age)
    }

    // With "direct" rowkey values.
    val users = (userTable get rows range (startId, stopId)) ~ userFromResult
    checkResult(users)

    // With "optional" rowkey values
    val start: Option[Rowkey] = Some(startId)
    val stop : Option[Rowkey] = Some(stopId)
    val users2 = (userTable get rows range (start, stop)) ~ userFromResult
    checkResult(users2)
  }

  it should "read starting from given row from HBase" in {
    val users = (userTable get rows rangeStart "user-id2") ~ userFromResult
    assertResult(2)(users.size)
    val user1 :: user2 :: Nil = users.toList
    assertResult("eric")(user1.name)
    assertResult(36)(user1.age)
    assertResult("damien")(user2.name)
    assertResult(40)(user2.age)
  }

  it should "read until given row from HBase" in {
    val user = (userTable get rows rangeStop "user-id2") ~ userFromResult
    assertResult(1)(user.size)
    val user1 = user.head
    assertResult("ghislain")(user1.name)
    assertResult(37)(user1.age)
  }

  it should "read from first row if given start row is None" in {
    // FIXME Wouldn't have to specify the type if we handled all of this with bytesconv
    // rather than with the Rowkey stuff...
    val start: Option[Rowkey] = None
    val stop : Option[Rowkey] = Some("user-id2")
    val users = (userTable get rows range (start, stop)) ~ userFromResult
    // Stop row is exclusive, so we'll get only 1 row
    assertResult(1)(users.size)
    val user = users.head
    assertResult("ghislain")(user.name)
  }

  it should "read all from given start row if given end row is None" in {
    val start: Option[Rowkey] = Some("user-id2")
    val stop : Option[Rowkey] = None
    val users = (userTable get rows range (start, stop)) ~ userFromResult
    assertResult(2)(users.size)
    val user1 :: user2 :: Nil = users.toList
    assertResult("eric")(user1.name)
    assertResult("damien")(user2.name)
  }

  it should "read rows in reverse if so specified" in {
    // Start and stop rows are inverted.
    val startId = "user-id3"
    val stopId  = "user-id"

    val users = (userTable get rows range (startId, stopId) reversed) ~ userFromResult
    assertResult(3)(users.size)
    val user1 :: user2 :: user3 :: Nil = users.toList
    assertResult("damien")(user1.name)
    assertResult("eric")(user2.name)
    assertResult("ghislain")(user3.name)
  }

  it should "read regexp filtered rows from HBase" in {
    val filterId = new RowFilter(CompareOp.EQUAL, new RegexStringComparator("user.*1"))
    val user = (userTable get rows filter (filterId)) ~ userFromResult
    assertResult(1)(user.size)
    val user1 = user.head
    assertResult("ghislain")(user1.name)
    assertResult(37)(user1.age)
  }

  ignore should "read timestamp range filtered rows from HBase" in {
    val filterId = new TimeFilter(now - 100L, now + 200L)
    val user = (userTable get rows filter (filterId)) ~ userFromResult
    assertResult(3)(user.size)
    val filterFuture = new TimeFilter(now + 10L, now + 20L)
    val noone = (userTable get rows filter (filterFuture)) ~ userFromResult
    assertResult(0)(noone.size)
  }

  it should "read the number of rows specified by limit" in {
    val n = 2  // page size
    // Paging with an int.
    val page1 = (userTable get rows limit n) ~ userFromResult
    assertResult(2)(page1.size)
    val user1 :: user2 :: Nil = page1.toList
    assertResult("ghislain")(user1.name)
    assertResult("eric")(user2.name)

    // Paging with an Option[Int].
    // XXX Ideally we should determine the start from the rowkey
    // of the last result of the previous page.
    val page2 = (userTable get rows rangeStart "user-id3" limit Some(n)) ~ userFromResult
    assertResult(1)(page2.size)
    val user3 = page2.head
    assertResult("damien")(user3.name)
  }

  it should "read all the rows when paging is None" in {
    val users = (userTable get rows limit None) ~ userFromResult
    assertResult(3)(users.size)
    val user1 :: user2 :: user3 :: Nil = users.toList
    assertResult("ghislain")(user1.name)
    assertResult("eric")(user2.name)
    assertResult("damien")(user3.name)
  }

  "A Deletes" should "delete some columns in a given column family" in new CleanTableWithF {
    (userTable
      put ("user-del1", "name", "ghislain") put ("user-del1", "f", "age", "37")
      put ("user-del1", "f", "tag", "one") put ("user-del1", "f", "type", "man")
      put ("user-del2", "name", "eric") put ("user-del2", "f", "age", "36")) execute

    (userTable delete ("user-del1", "f", "age") delete ("user-del2", "name")) execute
    
    val results = (userTable get rows) execute

    assertResult(2)(results.size)
    assertResult("user-del1")(results.head.getRow[String])
    assertResult("ghislain")(results.head.getValue("name").as[String])
    assertResult("one")(results.head.getValue("f", "tag").as[String])
    assertResult("man")(results.head.getValue("f", "type").as[String])
    assertResult(None)(results.head.getValue("f", "age").asOpt[String]) // Does not exists
    assertResult("user-del2")(results.tail.head.getRow[String])
    assertResult(None)(results.tail.head.getValue("name").asOpt[String]) // Does not exists
    assertResult("36")(results.tail.head.getValue("f", "age").as[String])

    (userTable deleteFamily("user-del1", "f")) execute

    val results2 = (userTable get rows) execute

    assertResult(2)(results2.size)
    assertResult("user-del1")(results2.head.getRow[String])
    assertResult("ghislain")(results2.head.getValue("name").as[String])
    assertResult(None)(results2.head.getValue("f", "tag").asOpt[String]) // Does not exists
    assertResult(None)(results2.head.getValue("f", "type").asOpt[String]) // Does not exists
    assertResult(None)(results2.head.getValue("f", "age").asOpt[String]) // Does not exists
    assertResult("user-del2")(results2.tail.head.getRow[String])
    assertResult(None)(results2.tail.head.getValue("name").asOpt[String]) // Does not exists
    assertResult("36")(results2.tail.head.getValue("f", "age").as[String])
  }

  "A Deletes" should "delete some columns" in new CleanTable {

    (userTable
      put ("user-del1", "name", "ghislain") put ("user-del1", "age", 37)
      put ("user-del2", "name", "eric") put ("user-del2", "age", 36)) execute

    (userTable delete ("user-del1", "age") delete ("user-del2", "name")) execute
    
    val results = (userTable get rows) execute

    assertResult(2)(results.size)
    assertResult("user-del1")(results.head.getRow[String])
    assertResult("ghislain")(results.head.getValue("name").as[String])
    assertResult(None)(results.head.getValue("age").asOpt[Int]) // Does not exists
    assertResult("user-del2")(results.tail.head.getRow[String])
    assertResult(None)(results.tail.head.getValue("name").asOpt[String]) // Does not exists
    assertResult(36)(results.tail.head.getValue("age").as[Int])
  }

  it should "delete some rows" in {
    (userTable
      put ("user-del1", "name", "ghislain") put ("user-del1", "age", 37)
      put ("user-del2", "name", "eric") put ("user-del2", "age", 36)) execute

    (userTable delete ("user-del1") delete ("user-del2")) execute
  }

  case class User(id: String, name: String, age: Int)

  case class Tweet(message: String, author: String)
}
