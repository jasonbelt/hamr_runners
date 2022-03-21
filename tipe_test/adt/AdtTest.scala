// #Sireum
package org.sireum.cli.hamr_runners.tipe_test.adt

import org.sireum._
import org.sireum.S32._

object AdtTest extends App {

  type Integer_32 = S32

  @datatype class Temperature(value: Integer_32)

  val t1: Temperature = Temperature(s32"7")

  def main(args: ISZ[String]): Z = {

    println(t1.value)

    return 0
  }
}
