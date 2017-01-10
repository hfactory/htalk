package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.bytesconv._

import scala.util.Random

/** Raised when a timestamp is invalid. */
class InvalidTimestampException(val ts: String) extends Exception {
  def this(ts: Long) = this(ts.toString)
  override def getMessage: String = s"Invalid timestamp '$ts'"
}

/** Timestamp with millisecond resolution. */
class Timestamp private (val time: Long) extends AnyVal with Ordered[Timestamp] {
  def compare(that: Timestamp): Int =
    if (time < that.time) -1
    else if (time > that.time) +1
    else 0

  /** Returns the timestamp reversed.
    * Can be used to order a timeseries so that newest items come first.
    */
  def reversed = new Timestamp(Long.MaxValue - time)

  /** Returns the timestamp immediately preceding this one.
    *
    * A timestamp has millisecond resolution, so the timestamp returned
    * is this timestamp - 1 ms.
    */
  def prev: Option[Timestamp] =
    if (this == Timestamp.MinValue) None else Some(new Timestamp(time - 1))

  /** Returns the timestamp immediately following this one.
    *
    * A timestamp has millisecond resolution, so the timestamp returned
    * is this timestamp + 1 ms.
    */
  def next: Option[Timestamp] =
    if (this == Timestamp.MaxValue) None else Some(new Timestamp(time + 1))

  override def toString = time.toString
}

object Timestamp {
  /** Creates a timestamp.
    * Raises [[com.ubeeko.htalk.criteria.InvalidTimestampException]] if the
    * timestamp is not valid, i.e. positive.
    */
  def apply(time: Long) = {
    if (time < 0)
      throw new InvalidTimestampException(time)
    new Timestamp(time)
  }

  val MinValue = Timestamp(0L)
  val MaxValue = Timestamp(Long.MaxValue)

  /** Returns the timestamp for the current time. */
  def now = new Timestamp(System.currentTimeMillis())

  /** Returns a random timestamp. */
  def random(): Timestamp =
    Random.nextLong() match {
      case Long.MinValue => Timestamp(Long.MaxValue)
      case x if x < 0    => Timestamp(-x)
      case x             => Timestamp(x)
    }

  implicit def timestampToLong(ts: Timestamp): Long = ts.time

  implicit object TimestampBytesConv extends BytesConv[Timestamp] {
    protected def fromBytes(b: Array[Byte]): Timestamp = new Timestamp(bytesTo[Long](b))
    protected def toBytes(ts: Timestamp): Array[Byte] = bytesFrom[Long](ts.time)
  }
}
