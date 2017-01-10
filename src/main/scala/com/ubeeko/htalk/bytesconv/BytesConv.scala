package com.ubeeko.htalk.bytesconv

import java.util.UUID

import org.apache.hadoop.hbase.util.Bytes

import scala.reflect.ClassTag
import scala.util.control.NonFatal

/** Raised by `BytesConv` when conversion fails. */
class BytesConvException(typeName: String, value: Array[Byte])
    extends Exception(s"Invalid $typeName value: '$value'")

abstract class BytesConv[T](implicit ct: ClassTag[T]) {
  final def apply(b: Array[Byte]): T =
    try fromBytes(b)
    catch {
      case NonFatal(_) => throw new BytesConvException(typeName, b)
    }

  final def apply(x: T): Array[Byte] = toBytes(x)

  final val typeName = ct.runtimeClass.getCanonicalName

  /** Performs the conversion from byte array.
    * Do '''not''' call this method directly. Instead, use method `apply`, which
    * converts any exception raised by this method to `BytesConvException`.
    */
  protected def fromBytes(b: Array[Byte]): T

  /** Performs the conversion to byte array.
    * Do '''not''' call this method directly. Instead, use method `apply`, which
    * converts any exception raised by this method to `BytesConvException`.
    */
  protected def toBytes(x: T): Array[Byte]
}

object BytesConv {
  implicit object BytesBytesConv extends BytesConv[Array[Byte]] {
    protected def fromBytes(b: Array[Byte]): Array[Byte] = b
    protected def toBytes(x: Array[Byte]): Array[Byte] = x
  }

  // Char = 16 bit unsigned integer.
  implicit object CharBytesConv extends BytesConv[Char] {
    protected def fromBytes(b: Array[Byte]): Char = Bytes.toShort(b).toChar
    protected def toBytes(x: Char): Array[Byte] = Bytes.toBytes(x.toShort)
  }

  implicit object StringBytesConv extends BytesConv[String] {
    protected def fromBytes(b: Array[Byte]): String = Bytes.toString(b)
    protected def toBytes(x: String): Array[Byte] = Bytes.toBytes(x)
  }

  implicit object BooleanBytesConv extends BytesConv[Boolean] {
    protected def fromBytes(b: Array[Byte]): Boolean = Bytes.toBoolean(b)
    protected def toBytes(x: Boolean): Array[Byte] = Bytes.toBytes(x)
  }

  implicit object IntBytesConv extends BytesConv[Int] {
    protected def fromBytes(b: Array[Byte]): Int = Bytes.toInt(b)
    protected def toBytes(x: Int): Array[Byte] = Bytes.toBytes(x)
  }

  implicit object LongBytesConv extends BytesConv[Long] {
    protected def fromBytes(b: Array[Byte]): Long = Bytes.toLong(b)
    protected def toBytes(x: Long): Array[Byte] = Bytes.toBytes(x)
  }

  implicit object FloatBytesConv extends BytesConv[Float] {
    protected def fromBytes(b: Array[Byte]): Float = Bytes.toFloat(b)
    protected def toBytes(x: Float): Array[Byte] = Bytes.toBytes(x)
  }

  implicit object DoubleBytesConv extends BytesConv[Double] {
    protected def fromBytes(b: Array[Byte]): Double = Bytes.toDouble(b)
    protected def toBytes(x: Double): Array[Byte] = Bytes.toBytes(x)
  }

  implicit object BigIntBytesConv extends BytesConv[BigInt] {
    protected def fromBytes(b: Array[Byte]): BigInt = BigInt(b)
    // As was done in the original toCellValue implicit conversion.
    protected def toBytes(x: BigInt): Array[Byte] = x.toByteArray
  }

  implicit object UUIDBytesConv extends BytesConv[UUID] {
    // XXX Crap but for now I don't care.
    protected def fromBytes(b: Array[Byte]): UUID = UUID.fromString(StringBytesConv(b))
    protected def toBytes(x: UUID): Array[Byte] = Bytes.toBytes(x.toString)
  }

  // No Date instance since a Date can take many forms: we'll let the users provide
  // their own instance, tailored to their needs.
}
