package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.bytesconv._

class Rowkey(val value: Array[Byte]) extends AnyVal

object Rowkey {
  implicit def apply[T](x: T)(implicit conv: BytesConv[T]): Rowkey =
    new Rowkey(conv(x))
}
