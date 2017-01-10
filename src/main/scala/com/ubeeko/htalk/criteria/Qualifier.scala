package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.bytesconv._

trait Qualifier {
  def value: Array[Byte]
}

object Qualifier {
  def apply(b: Array[Byte]) = new Qualifier {
    val value = b
  }

  def apply(s: String) = new Qualifier {
    val value = bytesFrom[String](s)
  }

  implicit def qualifierToByteArray(q: Qualifier): Array[Byte] =
    q.value
}