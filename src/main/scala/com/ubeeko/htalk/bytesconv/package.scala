package com.ubeeko.htalk

package object bytesconv {
  def bytesTo[T](b: Array[Byte])(implicit conv: BytesConv[T]): T = conv(b)
  def bytesFrom[T](x: T)(implicit conv: BytesConv[T]): Array[Byte] = conv(x)
}
