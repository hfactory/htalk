package com.ubeeko.htalk.criteria

import scala.concurrent.duration._

case class Family(value: String, options: FamilyOption*)

object Family {
  val Default = Family("d", Version(1))
}

sealed abstract class FamilyOption

//TODO thinking about using Duration from akka
case class TTL(duration: Duration) extends FamilyOption

case class Version(count: Int) extends FamilyOption
