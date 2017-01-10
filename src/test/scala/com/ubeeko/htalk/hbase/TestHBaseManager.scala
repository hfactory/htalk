package com.ubeeko.htalk.hbase

import java.util.Date

import com.ubeeko.htalk.bytesconv._
import org.apache.hadoop.hbase.HTableDescriptor
import org.apache.hadoop.hbase.client.HTableInterface
import org.apache.hadoop.hbase.TableExistsException
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.HTableInterfaceFactory
import org.apache.hadoop.conf.Configuration
import com.ubeeko.exceptions.NotImplementedOperation
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.Cell
import org.apache.hadoop.hbase.client.Row
import org.apache.hadoop.hbase.client.coprocessor.Batch
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.client.ResultScanner
import org.apache.hadoop.hbase.client.Put
import java.io.IOException
import org.apache.hadoop.hbase.client.Delete
import org.apache.hadoop.hbase.client.RowMutations
import org.apache.hadoop.hbase.client.Append
import org.apache.hadoop.hbase.client.Increment
import org.apache.hadoop.hbase.client.Durability
import org.apache.hadoop.classification.InterfaceAudience
import org.apache.hadoop.hbase.ipc.CoprocessorRpcChannel
import org.apache.hadoop.hbase.CellUtil
import com.google.protobuf.Service
import org.scalatest.Matchers
import com.ubeeko.htalk.criteria.Family
import org.apache.hadoop.hbase.client.Table
import org.apache.hadoop.hbase.client.BufferedMutator

case class TestTable(desc: HTableDescriptor, var rows: Map[IndexedSeq[Byte], List[Cell]], var enabled: Boolean = true) extends TestHTable

trait TestHBaseManager extends HBaseManager {
  protected var tables: Map[TableName, TestTable]

  def hbaseBackendVersion: String = "FakeHBase"

  def getRawTestTables: Map[TableName, TestTable] = tables

  def getTableDescriptors(tableNames: Seq[TableName]): Array[HTableDescriptor] = {
    tableNames.map(name => tables(name).desc).toArray
  }

  def isTableEnabled(name: TableName): Boolean = tables.get(name).map(_.enabled).getOrElse(false)

  def createTable(name: TableName,
                   families: Seq[String] = Seq(Family.Default.value),
                   ignoreExisting: Boolean = false): Unit = {
    if (!tableExists(name)) {
      val desc = new HTableDescriptor(name)
      families.foreach(fam => desc.addFamily(new HColumnDescriptor(fam)))
      tables += name -> TestTable(desc, Map())
    } else if (!ignoreExisting) throw new TableExistsException(name)
  }

  def deleteTable(name: TableName): Unit = {
    tables = tables - name
  }

  def getTable(name: TableName): Table = {
    tables.get(name).getOrElse(null)
  }

  private def getMutator(table: Table): BufferedMutator = new BufferedMutator {
    import scala.collection.JavaConversions._
    def close(): Unit = table.close() 
    def flush(): Unit = {}
    def getConfiguration(): org.apache.hadoop.conf.Configuration = table.getConfiguration
    def getName(): org.apache.hadoop.hbase.TableName = table.getName
    def getWriteBufferSize(): Long = throw NotImplementedOperation("getWriteBufferSize")
    def mutate(mutations: java.util.List[_ <: org.apache.hadoop.hbase.client.Mutation]): Unit = mutations.foreach(mutate)
    def mutate(mutation: org.apache.hadoop.hbase.client.Mutation): Unit = mutation match {
      case putOp: Put => table.put(putOp)
      case deleteOp: Delete => table.delete(deleteOp)
      case _: Append => throw NotImplementedOperation("Mutate with Append")
      case _: Increment => throw NotImplementedOperation("Mutate with Increment")
    }
  }

  def getBufferedMutator(name: TableName): BufferedMutator = {
    tables.get(name).map(getMutator).getOrElse(null)
  }

  def tableExists(name: TableName): Boolean = tables.exists(_._1 == name)

  def close(): Unit =
    tables = Map()
}

object TestHBaseManager extends Matchers {
  private def stringFromCell(cell: org.apache.hadoop.hbase.Cell, valueGetter: org.apache.hadoop.hbase.Cell => Array[Byte]): String =
    BytesConv.StringBytesConv(valueGetter(cell))

