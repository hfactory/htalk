package com.ubeeko.htalk.hbase

import com.typesafe.scalalogging.slf4j.Logging
import com.ubeeko.htalk.criteria.Family
import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.client.{BufferedMutator, ConnectionFactory, Table}
import org.apache.hadoop.conf.Configuration
import java.io.{InputStream, FileInputStream}
import java.util.Properties
import scala.collection.JavaConversions._
import scala.util.control.NonFatal
import scala.util.{Try, Success, Failure}

trait HBaseManager extends AutoCloseable {
  def hbaseBackendVersion: String

  final def connectedToCloudBigTable: Boolean = {
    // FIXME: No idea if this test will work; have to actually perform a test
    // on a Cloud BigTable instance!
    hbaseBackendVersion.contains("BigTable")
  }

  def getTableDescriptors(tableNames: Seq[TableName]): Array[HTableDescriptor]
  def isTableEnabled(name: TableName): Boolean
  def createTable(name: TableName,
                   families: Seq[String] = Seq(HBaseManager.defaultFamilyName),
                   ignoreExisting: Boolean = false): Unit
  def deleteTable(name: TableName): Unit
  def getTable(name: TableName): Table
  def getBufferedMutator(name: TableName): BufferedMutator
  def tableExists(name: TableName): Boolean

  def close(): Unit
}

class HBaseManagerInstance(config: Configuration)
    extends HBaseManager {

  private val connection = ConnectionFactory.createConnection(config)

  final def hbaseBackendVersion: String = connection.getAdmin.getClusterStatus.getHBaseVersion

  def getTableDescriptors(tableNames: Seq[TableName]): Array[HTableDescriptor] = {
    val hbaseAdmin = connection.getAdmin
    try {
      Try(hbaseAdmin.getTableDescriptorsByTableName(tableNames)) getOrElse {
        tableNames.map(name => hbaseAdmin.getTableDescriptor(name)).toArray
      }
    } finally {
      hbaseAdmin.close()
    }
  }

  def isTableEnabled(name: TableName): Boolean = {
    val hbaseAdmin = connection.getAdmin
    try {
      hbaseAdmin.isTableEnabled(name)
    } finally {
      hbaseAdmin.close()
    }
  }

  def createTable(name: TableName,
                   families: Seq[String] = Seq(Family.Default.value),
                   ignoreExisting: Boolean = false): Unit = {
    val hbaseAdmin = connection.getAdmin
    val tableDesc = new HTableDescriptor(name)
    families foreach { f =>
      tableDesc.addFamily(new HColumnDescriptor(f))
    }
    try {
      hbaseAdmin.createTable(tableDesc)
    } catch {
      case e: TableExistsException => if (!ignoreExisting) throw e
      case NonFatal(e) => throw e
    } finally {
      hbaseAdmin.close()
    }
  }

  def deleteTable(name: TableName): Unit = {
    val hbaseAdmin = connection.getAdmin
    try {
      val table = name
      hbaseAdmin.disableTable(table)
      hbaseAdmin.deleteTable(table)
    } finally {
      hbaseAdmin.close()
    }
  }

  def getTable(name: TableName): Table = {
    connection.getTable(name)
  }

  def getBufferedMutator(name: TableName): BufferedMutator = {
    connection.getBufferedMutator(name)
  }

  def tableExists(name: TableName): Boolean = {
    val hbaseAdmin = connection.getAdmin
    try {
      hbaseAdmin.tableExists(name)
    } finally {
      hbaseAdmin.close()
    }
  }

  def close(): Unit = connection.close()
}

object HBaseManager extends Logging {
  import scala.collection.JavaConverters._

  def apply(config: Configuration): HBaseManager =
    new HBaseManagerInstance(config)

  val defaultConfigFileName = "hbase-site.properties"

  val defaultFamilyName = "d"

  /** Attempts to read the specified config first from the filesystem, then from the resources. */
  def getHBaseConfiguration(configFileName: String = defaultConfigFileName,
                            classLoader: Option[ClassLoader] = None): Configuration = {
    def openFsStream(): Try[InputStream] = Try { new FileInputStream(configFileName) }

    def openResourceStream(): Try[InputStream] = {
      val loader = classLoader getOrElse Thread.currentThread.getContextClassLoader
      Option(loader.getResourceAsStream(configFileName)) match {
        case Some(s) => Success(s)
        case None    => Failure(new RuntimeException(s"HBase configuration not found: $configFileName"))
      }
    }

    def readProps(stream: InputStream): Try[Properties] = {
      logger.info(s"Using $configFileName as HBase configuration")
      try {
        val result = new Properties
        result.load(stream)
        Success(result)
      } catch {
        case NonFatal(e) => Failure(e)
      } finally {
        stream.close()
      }
    }

    def makeConfig(props: Properties): Try[Configuration] = Try {
      val config = HBaseConfiguration.create()
      logger.info("HBase configuration:")
      props.asScala foreach { case (k, v) =>
        logger.info("\t%s: %s" format (k, v))
        config.set(k, v)
      }
      config
    }

    ((openFsStream() orElse openResourceStream()) flatMap readProps flatMap makeConfig).get
  }
}
