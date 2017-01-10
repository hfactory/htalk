package com.ubeeko.exceptions

/**
 * Created with IntelliJ IDEA.
 * User: elb
 * Date: 18/06/13
 * Time: 10:15
 */
trait SpecificException extends Exception {
  @transient val specificException: Throwable
}