  // XXX Should be written with implicit
  def row(cell: org.apache.hadoop.hbase.Cell): String = stringFromCell(cell, CellUtil.cloneRow)
  def qualifier(cell: org.apache.hadoop.hbase.Cell): String = stringFromCell(cell, CellUtil.cloneQualifier)
  def family(cell: org.apache.hadoop.hbase.Cell): String = stringFromCell(cell, CellUtil.cloneFamily)
  def cellValue(cell: org.apache.hadoop.hbase.Cell): String = stringFromCell(cell, CellUtil.cloneValue)

  def typedCellValue[T](cell: org.apache.hadoop.hbase.Cell)(implicit conv: BytesConv[T]): T =
    conv(CellUtil.cloneValue(cell))

  def assertTableAndGetRows(htalkContext: HTalkContext, table: String, rowNumber: Int): Map[String, List[org.apache.hadoop.hbase.Cell]] = {
    val hbaseManager = htalkContext.hbaseManager.asInstanceOf[TestHBaseManager]
    val tableName = htalkContext.tableName(table)
    val tables = hbaseManager.getRawTestTables
    tables.keySet should contain(tableName)
    tables(tableName).rows should have size rowNumber
    tables(tableName).rows.map {
      case (rowId, cells) => (BytesConv.StringBytesConv(rowId.toArray), cells)
    }
  }

  def emptyInstance = new TestHBaseManager {
    protected var tables = Map.empty[TableName, TestTable]
  }
}

trait TestHTable extends Table {
  import scala.collection.JavaConversions._
  val desc: HTableDescriptor
  var rows: Map[IndexedSeq[Byte], List[Cell]]
  var enabled: Boolean

  def checkAndDelete(x$1: Array[Byte],x$2: Array[Byte],x$3: Array[Byte],x$4: org.apache.hadoop.hbase.filter.CompareFilter.CompareOp,x$5: Array[Byte],x$6: org.apache.hadoop.hbase.client.Delete): Boolean = ???
  def checkAndPut(x$1: Array[Byte],x$2: Array[Byte],x$3: Array[Byte],x$4: org.apache.hadoop.hbase.filter.CompareFilter.CompareOp,x$5: Array[Byte],x$6: org.apache.hadoop.hbase.client.Put): Boolean = ???
  def existsAll(x$1: java.util.List[org.apache.hadoop.hbase.client.Get]): Array[Boolean] = ???

  def hasFamily(family: Array[Byte]): Boolean = desc.hasFamily(family)

  def getRow(row: Array[Byte]) = rows.get(row.toIndexedSeq)
  def getFiltered(row: Array[Byte], family: Array[Byte], column: Option[Array[Byte]]) = {
    getRow(row).map {
      _.filter(cell => CellUtil.cloneFamily(cell).deep == family.deep &&
          column.map(CellUtil.cloneQualifier(cell).deep == _.deep).getOrElse(true))
    }.getOrElse(List())
  }

  def scanFilter(row: List[Cell], f: Cell => Boolean) = {
    row.filter(f)
  }
  def scanFamily(row: List[Cell], family: Array[Byte]) = scanFilter(row, cell => CellUtil.cloneFamily(cell).deep == family.deep)

  def scanColumn(row: List[Cell], family: Array[Byte], column: Array[Byte]) = scanFilter(row,
      cell => CellUtil.cloneFamily(cell).deep == family.deep && CellUtil.cloneQualifier(cell).deep == column.deep)

  def deleteRow(row: IndexedSeq[Byte]): Unit = rows -= row

  def delete(row: IndexedSeq[Byte], f: Cell => Boolean): Unit = {
    val results = rows.get(row).map {
      cells =>
        cells.filter(f)
    }.getOrElse(List())
  }

  def deleteFamily(row: IndexedSeq[Byte], family: Array[Byte]) = {
    val fam = family.deep
    delete(row, CellUtil.cloneFamily(_).deep == fam)
  }

  def deleteColumn(row: IndexedSeq[Byte], family: Array[Byte], qualifier: Array[Byte]) = {
    val fam = family.deep
    val qua = qualifier.deep
    delete(row, cell => CellUtil.cloneFamily(cell).deep == fam && CellUtil.cloneQualifier(cell).deep == qua)
  }

