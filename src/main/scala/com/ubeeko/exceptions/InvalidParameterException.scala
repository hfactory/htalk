package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 15:28
 */
trait InvalidParameterException extends IllegalOperationException

object InvalidParameterException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with InvalidParameterException
}

//TODO temporary java compatible case class
case class JavaCompatibleInvalidParameterException(message: String, cause: Throwable = null) extends Exception(message, cause) with InvalidParameterException {
  def this(message: String) = this(message, null)
}
