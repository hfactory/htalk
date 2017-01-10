/** *****************************************************************************
  * Copyright(c) 2011-2013 Ubeeko
  * All rights reserved.
  * *****************************************************************************/
package com.ubeeko.htalk.criteria

abstract class CompoundQualifier extends Qualifier {
  final def value = prefix ++ (separator getOrElse Array.empty) ++ suffix
  def prefix: Array[Byte]
  def suffix: Array[Byte]
  def separator: Option[Array[Byte]] = None
}
