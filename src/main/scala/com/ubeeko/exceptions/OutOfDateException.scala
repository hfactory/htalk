package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 15:19
 */
trait OutOfDateException extends BusinessRuleException

object OutOfDateException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with OutOfDateException
}
