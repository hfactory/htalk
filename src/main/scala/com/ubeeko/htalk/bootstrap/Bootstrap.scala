package com.ubeeko.htalk.bootstrap

import net.liftweb.json._
import java.net.URL
import scala.collection.JavaConversions._
import com.ubeeko.htalk.hbase._
import com.ubeeko.htalk.criteria._
import java.io.File
import java.util.jar.JarFile
import java.net.URLDecoder
import java.io.FilenameFilter
import org.apache.hadoop.hbase.client.HBaseAdmin
import org.apache.hadoop.hbase.HTableDescriptor
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.TableExistsException
import com.typesafe.scalalogging.slf4j.Logging
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.ConnectionFactory

object Bootstrap extends Logging {
  val configFilePath = "hbase-site.properties"
  val bootstrapFileSuffix = ".hbo"

  /**
   * List directory contents for a resource folder. Not recursive.
   * This is basically a brute-force implementation.
   * Works for regular files and also JARs.
   *
   * @param configFile Any resource that lives in the same place as the resources you want.
   * @param path Should end with "/", but not start with one.
   * @return Just the name of each member item, not the full paths.
   * @throws URISyntaxException
   * @throws IOException
   */
  //TODO put in ubeeko-libs
  private def getResourceListing(configFile: String, path: String): Iterable[String] = {
    val dirURL = getClass.getClassLoader.getResource(path);
    if (dirURL != null && dirURL.getProtocol.equals("file")) {
      /* A file path: easy enough */
      new File(dirURL.toURI()).listFiles(new FilenameFilter {
        def accept(dir: File, name: String) = name.endsWith(bootstrapFileSuffix)
      }).map(_.getAbsolutePath())
    } else if (dirURL == null) {
      /* 
         * In case of a jar file, we can't actually find a directory.
         * Have to assume the same jar as className.
         */
      val jarURL = getClass.getClassLoader.getResource(configFile);
      if (jarURL != null && jarURL.getProtocol.equals("jar")) {
        /* A JAR path */
        val jarPath = jarURL.getPath().substring(5, jarURL.getPath().indexOf("!")); //strip out only the JAR file
        val jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
        val entries = jar.entries(); //gives ALL entries in jar
        entries.filter { entry =>
          val name = entry.getName
          //filter according to the path and keep only json files
          name.startsWith(path) && name.endsWith(bootstrapFileSuffix)
        }.map(_.getName).toSet
      } else throw new UnsupportedOperationException(s"Cannot list files for path '$path' in URL $jarURL")
    } else throw new UnsupportedOperationException("Cannot find the resource folder, nor a jar containing it")

  }

  /**
   * This method is call through reflection by the plugin and must not change the parameters or name
   * without modifying the plugin
   */
  def loadHboFile(jsonFile: String, classLoader: ClassLoader): Unit =
    loadHboFiles(Iterable(jsonFile), true, Option(classLoader))

  def loadHboFiles(jsonFiles    : Iterable[String],
                   createOnly   : Boolean = true,
                   classLoader  : Option[ClassLoader] = None,
                   configuration: Option[Configuration] = None): Unit = {
    val loader = classLoader.getOrElse(Thread.currentThread.getContextClassLoader)
    val config = configuration.getOrElse(HBaseManager.getHBaseConfiguration(configFilePath, Some(loader)))
    implicit val htalkContext = HTalkContext(HBaseManager(config))
    try {
      jsonFiles.foreach(file => loadData(config, file, createOnly, loader))
    } finally {
      htalkContext.close()
    }
  }

  /**
   * @param createOnly if true throw an exception if the table already exists
   */
  private def loadData(config: Configuration, jsonFile: String, createOnly: Boolean, classLoader: ClassLoader)
                      (implicit htalkContext: HTalkContext): Unit = {
    import scala.io
    val dataBuffer = Option(classLoader.getResourceAsStream(jsonFile)) map io.Source.fromInputStream getOrElse {
      io.Source.fromFile(jsonFile, "UTF-8")
    }
    val data = parse(dataBuffer.mkString)

    import JSONData._

    val types = getTypes(data)
    types.foreach {
      case (name, typeName) =>
        fixType(name, typeName)
    }
    val table = getTable(data)
    val hTable = getName(table) match {
      case Some(value: String) => Table(value)
      case _ =>
        throw new UnsupportedOperationException(s"$jsonFile is malformed no table field found")
    }
    val connection = ConnectionFactory.createConnection(config)
    val hBaseAdmin = connection.getAdmin()
    // retrieve column family
    val tableName = TableName.valueOf(hTable.name)
    val desc = new HTableDescriptor(tableName)
    val familiesString = getFamilyNames(table)
    familiesString.foreach(f => desc.addFamily(new HColumnDescriptor(f.value)))
    logger.debug(s"Ready to create $tableName, with families" + familiesString.mkString)
    try {
      hBaseAdmin.createTable(desc)
      if (!hBaseAdmin.tableExists(tableName)) throw new TableExistsException(s"Cannot create table $tableName")
      logger.info(s"Table $tableName created")
    } catch {
      case e: Exception =>
        // Ignore the creation exception if createOnly is false
        if (createOnly) throw e
    } finally {
      hBaseAdmin.close()
    }
    val rows = getRows(table)
    logger.debug("Ready to parse %d rows" format rows.length)
    rows.foreach { row =>
      val rowId = getRowId(row)
      val put = getTimestamp(row) match {
        case Some(ts) => (f: Family, q: Qualifier, v: Array[Byte]) => hTable advancedPut (rowId, f, q, ts, v)
        case None     => (f: Family, q: Qualifier, v: Array[Byte]) => hTable put (rowId, f, q, v)
      }
      val cols = getColumns(row)
      cols.foreach { col =>
        put(getFamily(col), getQualifier(col), getValue(col))
      }
    }
    logger.info("Parsing done")
    logger.info(s"Ready to insert data in table $tableName")

    hTable._put execute

    logger.info(s"Data inserted into table $tableName")
  }

  def main(args: Array[String]): Unit = {
    def printUsage() {
      System.out.println("Executable takes no parameters")
    }
    if (args.length != 0) {
      printUsage()
      sys.exit(1)
    }
    try {
      val jsonFiles = getResourceListing(configFilePath, "HBase/Bootstrap/")
      loadHboFiles(jsonFiles)
    } catch {
      case e: TableExistsException =>
        System.out.println(e.getMessage())
        sys.exit(2)
      case e: UnsupportedOperationException =>
        System.out.println(e.getMessage())
        sys.exit(3)
    }
  }
}
