# Getting started

## Example of data storage

Insert a single cell:

``` scala

      ("user" put("project1", "name", "YourProject")) execute

            
```

Insert a batch in one line:

``` scala

      ("user" put("project1", "name", "YourProject") put("project1", "attribute", "open")) execute

            
```

## Example of retrieval

Define the Entity transformation from the result:

``` scala

      def userFromResult(rs: Result): User = {
        User(rs.getRow[String], rs("name").as[String], rs("age").as[Int])
      }

            
```

Scan all rows and return a list of entities:

``` scala

      ("user" get rows) ~ userFromResult

            
```

Get rows inserted in a time range as entities:

``` scala

      val rangeFilter = new TimeFilter(from, to)

      ("user" get rows filter(rangeFilter)) ~ userFromResult

            
```

Get a specific row:

``` scala

      ("user" get "rowId" qualifiers("name", "age")) ~ userFromResult

            
```

# The language in detail

## Table declaration

The starting point of a query is the table to query against. It can be used explicitely as well as implicitely.

### Explicit declaration

To create a `com.ubeeko.criteria.Table` object and keep its reference we can use a value `myTable`. It will then allow to create `Put`, `Get`, `Scan` or `Delete` queries.

``` scala
      val myTable = new Table("my_table")
                
```

When using the explicit declaration you can change the flushing method to manual to allow batch puts using the following syntax :

``` scala
      myTable.autoFlush(false)
                
```

### Implicit declaration

Using the name of the table with the keywords `put`, `get` or `delete`, you can create the corresponding query.

``` scala
      "my_table" get rowId
                
```

The `rowId` is a `Byte` array or any type with an implicit conversion to `Array[Byte]` like `String`. We will explain the details of the syntax in the following sections. In this mode, the flushing is always done after each insertion.

## Query building

The queries can be built using the chaining approach. All method returns the query built and other methods can be called over it. The methods exists on the `Table` class and the class of the query (`Put`, `Get`, `Scan`, `Delete`)

We will use the value `table` in the example. It can be replaced by your own value in case of explicit declaration, the name of the table if using the implicit one (like `"user"`) or it can be a query object like `batchPut`.

### Put syntax

All the `put` methods return a `Put` object. The basic put method is `put(rowKey, family, qualifier, value)`. `rowKey` and `qualifier` are mandatory, `family` can be ellipsed and `value` is either a direct type `T` or an `Option[T]`. If value is `None` the `put` method returns the `Put` unmodified. An implicit `BytesConv` for the type `T` must exists. The address of the cell can also be defined with the `Cell` class.

``` scala
      table put(rowKey, family, qualifier, value)
      table put(rowKey, family, qualifier, Some(value))
      table put(rowKey, qualifier, value)
      table put(rowKey, qualifier, Some(value))
      batchPut put(rowKey, family, qualifier, value)
      batchPut put(rowKey, family, qualifier, Some(value))
      batchPut put(rowKey, qualifier, value)
      batchPut put(rowKey, qualifier, Some(value))
      val cell = Cell(rowKey, family, qualifier)
      batchPut put(cell, value)
                
```

### Get/Scan syntax

The `Get` and `Scan` have a common syntax for the main part. The main difference is that the scan uses the `rows` keyword in place of the rowKey.

The basic modifiers are `family`, `qualifiers`. When using family alone the query returns all columns of the family and when using `qualifiers` it returns only the qualifiers defined for the currently defined family `"d"` if none is specified. Multiple calls of qualifiers make a union of all qualifiers given. Let's explain the following example.

``` scala
      table get rowKey qualifiers("one", "two") qualifiers("three") family("e") family("f") qualifiers("four")
                
```

This query will retrieve the row `rowKey` with the qualifiers `one`, `two` and `three` in the column family `d`, all the qualifiers of the column family `e` and the qualifier `four` in the column family `f`.

#### Filtering of the fetch queries

