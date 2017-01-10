package com.ubeeko.htalk.criteria

import com.ubeeko.htalk.bytesconv._
import org.specs2.mutable.Specification

class TimestampSpec extends Specification {
  "Timestamp" can {
    "be created from zero" in {
      Timestamp(0).time must_== 0
    }

    "be created from a positive long" in {
      Timestamp(3).time must_== 3
      Timestamp(Long.MaxValue).time must_== Long.MaxValue
    }

    "not be created from a negative long" in {
      val negValues = Seq(Long.MinValue, -12345L, -20, -2, -1)
      negValues forall { v =>
        Timestamp(v) must throwA[InvalidTimestampException].like {
          case e: InvalidTimestampException => e.ts must_== v.toString
        }
      }
    }
  }

  "Timestamp.reversed" should {
    "return a timestamp as expected by HBase for newest-first ordering" in {
      (0 to 100) forall { time =>
        val ts = Timestamp(time)
        ts.reversed.time must_== Long.MaxValue - ts.time
        ts.reversed.reversed.time must_== ts.time
      }
    }
  }

  "Timestamp" should {
    "be ordered" in {
      (1 to 100) forall { _ =>
        val ts1 = Timestamp.random()
        val ts2 = Timestamp.random()
        ts1.compare(ts2) must_== ts1.time.compare(ts2.time)
      }
    }
  }

  "Timestamp.now" should {
    "always increase" in {
      (Timestamp.now /: (1 to 100)) { case (before, _) =>
        // Sleep 1 ms to ensure we do get different timestamps, as each iteration
        // of the fold may be faster than the timestamp resolution.
        Thread.sleep(1)
        val now = Timestamp.now
        now must be_>(before)
        now
      }
      true
    }
  }

  "Timestamp.next" should {
    "return None if the timestamp is the maximum" in {
      Timestamp.MaxValue.next must beNone
    }

    "return the immediately succeeding timestamp if not the maximum" in {
      def nonMaxRandom() = {
        val ts = Timestamp.random()
        if (ts == Timestamp.MaxValue) Timestamp(ts.time - 1) else ts
      }
      (1 to 100) forall { _ =>
        val ts = nonMaxRandom()
        ts.next must beSome(Timestamp(ts.time + 1))
      }
    }
  }

  "Timestamp.prev" should {
    "return None if the timestamp is the minimum (zero)" in {
      Timestamp.MinValue.prev must beNone
    }

    "return the immediately preceeding timestamp if not the minimum" in {
      def nonMinRandom() = {
        val ts = Timestamp.random()
        if (ts == Timestamp.MinValue) Timestamp(ts.time + 1) else ts
      }
      (1 to 100) forall { _ =>
        val ts = nonMinRandom()
        ts.prev must beSome(Timestamp(ts.time - 1))
      }
    }
  }

  "Composition of Timestamp.prev and Timestamp.next" should {
    "be identity" in {
      val ts = Timestamp.now
      ts.prev flatMap (_.next) must beSome(ts)
      ts.next flatMap (_.prev) must beSome(ts)
    }
  }

  "Timestamp" should {
    "be serializable and deserializable to/from bytes" in {
      val origTs  = Timestamp.random()
      val finalTs = bytesTo[Timestamp](bytesFrom[Timestamp](origTs))
      finalTs must_== origTs
    }
  }
}
