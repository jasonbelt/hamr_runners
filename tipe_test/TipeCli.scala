// #Sireum
package org.sireum.cli.hamr_runners.tipe_test

import org.sireum._

object TipeCli extends App{

  def main(args: ISZ[String]): Z = {

    val path = Os.home / "devel/sireum/kekinian/cli/jvm/src/main/scala/org/sireum/cli/hamr_runners/tipe_test/spec-defs"

    Sireum.run(ISZ("proyek", "tipe", "--verbose", path.value))

    Sireum.run(ISZ("proyek", "logika", "--all", path.value))

    return 0
  }
}