  /**
   * Gets the fully qualified table name instance of this table.
   */
  def getName: TableName = desc.getTableName

  /**
   * Returns the {@link Configuration} object used by this instance.
   * <p>
   * The reference returned is not a copy, so any change made to it will
   * affect this instance.
   */
  def getConfiguration: Configuration = throw NotImplementedOperation("getConfiguration not implemented in the Test implementation")

  /**
   * Gets the {@link HTableDescriptor table descriptor} for this table.
   * @throws IOException if a remote or network exception occurs.
   */
  def getTableDescriptor: HTableDescriptor = desc

  /**
   * Test for the existence of columns in the table, as specified by the Get.
   * <p>
   *
   * This will return true if the Get matches one or more keys, false if not.
   * <p>
   *
   * This is a server-side call so it prevents any data from being transfered to
   * the client.
   *
   * @param get the Get
   * @return true if the specified Get matches one or more keys, false if not
   * @throws IOException e
   */
  def exists(get: Get): Boolean = {
    get.getFamilyMap().exists{
      case (family, List()) => 
        hasFamily(family) && !getFiltered(get.getRow(), family, None).isEmpty
      case (family, columns) => 
        hasFamily(family) && columns.exists(column => !getFiltered(get.getRow(), family, Some(column)).isEmpty)
    }
  }

  /**
   * Test for the existence of columns in the table, as specified by the Gets.
   * <p>
   *
   * This will return an array of booleans. Each value will be true if the related Get matches
   * one or more keys, false if not.
   * <p>
   *
   * This is a server-side call so it prevents any data from being transfered to
   * the client.
   *
   * @param gets the Gets
   * @return Array of Boolean true if the specified Get matches one or more keys, false if not
   * @throws IOException e
   */
  def exists(gets: java.util.List[Get]): Array[java.lang.Boolean] = gets.map(get => new java.lang.Boolean(exists(get))).toArray

  /**
   * Method that does a batch call on Deletes, Gets, Puts, Increments, Appends and RowMutations.
   * The ordering of execution of the actions is not defined. Meaning if you do a Put and a
   * Get in the same {@link #batch} call, you will not necessarily be
   * guaranteed that the Get returns what the Put had put.
   *
   * @param actions list of Get, Put, Delete, Increment, Append, RowMutations objects
   * @param results Empty Object[], same size as actions. Provides access to partial
   *                results, in case an exception is thrown. A null in the result array means that
   *                the call for that action failed, even after retries
   * @throws IOException
   * @since 0.90.0
   */
  def batch(actions: java.util.List[_ <: Row], results: Array[Object]): Unit = throw NotImplementedOperation("batch not implemented in the Test implementation")

  /**
   * Same as {@link #batch(List, Object[])}, but returns an array of
   * results instead of using a results parameter reference.
   *
   * @param actions list of Get, Put, Delete, Increment, Append, RowMutations objects
   * @return the results from the actions. A null in the return array means that
   *         the call for that action failed, even after retries
   * @throws IOException
   * @since 0.90.0
   */
  def batch(actions: java.util.List[_ <: Row]): Array[Object] = throw NotImplementedOperation("batch not implemented in the Test implementation")

  /**
   * Same as {@link #batch(List, Object[])}, but with a callback.
   * @since 0.96.0
   */
  def batchCallback[R](
    actions: java.util.List[_ <: Row], results: Array[Object], callback: Batch.Callback[R] 
  ): Unit = throw NotImplementedOperation("batchCallback not implemented in the Test implementation")


  /**
   * Same as {@link #batch(List)}, but with a callback.
   * @since 0.96.0
   */
  def batchCallback[R](
    actions: java.util.List[_ <: Row], callback: Batch.Callback[R]
  ): Array[Object] = throw NotImplementedOperation("batchCallback not implemented in the Test implementation")

