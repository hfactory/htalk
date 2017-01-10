package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 13:59
 */
trait ConfigurationException extends TechnicalException with RetryableException

object ConfigurationException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with ConfigurationException
}

