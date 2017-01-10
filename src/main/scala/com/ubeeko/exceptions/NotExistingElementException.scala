package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 15:19
 */
trait NotExistingElementException extends BusinessRuleException

object NotExistingElementException {
  def apply(message: String, cause: Throwable = null, fatal: Boolean = true) = {
    if (fatal) {
      new Exception(message, cause) with NotExistingElementException
    } else {
      new Exception(message, cause) with NotExistingElementException with RetryableException
    }
  }
}

//TODO temporary java compatible case class
case class JavaCompatibleNotExistingElementException(message: String, cause: Throwable = null) extends Exception(message, cause) with NotExistingElementException {
  def this(message: String) = this(message, null)
}
