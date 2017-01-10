package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 15:28
 */
trait NotImplementedOperation extends IllegalOperationException

object NotImplementedOperation {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with NotImplementedOperation
}

//TODO temporary java compatible case class
case class JavaCompatibleNotImplementedOperation(message: String, cause: Throwable = null) extends Exception(message, cause) with NotImplementedOperation {
  def this(message: String) = this(message, null)
}