/** *****************************************************************************
  * Copyright(c) 2011-1012 Ubeeko
  * All rights reserved.
  *
  * Created: 17/07/13
  * Author: ivan
  * *****************************************************************************/
package com.ubeeko.exceptions

import scala.collection.mutable

trait InvalidParameterExceptions extends IllegalOperationException

object InvalidParameterExceptions {
  def apply(invalidParameterExceptions: mutable.Seq[InvalidParameterException], message: String, cause: Throwable = null) = {
    new Exception(message, cause) with InvalidParameterExceptions
  }
}

class JavaCompatibleInvalidParameterExceptions(invalidParameterExceptions: mutable.Seq[InvalidParameterException],
                                       message: String, cause: Throwable = null)
  extends Exception(message, cause) with InvalidParameterExceptions
{
  def this(invalidParameterExceptions: mutable.Seq[InvalidParameterException], message: String) = this(invalidParameterExceptions, message, null)
  def this(message:String) = this(mutable.Seq(), message, null)

  def add(exception: InvalidParameterException) {
    invalidParameterExceptions :+ exception
  }

  def isEmpty: Boolean = invalidParameterExceptions.isEmpty

}
