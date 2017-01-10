package com.ubeeko.htalk.bootstrap

import com.ubeeko.htalk.bytesconv._

import com.ubeeko.htalk.criteria.Family
import com.ubeeko.htalk.criteria.Rowkey
import com.ubeeko.htalk.criteria.Timestamp

import net.liftweb.json.JString
import net.liftweb.json.JValue
import net.liftweb.json.JArray
import net.liftweb.json.JDouble
import net.liftweb.json.JInt
import net.liftweb.json.{ JField, JObject, JNothing, JNull }

import scala.collection.mutable

object JSONData {
  private val map = mutable.Map.empty[String, String]

  //TODO Use an instance
  def fixType(column: String, columnType: String): Unit = {
    map += column -> columnType
  }

  def clear(): Unit = map.clear

  /* TODO use
trait Convert[T] {
  def convert(value: JValue): T
}

object StringConvert extends Convert[String] {
  def convert(value: JValue): String = value match {
    case JString(s) => s
    case JInt(i)    => i
    case JDouble(d) => d
    case _ => throw ...
  }
}
implicit object StringConvert extends Convert[String]
==> def convert[T](x: JValue)(implicit converter: Convert[T]): T = converter.convert(x)
def convert[T: Convert](x: JValue): T = implicitly[Convert[T]].convert(x)
   */
  private def convertRowkey(value: JValue, convertTo: Option[String], entityName: String): Rowkey = {
    convertTo match {
      case None => value match {
        case JString(string) => string
        case JDouble(double) => double
        case JInt(big) => big
        case _ => throw new UnsupportedOperationException(s"$entityName type not understood: $value")
      }
      case Some("string") => value match {
        case JString(string) => string
        case JInt(int) => int.toString
        case JDouble(double) => double.toString
        case _ => throw new UnsupportedOperationException(s"$entityName value is not a string: $value")
      }
      case Some("int") => value match {
        case JInt(big) => big.toInt
        case JString(string) => Integer.parseInt(string)
        case JDouble(double) if double.isValidInt => double.toInt
        case _ => throw new UnsupportedOperationException(s"$entityName value is not a int: $value")
      }
      case Some("long") => value match {
        case JInt(big) => big.toLong
        case JString(string) => string.toLong
        case JDouble(double) if double.isValidInt => double.toLong
        case _ => throw new UnsupportedOperationException(s"$entityName value is not a long: $value")
      }
      case Some("bigint") => value match {
        case JInt(big) => big
        case JString(string) => BigInt(string)
        case JDouble(double) if double.isValidInt => BigInt(double.toInt)
        case _ => throw new UnsupportedOperationException(s"$entityName value is not a bigint: $value")
      }
      case Some("float") => value match {
        case JDouble(double) => double.toFloat
        case JString(string) => string.toFloat
        case JInt(int) => int.toFloat
        case _ => throw new UnsupportedOperationException(s"$entityName value is not a float: $value")
      }
      case Some("double") => value match {
        case JDouble(double) => double
        case JString(string) => string.toDouble
        case JInt(int) => int.toDouble
        case _ => throw new UnsupportedOperationException(s"$entityName value is not a double: $value")
      }
      case _ => throw new UnsupportedOperationException(s"$entityName type not understood: $value")
    }
  }

  // To automatically convert typed values to byte arrays in convertCelLValue().
  implicit def old_toByteArray[T](v: T)(implicit conv: BytesConv[T]): Array[Byte] = conv(v)
  //implicit def toByteArray[T](v: T): Array[Byte] = bytesFrom[T](v)

