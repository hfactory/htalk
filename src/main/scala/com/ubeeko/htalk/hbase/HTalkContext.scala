package com.ubeeko.htalk.hbase

import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.client.{BufferedMutator, Table}

case class HTalkOptions(prefix: Option[String] = None)

class HTalkContext(val hbaseManager: HBaseManager, val options: Option[HTalkOptions])
    extends AutoCloseable {
  final def tableName(name: String) = {
    TableName.valueOf(options.flatMap(_.prefix).getOrElse("") + name)
  }

  def getTableDescriptors(tableNames: Seq[String]): Array[HTableDescriptor] =
    hbaseManager.getTableDescriptors(tableNames.map(tableName))

  def isTableEnabled(name: String): Boolean =
    hbaseManager.isTableEnabled(tableName(name))

  def createTable(name: String,
                   families: Seq[String] = Seq(HBaseManager.defaultFamilyName),
                   ignoreExisting: Boolean = false): Unit =
    hbaseManager.createTable(tableName(name), families, ignoreExisting)

  def deleteTable(name: String): Unit =
    hbaseManager.deleteTable(tableName(name))

  def getTable(name: String): Table =
    hbaseManager.getTable(tableName(name))

  def getBufferedMutator(name: String): BufferedMutator =
    hbaseManager.getBufferedMutator(tableName(name))

  def tableExists(name: String): Boolean =
    hbaseManager.tableExists(tableName(name))

  /** Closes the context.
    *
    * The context must not be used after it's been closed.
    * Doing would result in an undetermined behaviour.
    *
    * If a derived class overrides this method, it *must* call super.
    */
  def close(): Unit =
    hbaseManager.close()
}

object HTalkContext {
  def apply(hbaseManager: HBaseManager,
            options: Option[HTalkOptions] = None) =
    new HTalkContext(hbaseManager, options)
}
