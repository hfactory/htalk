package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.bytesconv._
import org.specs2.mutable.Specification

class QualifierSpec extends Specification {
  "A qualifier" can {
    "be created from Array[Byte]" in {
      val b = Array[Byte](1, 2, 55, 3, 4)
      val q = Qualifier(b)
      q.value must beEqualTo(b)
    }

    "be created from String" in {
      val s = "hello"
      val q = Qualifier(s)
      q.value must beEqualTo(bytesFrom[String](s))
    }
  }
}
