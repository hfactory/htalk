/** *****************************************************************************
  * Copyright(c) 2011-1012 Ubeeko
  * All rights reserved.
  *
  * Created: 09/07/13
  * Author: ivan
  * *****************************************************************************/
package com.ubeeko.exceptions

trait ConflictException extends BusinessRuleException

object ConflictException {
  def apply(expectedVersion:Long, message: String, cause: Throwable = null) = new Exception(message, cause) with ConflictException
}

//TODO temporary java compatible case class
case class JavaCompatibleConflictException(expectedVersion: Long, message: String, cause: Throwable = null) extends Exception(message, cause) with ConflictException {
  def this(expectedVersion: Long, message: String) = this(expectedVersion, message, null)
}