  /**
   * Extracts certain cells from a given row.
   * @param get The object that specifies what data to fetch and from which row.
   * @return The data coming from the specified row, if it exists.  If the row
   * specified doesn't exist, the {@link Result} instance returned won't
   * contain any {@link KeyValue}, as indicated by {@link Result#isEmpty()}.
   * @throws IOException if a remote or network exception occurs.
   * @since 0.20.0
   */
  def get(get: Get): Result = {
    val familyMap = get.getFamilyMap()
    val rowkey = get.getRow
    val cells = if (familyMap.isEmpty) {
      getRow(rowkey).getOrElse(List())
    } else {
      familyMap.flatMap {
        case (family, null) => getFiltered(rowkey, family, None)
        case (family, columns) => columns.flatMap(column => getFiltered(get.getRow, family, Some(column)))
      }
    }
    Result.create(cells.toSeq)
  }

  /**
   * Extracts certain cells from the given rows, in batch.
   *
   * @param gets The objects that specify what data to fetch and from which rows.
   *
   * @return The data coming from the specified rows, if it exists.  If the row
   *         specified doesn't exist, the {@link Result} instance returned won't
   *         contain any {@link KeyValue}, as indicated by {@link Result#isEmpty()}.
   *         If there are any failures even after retries, there will be a null in
   *         the results array for those Gets, AND an exception will be thrown.
   * @throws IOException if a remote or network exception occurs.
   *
   * @since 0.90.0
   */
  def get(gets: java.util.List[Get]): Array[Result] = gets.map(get).toArray

  /**
   * Return the row that matches <i>row</i> exactly,
   * or the one that immediately precedes it.
   *
   * @param row A row key.
   * @param family Column family to include in the {@link Result}.
   * @throws IOException if a remote or network exception occurs.
   * @since 0.20.0
   * 
   * @deprecated As of version 0.92 this method is deprecated without
   * replacement.   
   * getRowOrBefore is used internally to find entries in hbase:meta and makes
   * various assumptions about the table (which are true for hbase:meta but not
   * in general) to be efficient.
   */
  def getRowOrBefore(row: Array[Byte], family: Array[Byte]): Result = throw NotImplementedOperation("getRowOrBefore not implemented in the Test implementation")

  /**
   * Returns a scanner on the current table as specified by the {@link Scan}
   * object.
   * Note that the passed {@link Scan}'s start row and caching properties
   * maybe changed.
   *
   * @param scan A configured {@link Scan} object.
   * @return A scanner.
   * @throws IOException if a remote or network exception occurs.
   * @since 0.20.0
   */
   def getScanner(scan: Scan): ResultScanner = {
    import org.apache.hadoop.hbase.util.Bytes
    val familyMap = scan.getFamilyMap
    val results = if (familyMap.isEmpty) {
      rows.values
    } else {
      rows.values.map {row =>
        familyMap.flatMap {
          case (family, null) => scanFamily(row, family)
          case (family, columns) => columns.flatMap(column => scanColumn(row, family, column))
        }.toList
      }.filterNot(_.isEmpty)
    }.filter(row => {
      val firstCell = row.head
      val rowkey = firstCell.getRowArray.drop(firstCell.getRowOffset).take(firstCell.getRowLength)
      val startRow = Option(scan.getStartRow)
      val comp = Bytes.compareTo(rowkey, startRow.getOrElse(Array.empty))
      comp > 0
    })
    new ResultScanner {
      import scala.collection.JavaConversions.asJavaIterator
      protected var iterator: java.util.Iterator[Result] = results.iterator.map(Result.create(_))
      def next: Result = {
        if (iterator.hasNext) iterator.next
        else null
      }
      def next(nbRows: Int) = {
        iterator.take(nbRows).toArray
      }
      def close: Unit = iterator = Iterator.empty 
    }
  }

  /**
   * Gets a scanner on the current table for the given family.
   *
   * @param family The column family to scan.
   * @return A scanner.
   * @throws IOException if a remote or network exception occurs.
   * @since 0.20.0
   */
  def getScanner(family: Array[Byte]): ResultScanner = {
    getScanner(new Scan().addFamily(family))
  }

  /**
   * Gets a scanner on the current table for the given family and qualifier.
   *
   * @param family The column family to scan.
   * @param qualifier The column qualifier to scan.
   * @return A scanner.
   * @throws IOException if a remote or network exception occurs.
   * @since 0.20.0
   */
  def getScanner(family: Array[Byte], qualifier: Array[Byte]): ResultScanner = {
    getScanner(new Scan().addColumn(family,qualifier))
  }


