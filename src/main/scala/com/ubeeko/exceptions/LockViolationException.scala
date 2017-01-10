package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 28/08/13
 */
trait LockViolationException extends InvalidRightOperation

object LockViolationException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with LockViolationException
}