  private def convertCellValue(value: JValue, convertTo: Option[String], entityName: String): Array[Byte] = {
    convertTo match {
      case None => value match {
        case JString(string) => string
        case JDouble(double) => double
        case JInt(big) => big
        case _ => throw new UnsupportedOperationException(s"$entityName type not understood: $value")
      }
      case Some("string") => value match {
        case JString(string) => string
        case JInt(int) => int.toString
        case JDouble(double) => double.toString
        case _ => throw new UnsupportedOperationException(s"$entityName value is not a string: $value")
      }
      case Some("int") => value match {
        case JInt(big) => big.toInt
        case JString(string) => string.toInt
        case JDouble(double) if double.isValidInt => double.toInt
        case _ => throw new UnsupportedOperationException(s"$entityName value is not a int: $value")
      }
      case Some("long") => value match {
        case JInt(big) => big.toLong
        case JString(string) => string.toLong
        case JDouble(double) if double.isValidInt => double.toLong
        case _ => throw new UnsupportedOperationException(s"$entityName value is not a long: $value")
      }
      case Some("bigint") => value match {
        case JInt(big) => big
        case JString(string) => BigInt(string)
        case JDouble(double) if double.isValidInt => BigInt(double.toInt)
        case _ => throw new UnsupportedOperationException(s"$entityName value is not a bigint: $value")
      }
      case Some("float") => value match {
        case JDouble(double) => double.toFloat
        case JString(string) => string.toFloat
        case JInt(int) => int.toFloat
        case _ => throw new UnsupportedOperationException(s"$entityName value is not a float: $value")
      }
      case Some("double") => value match {
        case JDouble(double) => double
        case JString(string) => string.toDouble
        case JInt(int) => int.toDouble
        case _ => throw new UnsupportedOperationException(s"$entityName value is not a double: $value")
      }
      case _ => throw new UnsupportedOperationException(s"$entityName type not understood: $value")
    }
  }

  private def convertToRowkey(value: JValue): Rowkey = convertRowkey(value, map.get("id"), "Row id")

  private def convertToCellValue(value: JValue, columnName: String): Array[Byte] =
    convertCellValue(value, map.get(columnName), s"Column $columnName")

  def getTypes(data: JValue): Iterable[(String, String)] = {
    data \ "types" match {
      case JObject(fields: List[JField]) => fields.map(_ match {
        case JField(name, JString(value)) => (name, value)
        case value => throw new UnsupportedOperationException(s"Malformed column type: $value")
      })
      case JNothing | JNull => Iterable.empty
      case types => throw new UnsupportedOperationException(s"Malformed types: $types")
    }
  }

  def getTable(data: JValue) = data \ "table"

  def getName(data: JValue): Option[String] =
    data \ "name" match {
      case JString(table) => Some(table)
      case _ => None
    }

  def getFamilyNames(table: JValue): Set[Family] = {
    // Get the families array if it exists
    val families = getValues(table, "families").map(_ \ "name" match {
      case JString(name) => name: Family
      case _ => Family.Default
    }).toSet
    val result = if (families.isEmpty) {
      // Get the family from all the rows since families array is not present
      val familiesInScope = (table \\ "f" \\ classOf[JString])
      familiesInScope.map(_.toString: Family).toSet
    } else {
      // Use families defined
      families
    }
    // Always add the default family
    result + Family.Default
  }

  private def getValues(data: JValue, field: String): List[JValue] = data \ field match {
    case l: JArray => l.children
    case _ => List.empty
  }

  def getRows(data: JValue) = getValues(data, "rows")

  def getColumns(row: JValue) = getValues(row, "c")

  def getRowId(row: JValue) = convertToRowkey(row \ "id")

  def getTimestamp(row: JValue): Option[Timestamp] = row \ "ts" match {
    case JInt(ts) => Some(ts.toLong)
    case _ => None
  }

  def getValue(column: JValue): Array[Byte] = convertToCellValue(column \ "v", getQualifier(column))

  private def getStringValue(container: JValue, field: String, default: Option[String]) =
    container \ field match {
      case JString(value) => value
      case _ => default.getOrElse(throw new Exception("Not a String")) //TODO use a Business Rule Exception
    }

  def getFamily(column: JValue) = getStringValue(column, "f", Some("d"))

  def getQualifier(column: JValue) = getStringValue(column, "q", None)
}