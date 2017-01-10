package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 10/06/13
 * Time: 15:37
 */
trait InvalidDataStructureException extends TechnicalException with RetryableException

object InvalidDataStructureException {
  def apply(message: String, cause: Throwable = null) = new Exception(message, cause) with InvalidDataStructureException
}
