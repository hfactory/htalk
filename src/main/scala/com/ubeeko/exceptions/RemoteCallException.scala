package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 15:37
 */
trait RemoteCallException extends TechnicalException with RetryableException

object RemoteCallException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with RemoteCallException
}
