package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 15:37
 */
trait AlreadyConnectedException extends TechnicalException

object AlreadyConnectedException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with AlreadyConnectedException
}
