package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.bytesconv._
import com.ubeeko.htalk.hbase.{HTalkContext, TestHBaseManager}
import com.ubeeko.htalk.tests.TestCommons
import org.apache.hadoop.hbase.filter.ColumnPrefixFilter
import org.specs2.mutable.Specification

class ResultSpec extends Specification {
  val table = "result-spec-test"

  implicit val hTalkContext = TestCommons.newContext()

  hTalkContext.createTable(table)

  val translations: Map[String, Map[String, String]] = Map(
    "one" -> Map("fr" -> "un"  , "es" -> "uno"),
    "two" -> Map("fr" -> "deux", "es" -> "dos")
  )

  translations foreach { case (numberInEnglish, langMap) =>
    langMap foreach { case (lang, numberInLang) =>
      table.put(numberInEnglish, lang, numberInLang).execute
    }
  }

  "getCells" should {
    "return an empty map if the result is empty" in {
      val r = table.get("non-existent-rowkey").execute.head
      r.isEmpty must beTrue
      r.getCells() must beEmpty
    }

    "return the columns of the specified family" in {
      val number = "one"
      val r = table.get(number).execute.head
      r.nonEmpty must beTrue

      val cells = r.getCells()

      // Contains exactly two cells and these cells must match the
      // data initially stored above.
      cells.size must_== 2

      val conv = bytesTo[String] _
      val stringCells = cells map { case (qualifier, value) =>
        (conv(qualifier), conv(value))
      }

      val langs = translations(number)
      langs forall { case (lang, numberInLang) =>
        stringCells.get(lang) must beSome(numberInLang)
      }
    }
  }
}
