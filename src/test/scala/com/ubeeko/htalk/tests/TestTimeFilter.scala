package com.ubeeko.htalk.tests

import org.scalatest._
import com.ubeeko.htalk.criteria._
import scala.concurrent.duration._
import java.util.Date

@Ignore
class TestTimeFilterSpec extends FlatSpec with Matchers {

  "TimeFilter" should "filter with exact time" in {
    val now = new Date().getTime()
    val precise = "user" get "one" at (now)
    val hGet = precise hBaseGet

    assertResult(now)(hGet.getTimeRange().getMin())
  }

  it should "filter after a certain time" in {
    val precise = "user" get "one" after (new Date().getTime())
  }

  it should "filter before a certain time" in {
    val precise = "user" get "one" before (new Date().getTime())
  }

  it should "filter between  to dates" in {
    val precise = "user" get "one" between (new Date().getTime(), new Date().getTime())
  }

  it should "filter the lase two days" in {
    val precise = "user" get "one" last (2.days)
  }

  it should "filter today only" in {
    val precise = "user" get "one" today
  }

}