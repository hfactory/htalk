package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 15:28
 */
trait InvalidRightOperation extends IllegalOperationException

object InvalidRightOperation {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with InvalidRightOperation
}

//TODO temporary java compatible case class
case class JavaCompatibleInvalidRightOperation(message: String, cause: Throwable = null) extends Exception(message, cause) with InvalidRightOperation {
  def this(message: String) = this(message, null)
}