  /**
   * Puts some data in the table.
   * <p>
   * If {@link #isAutoFlush isAutoFlush} is false, the update is buffered
   * until the internal buffer is full.
   * @param put The data to put.
   * @throws IOException if a remote or network exception occurs.
   * @since 0.20.0
   */
  def put(put: Put): Unit = {
    val cells = put.getFamilyCellMap().flatMap {
      case (family, cells) =>
        if (desc.hasFamily(family)) cells
        else throw new IOException("Family not defined")
    }
    if (!cells.isEmpty) {
      val row = put.getRow.toIndexedSeq
      rows = rows + (row -> (rows.getOrElse(row, List()) ++ cells))
    }
  }

  /**
   * Puts some data in the table, in batch.
   * <p>
   * If {@link #isAutoFlush isAutoFlush} is false, the update is buffered
   * until the internal buffer is full.
   * <p>
   * This can be used for group commit, or for submitting user defined
   * batches.  The writeBuffer will be periodically inspected while the List
   * is processed, so depending on the List size the writeBuffer may flush
   * not at all, or more than once.
   * @param puts The list of mutations to apply. The batch put is done by
   * aggregating the iteration of the Puts over the write buffer
   * at the client-side for a single RPC call.
   * @throws IOException if a remote or network exception occurs.
   * @since 0.20.0
   */
  def put(puts: java.util.List[Put]): Unit = puts.foreach(put)

  private def checkAnd(row: Array[Byte], family: Array[Byte], qualifier: Array[Byte],
      value: Array[Byte], f: => Unit): Boolean = {
    val ok = (getFiltered(row, family, Some(qualifier)), value) match {
      case (List(), null) => true
      case (_, null) => false
      case (list, _) => CellUtil.cloneValue(list.maxBy(_.getTimestamp)).deep == value.deep
    }
    if (ok) f
    ok
 
  }
  /**
   * Atomically checks if a row/family/qualifier value matches the expected
   * value. If it does, it adds the put.  If the passed value is null, the check
   * is for the lack of column (ie: non-existance)
   *
   * @param row to check
   * @param family column family to check
   * @param qualifier column qualifier to check
   * @param value the expected value
   * @param put data to put if check succeeds
   * @throws IOException e
   * @return true if the new put was executed, false otherwise
   */
  def checkAndPut(row: Array[Byte], family: Array[Byte], qualifier: Array[Byte],
      value: Array[Byte], putData: Put): Boolean =
        checkAnd(row, family, qualifier, value, put(putData))

  /**
   * Deletes the specified cells/row.
   *
   * @param delete The object that specifies what to delete.
   * @throws IOException if a remote or network exception occurs.
   * @since 0.20.0
   */
  def delete(delete: Delete): Unit = {
    val familyMap = delete.getFamilyCellMap()
    val row = delete.getRow.toIndexedSeq
    if (familyMap.isEmpty) deleteRow(row)
    else familyMap.foreach {
      case (family, null) => deleteFamily(row, family)
      case (family, list) if list.isEmpty() => deleteFamily(row, family)
      case (family, cells) => cells.foreach(cell => deleteColumn(row, family, CellUtil.cloneQualifier(cell)))
    }
  }

  /**
   * Deletes the specified cells/rows in bulk.
   * @param deletes List of things to delete.  List gets modified by this
   * method (in particular it gets re-ordered, so the order in which the elements
   * are inserted in the list gives no guarantee as to the order in which the
   * {@link Delete}s are executed).
   * @throws IOException if a remote or network exception occurs. In that case
   * the {@code deletes} argument will contain the {@link Delete} instances
   * that have not be successfully applied.
   * @since 0.20.1
   */
  def delete(deletes: java.util.List[Delete]): Unit =
    deletes.foreach(delete)

  /**
   * Atomically checks if a row/family/qualifier value matches the expected
   * value. If it does, it adds the delete.  If the passed value is null, the
   * check is for the lack of column (ie: non-existance)
   *
   * @param row to check
   * @param family column family to check
   * @param qualifier column qualifier to check
   * @param value the expected value
   * @param deleteData data to delete if check succeeds
   * @throws IOException e
   * @return true if the new delete was executed, false otherwise
   */
  def checkAndDelete(row: Array[Byte], family: Array[Byte], qualifier: Array[Byte],
      value: Array[Byte], deleteData: Delete): Boolean =
        checkAnd(row, family, qualifier, value, delete(deleteData))

