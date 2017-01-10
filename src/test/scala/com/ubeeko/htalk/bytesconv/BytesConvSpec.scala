package com.ubeeko.htalk.bytesconv

import java.util.UUID

import org.specs2.mutable.Specification

import scala.util.Random

class BytesConvSpec extends Specification {
  "BytesConv" should {
    "convert back and forth between bytes and type" in {
      def randBytes(): Array[Byte] = {
        val b = Array[Byte](13)
        Random.nextBytes(b)
        b
      }
      Stream.continually(randBytes()).take(10) forall { v =>
        bytesTo[Array[Byte]](v) must_== v
      }

      Seq(' ', 'A', 'Z', 'a', 'z', '?', '@', 'é') forall { v =>
        bytesTo[Char](bytesFrom(v)) must_== v
      }

      Seq("", "one", "two", "a b c") forall { v =>
        bytesTo[String](bytesFrom(v)) must_== v
      }

      Seq(true, false) forall { v =>
        bytesTo[Boolean](bytesFrom(v)) must_== v
      }

      Seq(Int.MinValue, -25, -3, 0, 1, 2, 92837, Int.MaxValue) forall { v =>
        bytesTo[Int](bytesFrom(v)) must_== v
      }

      Seq(Float.MinValue, -55.78f, 0.0f, 1.0f, 18.34f, Float.MaxValue) forall { v =>
        bytesTo[Float](bytesFrom(v)) must_== v
      }

      Seq(Double.MinValue, -29387d, 0.0d, 1.0d, +3.14159265d, Double.MaxValue) forall { v =>
        bytesTo[Double](bytesFrom(v)) must_== v
      }

      (0 until 10) map (_ => UUID.randomUUID) forall { v =>
        bytesTo[UUID](bytesFrom(v.toString)) must_== v
      }
    }
  }

}
