package org.sireum.cli.hamr

import org.sireum._
import org.sireum.cli._

object Urgent_Runner {

  def main(args: Array[Predef.String]): Unit = {

    val srcDir = Os.home / "devel/slang-embedded/urgency/urgency"
    val aadlDir = srcDir / "src/aadl"
    val cDir = srcDir / "src/c"
    val slang = aadlDir / ".slang/Urgency_Sys_impl_Instance.json"

    val o = Util.o(
      args = ISZ(slang.value),
      devicesAsThreads = F,
      outputDir = Some(srcDir.value),
      excludeComponentImpl = F,

      aadlRootDir = Some(aadlDir.value),
    )

    val appsDir = Os.path("/home/sireum/devel/sel4/home/camkes-project/projects/camkes/apps")

    def run(o: Cli.HamrCodeGenOption): Int = {
      val ret = cli.HAMR.codeGen(o)
      return ret
    }

    var ret = 0

    if(ret == 0) {
      ret = run(o(
        platform = Cli.HamrPlatform.JVM,
      ))
    }
  }
}