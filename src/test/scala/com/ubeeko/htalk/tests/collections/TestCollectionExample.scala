/**
 * *****************************************************************************
 * Copyright(c) 2011-2013 Ubeeko
 * All rights reserved.
 * ****************************************************************************
 */
package com.ubeeko.htalk.tests.collections

import com.ubeeko.htalk.criteria._
import com.ubeeko.htalk.bytesconv._
import com.ubeeko.htalk.tests.TestCommons

import org.apache.hadoop.hbase.filter.ColumnPrefixFilter

import org.scalatest.{ BeforeAndAfterAll, Matchers, FlatSpec }

import scala.collection._

class TestCollectionExample extends FlatSpec with BeforeAndAfterAll with Matchers with TestCommons {
  case class Forum(name: String, description: String, posts: Map[String, String])

  val forumTable = "Forum-" + randomSuffix(5)
  val forum = "example-" + randomSuffix(5)

  val postPrefixStr = "POST"
  val postPrefix = bytesFrom[String](postPrefixStr)

  logger.debug("using forum table %s and forum name %s" format (forumTable, forum))

  override def beforeAll() {
    htalkContext.createTable(forumTable)
  }

  override def afterAll() {
    htalkContext.deleteTable(forumTable)
    super.afterAll()
  }

  class TestQualifier(v: Array[Byte]) extends CompoundQualifier {
    def prefix = v.slice(0, postPrefix.length)
    def suffix = v.slice(postPrefix.length, v.length)
  }

  object TestQualifier {
    def apply(prefix: String, suffix: String) =
      new TestQualifier(bytesFrom[String](prefix + suffix))
  }

  private def postsFromResult(result: Result): Map[String, String] = {
    result getCells() map { case (k, v) =>
      val qualifier =
        if (k.value.startsWith(postPrefix)) {
          val q = new CompoundQualifier {
            def prefix = k.value.slice(0, postPrefix.length)
            def suffix = k.value.slice(postPrefix.length, k.value.length)
          }
          bytesTo[String](q.prefix) + bytesTo[String](q.suffix)
        }
        else
          bytesTo[String](k.value)
      val value = bytesTo[String](v)
      qualifier -> value
    }
  }

  val postContents = Map(
    "1" -> "First post content",
    "2" -> "Second post content",
    "3" -> "Third post content"
  )

  "a collection" must "be put in hbase" in {
    val putOp = {
      val basePutOp = forumTable.put(forum, "name", "sav")
                                .put(forum, "description", "le sav")
      (basePutOp /: postContents) { case (op, (num, contents)) =>
        op.put(forum, TestQualifier(postPrefixStr, num), contents)
      }
    }

    putOp execute
  }

  it must "be retrieved using get" in {
    val res = (forumTable get forum filter new ColumnPrefixFilter(postPrefix)) ~ postsFromResult
    val collection = res.head

    postContents foreach { case (num, contents) =>
      collection(postPrefixStr + num) should be(contents)
    }

    res.head.size should be(3)
  }
}
