package org.sireum.cli.hamr_runners.slang_embedded

import org.sireum._
import org.sireum.cli.hamr_runners.Util

object Urgent {

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

    def run(o: Cli.SireumHamrCodegenOption): Int = {
      val ret = cli.HAMR.codeGen(o)
      return ret.toInt
    }

    var ret = 0

    if(ret == 0) {
      ret = run(o(
        platform = Cli.SireumHamrCodegenHamrPlatform.JVM,
      ))
    }
  }
}