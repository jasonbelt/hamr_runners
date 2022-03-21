// #Sireum
package org.sireum.cli.hamr_runners.tipe_test

import org.sireum._

object TipeCli extends App{

  def main(args: ISZ[String]): Z = {

    //val path = Os.home / "CASE/Sireum/cli/jvm/src/main/scala/org/sireum/cli/hamr_runners/tipe_test/x"
    val path = Os.home / "CASE/Sireum/cli/jvm/src/main/scala/org/sireum/cli/hamr_runners/tipe_test/adt"

    Sireum.run(ISZ("slang", "tipe", "-s", path.value))

    return 0
  }
}