  /**
   * Performs multiple mutations atomically on a single row. Currently
   * {@link Put} and {@link Delete} are supported.
   *
   * @param rm object that specifies the set of mutations to perform atomically
   * @throws IOException
   */
  def mutateRow(rm: RowMutations): Unit = ???

  /**
   * Appends values to one or more columns within a single row.
   * <p>
   * This operation does not appear atomic to readers.  Appends are done
   * under a single row lock, so write operations to a row are synchronized, but
   * readers do not take row locks so get and scan operations can see this
   * operation partially completed.
   *
   * @param append object that specifies the columns and amounts to be used
   *                  for the increment operations
   * @throws IOException e
   * @return values of columns after the append operation (maybe null)
   */
  def append(append: Append): Result = ???

  /**
   * Increments one or more columns within a single row.
   * <p>
   * This operation does not appear atomic to readers.  Increments are done
   * under a single row lock, so write operations to a row are synchronized, but
   * readers do not take row locks so get and scan operations can see this
   * operation partially completed.
   *
   * @param increment object that specifies the columns and amounts to be used
   *                  for the increment operations
   * @throws IOException e
   * @return values of columns after the increment
   */
  def increment(increment: Increment): Result = ???

  /**
   * See {@link #incrementColumnValue(byte[], byte[], byte[], long, Durability)}
   * <p>
   * The {@link Durability} is defaulted to {@link Durability#SYNC_WAL}.
   * @param row The row that contains the cell to increment.
   * @param family The column family of the cell to increment.
   * @param qualifier The column qualifier of the cell to increment.
   * @param amount The amount to increment the cell with (or decrement, if the
   * amount is negative).
   * @return The new value, post increment.
   * @throws IOException if a remote or network exception occurs.
   */
  def incrementColumnValue(row: Array[Byte], family: Array[Byte], qualifier: Array[Byte],
      amount: Long): Long = ???

  /**
   * Atomically increments a column value. If the column value already exists
   * and is not a big-endian long, this could throw an exception. If the column
   * value does not yet exist it is initialized to <code>amount</code> and
   * written to the specified column.
   *
   * <p>Setting durability to {@link Durability#SKIP_WAL} means that in a fail
   * scenario you will lose any increments that have not been flushed.
   * @param row The row that contains the cell to increment.
   * @param family The column family of the cell to increment.
   * @param qualifier The column qualifier of the cell to increment.
   * @param amount The amount to increment the cell with (or decrement, if the
   * amount is negative).
   * @param durability The persistence guarantee for this increment.
   * @return The new value, post increment.
   * @throws IOException if a remote or network exception occurs.
   */
  def incrementColumnValue(row: Array[Byte], family: Array[Byte], qualifier: Array[Byte],
      amount: Long, durability: Durability): Long = ???

  /**
   * @deprecated Use {@link #incrementColumnValue(byte[], byte[], byte[], long, Durability)}
   */
  @Deprecated
  def incrementColumnValue(row: Array[Byte], family: Array[Byte], qualifier: Array[Byte],
      amount: Long, writeToWAL: Boolean) : Long = ???

  /**
   * Tells whether or not 'auto-flush' is turned on.
   *
   * @return {@code true} if 'auto-flush' is enabled (default), meaning
   * {@link Put} operations don't get buffered/delayed and are immediately
   * executed.
   */
  def isAutoFlush(): Boolean = true

  /**
   * Executes all the buffered {@link Put} operations.
   * <p>
   * This method gets called once automatically for every {@link Put} or batch
   * of {@link Put}s (when <code>put(List<Put>)</code> is used) when
   * {@link #isAutoFlush} is {@code true}.
   * @throws IOException if a remote or network exception occurs.
   */
  def flushCommits(): Unit = {}

  /**
   * Releases any resources held or pending changes in internal buffers.
   *
   * @throws IOException if a remote or network exception occurs.
   */
  def close(): Unit = {}

