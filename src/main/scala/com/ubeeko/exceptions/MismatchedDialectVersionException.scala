package com.ubeeko.exceptions

trait MismatchedDialectVersionException extends TechnicalException with RetryableException

object MismatchedDialectVersionException {
  def apply(message: String, cause: Throwable = null) =
    new Exception(message, cause) with MismatchedDialectVersionException
}
