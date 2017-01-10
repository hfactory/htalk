package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 28/08/13
 */
trait UnknownLockException extends NotExistingElementException

object UnknownLockException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with UnknownLockException
}