  /**
   * Creates and returns a {@link com.google.protobuf.RpcChannel} instance connected to the
   * table region containing the specified row.  The row given does not actually have
   * to exist.  Whichever region would contain the row based on start and end keys will
   * be used.  Note that the {@code row} parameter is also not passed to the
   * coprocessor handler registered for this protocol, unless the {@code row}
   * is separately passed as an argument in the service request.  The parameter
   * here is only used to locate the region used to handle the call.
   *
   * <p>
   * The obtained {@link com.google.protobuf.RpcChannel} instance can be used to access a published
   * coprocessor {@link com.google.protobuf.Service} using standard protobuf service invocations:
   * </p>
   *
   * <div style="background-color: #cccccc; padding: 2px">
   * <blockquote><pre>
   * CoprocessorRpcChannel channel = myTable.coprocessorService(rowkey);
   * MyService.BlockingInterface service = MyService.newBlockingStub(channel);
   * MyCallRequest request = MyCallRequest.newBuilder()
   *     ...
   *     .build();
   * MyCallResponse response = service.myCall(null, request);
   * </pre></blockquote></div>
   *
   * @param row The row key used to identify the remote region location
   * @return A CoprocessorRpcChannel instance
   */
  @InterfaceAudience.Private // TODO add coproc audience level  
  def coprocessorService(row: Array[Byte]): CoprocessorRpcChannel = ???

  /**
   * Creates an instance of the given {@link com.google.protobuf.Service} subclass for each table
   * region spanning the range from the {@code startKey} row to {@code endKey} row (inclusive),
   * and invokes the passed {@link org.apache.hadoop.hbase.client.coprocessor.Batch.Call#call}
   * method with each {@link Service}
   * instance.
   *
   * @param service the protocol buffer {@code Service} implementation to call
   * @param startKey start region selection with region containing this row.  If {@code null}, the
   *                 selection will start with the first table region.
   * @param endKey select regions up to and including the region containing this row.
   *               If {@code null}, selection will continue through the last table region.
   * @param callable this instance's
   *                 {@link org.apache.hadoop.hbase.client.coprocessor.Batch.Call#call}
   *                 method will be invoked once per table region, using the {@link Service}
   *                 instance connected to that region.
   * @param <T> the {@link Service} subclass to connect to
   * @param <R> Return type for the {@code callable} parameter's
   * {@link org.apache.hadoop.hbase.client.coprocessor.Batch.Call#call} method
   * @return a map of result values keyed by region name
   */
  @InterfaceAudience.Private // TODO add coproc audience level
  def coprocessorService[T <: Service, R](service: Class[T],
      startKey: Array[Byte], endKey: Array[Byte], callable: Batch.Call[T,R]): java.util.Map[Array[Byte],R] = ???

  /**
   * Creates an instance of the given {@link com.google.protobuf.Service} subclass for each table
   * region spanning the range from the {@code startKey} row to {@code endKey} row (inclusive),
   * and invokes the passed {@link org.apache.hadoop.hbase.client.coprocessor.Batch.Call#call}
   * method with each {@link Service} instance.
   *
   * <p>
   * The given
   * {@link org.apache.hadoop.hbase.client.coprocessor.Batch.Callback#update(byte[], byte[], Object)}
   * method will be called with the return value from each region's
   * {@link org.apache.hadoop.hbase.client.coprocessor.Batch.Call#call} invocation.
   *</p>
   *
   * @param service the protocol buffer {@code Service} implementation to call
   * @param startKey start region selection with region containing this row.  If {@code null}, the
   *                 selection will start with the first table region.
   * @param endKey select regions up to and including the region containing this row.
   *               If {@code null}, selection will continue through the last table region.
   * @param callable this instance's
   *                 {@link org.apache.hadoop.hbase.client.coprocessor.Batch.Call#call} method
   *                 will be invoked once per table region, using the {@link Service} instance
   *                 connected to that region.
   * @param callback
   * @param <T> the {@link Service} subclass to connect to
   * @param <R> Return type for the {@code callable} parameter's
   * {@link org.apache.hadoop.hbase.client.coprocessor.Batch.Call#call} method
   */
  @InterfaceAudience.Private // TODO add coproc audience level
  def coprocessorService[T <: Service, R](service: Class[T],
      startKey: Array[Byte], endKey: Array[Byte], callable: Batch.Call[T,R],
      callback: Batch.Callback[R]): Unit = ???

