package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 15:19
 */
trait AlreadyExistingElementException extends BusinessRuleException

object AlreadyExistingElementException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with AlreadyExistingElementException
}

//TODO temporary java compatible case class
case class JavaCompatibleAlreadyExistingElementException(message: String, cause: Throwable = null) extends Exception(message, cause) with AlreadyExistingElementException {
  def this(message: String) = this(message, null)
}
