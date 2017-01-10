package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 15:37
 */
trait NotConnectedException extends TechnicalException with RetryableException

object NotConnectedException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with NotConnectedException
}

//TODO temporary java compatible case class
case class JavaCompatibleNotConnectedException(message: String, cause: Throwable = null) extends Exception(message, cause) with NotConnectedException {
  def this(message: String) = this(message, null)
}