  /**
   * See {@link #setAutoFlush(boolean, boolean)}
   *
   * @param autoFlush
   *          Whether or not to enable 'auto-flush'.
   * @deprecated in 0.96. When called with setAutoFlush(false), this function also
   *  set clearBufferOnFail to true, which is unexpected but kept for historical reasons.
   *  Replace it with setAutoFlush(false, false) if this is exactly what you want, or by
   *  {@link #setAutoFlushTo(boolean)} for all other cases.
   */
  @Deprecated
  def setAutoFlush(autoFlush: Boolean): Unit = ???

  /**
   * Turns 'auto-flush' on or off.
   * <p>
   * When enabled (default), {@link Put} operations don't get buffered/delayed
   * and are immediately executed. Failed operations are not retried. This is
   * slower but safer.
   * <p>
   * Turning off {@code #autoFlush} means that multiple {@link Put}s will be
   * accepted before any RPC is actually sent to do the write operations. If the
   * application dies before pending writes get flushed to HBase, data will be
   * lost.
   * <p>
   * When you turn {@code #autoFlush} off, you should also consider the
   * {@code #clearBufferOnFail} option. By default, asynchronous {@link Put}
   * requests will be retried on failure until successful. However, this can
   * pollute the writeBuffer and slow down batching performance. Additionally,
   * you may want to issue a number of Put requests and call
   * {@link #flushCommits()} as a barrier. In both use cases, consider setting
   * clearBufferOnFail to true to erase the buffer after {@link #flushCommits()}
   * has been called, regardless of success.
   * <p>
   * In other words, if you call {@code #setAutoFlush(false)}; HBase will retry N time for each
   *  flushCommit, including the last one when closing the table. This is NOT recommended,
   *  most of the time you want to call {@code #setAutoFlush(false, true)}.
   *
   * @param autoFlush
   *          Whether or not to enable 'auto-flush'.
   * @param clearBufferOnFail
   *          Whether to keep Put failures in the writeBuffer. If autoFlush is true, then
   *          the value of this parameter is ignored and clearBufferOnFail is set to true.
   *          Setting clearBufferOnFail to false is deprecated since 0.96.
   * @see #flushCommits
   */
  def setAutoFlush(autoFlush: Boolean, clearBufferOnFail: Boolean): Unit = {
    // Doesn't do anything. Autoflush is always true in this implementation.
  }

  /**
   * Set the autoFlush behavior, without changing the value of {@code clearBufferOnFail}
   */
  def setAutoFlushTo(autoFlush: Boolean): Unit = {
    // Doesn't do anything. Autoflush is always true in this implementation.
  }

  /**
   * Returns the maximum size in bytes of the write buffer for this HTable.
   * <p>
   * The default value comes from the configuration parameter
   * {@code hbase.client.write.buffer}.
   * @return The size of the write buffer in bytes.
   */
  def getWriteBufferSize(): Long = 4096L * 1024L

  /**
   * Sets the size of the buffer in bytes.
   * <p>
   * If the new size is less than the current amount of data in the
   * write buffer, the buffer gets flushed.
   * @param writeBufferSize The new write buffer size, in bytes.
   * @throws IOException if a remote or network exception occurs.
   */
  def setWriteBufferSize(writeBufferSize: Long) = ???

  /* New method in 0.98.10.1 */
  def batchCoprocessorService[R <: com.google.protobuf.Message](x$1: com.google.protobuf.Descriptors.MethodDescriptor,x$2: com.google.protobuf.Message,x$3: Array[Byte],x$4: Array[Byte],x$5: R,x$6: org.apache.hadoop.hbase.client.coprocessor.Batch.Callback[R]): Unit = ???

  /* New method in 0.98.10.1 */
  def batchCoprocessorService[R <: com.google.protobuf.Message](x$1: com.google.protobuf.Descriptors.MethodDescriptor,x$2: com.google.protobuf.Message,x$3: Array[Byte],x$4: Array[Byte],x$5: R): java.util.Map[Array[Byte],R] = ???

  /* New method in 0.98.10.1 */
  def checkAndMutate(x$1: Array[Byte],x$2: Array[Byte],x$3: Array[Byte],x$4: org.apache.hadoop.hbase.filter.CompareFilter.CompareOp,x$5: Array[Byte],x$6: org.apache.hadoop.hbase.client.RowMutations): Boolean = ???
}
