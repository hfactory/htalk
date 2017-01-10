package com.ubeeko.htalk.tests
import org.scalatest._
import com.ubeeko.htalk.criteria._

class TableSpec extends FlatSpec with Matchers {

  val userTable: Table = "user"

  "A Table" should "be created from a string" in {
    userTable.toString === "user"
  }

}