Mostly needed for the scan operations, but also usable for the get ones, HTalk allows using any HBase filter and provide syntactic sugar for the most usual of them.

To use an HBase filter simply use the `filter` method. If used repeatedly the filters must all match.

``` scala
      table get rows filter(new FirstKeyOnlyFilter())
                    
```

Available filters are `value` to filter on the value of the column, `columnValue` that will only keep the rows matching the given value for the qualifier, `limit` to limit the size of the query and `firstKeyOnly` to get only the first column of each row.

``` scala
      table get rows value("myValue")
      table get rows value("myValue", CompareFilter.CompareOp.NOT_EQUAL)
      table get rows columnValue(qualifier, value)
      table get rows columnValue(qualifier, value, CompareFilter.CompareOp.NOT_EQUAL)
      table get rows columnValue(family, qualifier, value)
      table get rows columnValue(family, qualifier, value, CompareFilter.CompareOp.NOT_EQUAL)
      table get rows limit(10)
      table get rows limit(Some(10))
      table get rows limit(None) // Does nothing, provided for ease of use
      table get rows firstKeyOnly
                    
```

#### Scan queries, specific filtering

The Scan queries can use `range` to specify start and stop row keys, `rangeStart` and `rangeStop` to specify only only side, `reversed` to scan in the reverse order, `startExclusive` to exclude the start row key and `reverse` or `excludeStart` to set the order and exclusive start with a Boolean.

``` scala
      table get rows range(startRowKey, stopRowKey)
      table get rows rangeStart(startRowKey) // also usable with an Option
      table get rows rangeStop(stopRowKey) // also usable with an Option
      table get rows range(Some(startRowKey), None) // Both parameters are Option[RowKey]
      val scan = table get rows reversed
      scan reverse(false) // reset the previously set reverse order
      scan startExclusive
      scan excludeStart(false) // reset the previously set exclusive start
                    
```

### Delete syntax

All the `delete` methods and the `deleteFamily` one return a Delete object. The basic delete method is `delete(rowKey, family, qualifier)`. `rowKey` is mandatory, `qualifier` and `family` can be ellipsed.

``` scala
      table delete(rowKey, family, qualifier)
      table delete(rowKey, qualifier)
      table delete(rowKey)
      batchDelete deleteFamily(rowKey, family)
                
```

## Execution of a query

To execute a query simply use the `execute` keyword as in the following example :

``` scala
      val query = table put(rowKey, family, qualifier, value) put(rowKey, family, qualifier2, value2)
      query execute
            
```

Read queries return an `Iterable[Result]` and `Put` queries return `Unit` and only have the side-effect of inserting the data in HBase.

### Specific syntax for fetch queries

Fetch queries can return huge amount of data in which case the data must be treated as a flow and not as a whole. And a facility of writing is available for small request that need mapping.

To get a read-once iterator instead of an immutable iterable use `executeAsIterator` :

``` scala
      val query = table get rows
      val resultsIterator = query executeAsIterator
            
```

To get an iterable of high level object you can use the following syntax :

``` scala
      case class MyClass(one: Int, two: Option[String])
      val query = table get rowKey qualifiers("one", two") limit(10)
      val myClassIterable = query ~ {rs =>
        MyClass(rs("one").as[Int], rs("two").asOpt[String])
      }
            
```

### Configure the context

In order to execute the queries you need to have an implicit context in scope. You can build it using the following syntax :

``` scala
      implicit val htalkContext = HTalkContext(HBaseManager(getHBaseConfiguration))
      implicit val htalkContext = HTalkContext(HBaseManager(getHBaseConfiguration("yourConfigurationFilePath"))
            
```

The default path for the configuration file is hbase-site.properties and the content of the file is :

``` text
hbase.zookeeper.quorum=hfactoryserver
hbase.zookeeper.property.clientPort=2181
zookeeper.znode.parent=/hbase
            
```
