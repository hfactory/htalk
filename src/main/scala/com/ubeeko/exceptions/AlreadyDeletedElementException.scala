package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 15:19
 */
trait AlreadyDeletedElementException extends BusinessRuleException

object AlreadyDeletedElementException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with AlreadyDeletedElementException
}
