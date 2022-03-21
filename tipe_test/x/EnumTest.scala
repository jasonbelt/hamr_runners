// #Sireum
package org.sireum.cli.hamr_runners.tipe_test.x

import org.sireum._

object EnumTest {

  @enum object X {
    "a"
    "b"
  }

  @datatype class A(name: String)

  @datatype class B(a: A)

  val x: X.Type = X.a

  val datatypes: B = B(A("hi"))

  val name: String = datatypes.a.name

  def a(): Unit = {
    println(x)

    println(datatypes.a.name)
  }
}
