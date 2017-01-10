package com.ubeeko.htalk.tests.bootstrap

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import com.ubeeko.htalk.bootstrap.Bootstrap
import org.apache.hadoop.hbase.client.ConnectionFactory
import com.ubeeko.htalk.tests.TestCommons
import org.apache.hadoop.hbase.TableName

class TestBootstrapSpec extends FlatSpec with Matchers with TestCommons {
  "A Bootstrap process" should "create table forum" in {
    val config = getHBaseConfiguration
    val forumTableName = TableName.valueOf("test-hild-forum")
    lazy val connection = ConnectionFactory.createConnection(config)
    def hAdmin = connection.getAdmin

    try {
      Bootstrap.loadHboFiles(
        jsonFiles     = Iterable("HBase/Bootstrap/data.json"),
        createOnly    = true,
        classLoader   = None,
        configuration = Some(config)
      )
      hAdmin.isTableAvailable(forumTableName) should be(true)
    } catch {
      case t: Throwable => {
        t.printStackTrace()
      }
    } finally {
      hAdmin.disableTable(forumTableName)
      hAdmin.deleteTable(forumTableName)
      hAdmin.close()
      connection.close()
    }
  }
}