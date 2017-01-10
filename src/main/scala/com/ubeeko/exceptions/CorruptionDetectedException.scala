package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 15:37
 */
trait CorruptionDetectedException extends TechnicalException

object CorruptionDetectedException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with CorruptionDetectedException
}

//TODO temporary java compatible case class
case class JavaCompatibleCorruptionDetectedException(message: String, cause: Throwable = null) extends Exception(message, cause) with CorruptionDetectedException{
  def this(message: String) = this(message, null)
}