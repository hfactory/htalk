package com.ubeeko.htalk.tests
import com.ubeeko.htalk.criteria._
import com.ubeeko.htalk.hbase.TestHBaseManager
import com.ubeeko.htalk.hbase.HTalkContext
import org.apache.hadoop.hbase.filter.FilterList
import java.util.Date
import org.specs2.mutable.Specification
import scala.util.Try

class GetSpec extends Specification {

  val userTable = "user"

  case class User(val firstname: String, val name: Option[String])

  implicit val htalkcontext = TestCommons.newContext()

  htalkcontext.createTable("user", Seq("p", "u"))
  "user".put("one", "p", "firstname", "One").
         put("one", "p", "name", "Only").
         put("one", "p", "middlename", "and").
         put("two", "p", "uid", 1001).
         put("two", "p", "firstname", "Two").
         put("two", "p", "middlename", "to").
         put("two", "p", "name", "Two").
         execute

  "Get" should {
    "be able to return all qualifier" in {
      ("user" get "one" count) should_== Count(1,3)
      ("user" get "two" count) should_== Count(1,4)
      ("user" get "three" count) should_== Count(1,0)
    }

    "be configurable with a family" in {
      (userTable get "one" family "p" count) should_== Count(1,3)
    }

    "be configurable using qualifiers" in {
      (userTable get "one" family "p" qualifiers ("name", "firstname") count) should_== Count(1,2)
    }

    "be filterable" in {
      //XXX implement some filters in TestHBaseManager
      (userTable get "one" filter new FilterList count) should_== Count(1,3)
    }

    "be complex" in {
      //XXX implement some filters in TestHBaseManager
      (userTable get "two" family "p" qualifiers ("uid") at new Date().getTime filter new FilterList).
        hBaseGet.getFilter() should not beNull
    }

    "be composable to entity" in {
      val composer = (rs: Result) => {
        User(rs.getValue("p", "firstname").as[String], rs.getValue("p", "name").asOpt[String])
      }

      val select = (userTable get "one") ~ composer
      select should have size 1
      select should contain(User("One", Some("Only")))
    }
  }
}
