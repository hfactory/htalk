/** *****************************************************************************
  * Copyright(c) 2011-1012 Ubeeko
  * All rights reserved.
  * *****************************************************************************/
package com.ubeeko.exceptions

import scala.collection.mutable

trait ConflictExceptions extends BusinessRuleException

object ConflictExceptions {
  def apply(conflictExceptions: mutable.Seq[ConflictException], message: String, cause: Throwable = null) = {
    new Exception(message, cause) with ConflictExceptions
  }
}

class JavaCompatibleConflictExceptions(conflictExceptions: mutable.Seq[ConflictException],
                                       message: String, cause: Throwable = null)
  extends Exception(message, cause) with ConflictExceptions
{
  def this(conflictExceptions: mutable.Seq[ConflictException], message: String) = this(conflictExceptions, message, null)
  def this(message:String) = this(mutable.Seq(), message, null)

  def add(exception: ConflictException) {
    conflictExceptions :+ exception
  }

  def isEmpty: Boolean = conflictExceptions.isEmpty

}
