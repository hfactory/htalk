package com.ubeeko.htalk.tests

import org.apache.hadoop.conf.Configuration
import com.typesafe.scalalogging.slf4j.Logging
import org.scalatest.{Suite, BeforeAndAfterAll}
import scala.util.Random
import com.ubeeko.htalk.hbase.{HTalkContext, HBaseManager, TestHBaseManager, HTalkOptions}

trait TestCommons extends BeforeAndAfterAll with Logging { this: Suite =>
  implicit val htalkContext = HTalkContext(HBaseManager(getHBaseConfiguration))
  val htalkContextWithPrefix = HTalkContext(HBaseManager(getHBaseConfiguration),
                                            Some(HTalkOptions(prefix=Some("______"))))

  override protected def afterAll(): Unit = {
    println("Closing HTalk context")
    htalkContext.close()
    htalkContextWithPrefix.close()
    super.afterAll()
  }

  def getHBaseConfiguration: Configuration = {
    val username = System.getProperty("user.name")
    val hbaseSiteFilename = "hbase-site-" + username + ".properties"

    HBaseManager.getHBaseConfiguration(hbaseSiteFilename)
  }

  // XXX Use StringUtils.randomString once the containing package is extracted
  // from HFactory and moved into a separate ubeeko util lib.
  def randomSuffix(length: Int): String = {
    def randomLetter: Char = ('a'.toByte + Random.nextInt(26).toByte).toChar
    Stream.continually(randomLetter).take(4).mkString
  }

}

object TestCommons {
  def newContext(options: Option[HTalkOptions] = None) = {
    HTalkContext(TestHBaseManager.emptyInstance, options)
  }
}
