package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 18/02/13
 * Time: 09:13
 */
trait IllegalOperationException extends Exception

object IllegalOperationException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with IllegalOperationException
}


//TODO temporary java compatible case class
case class JavaCompatibleIllegalOperationException(message: String, cause: Throwable = null) extends Exception(message, cause) with IllegalOperationException {
  def this(message: String) = this(message, null)
}