package org.sireum.cli.hamr_runners.casex

import org.sireum._

object PFC_splat{

  def main(args: Array[Predef.String]): Unit = {
    ///home/sireum/devel/sel4/home/CASETeam/examples/ksu-proprietary/producer-splatfilter-consumer/pfc-project/src/aadl/Dec2019/.slang/PFC_PFC_Sys_Impl_Instance.json
    val srcDir = Os.home / "devel/case/CASETeam/examples/ksu-proprietary/producer-splatfilter-consumer/pfc-project/"
    val aadlDir = srcDir / "src/aadl/Dec2019"
    val cDir = srcDir / "src/c"
    val slang = aadlDir / ".slang/PFC_PFC_Sys_Impl_Instance.json"

    val projName = "pfc_splat_filter"

    val o = Util.o(
      args = ISZ(slang.value),
      devicesAsThreads = F,
      outputDir = Some(srcDir.value),
      excludeComponentImpl = F,

      bitWidth = 32,
      maxStringSize = 256,
      maxArraySize = 12,

      slangOutputCDir = Some(cDir.value),
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


    if(ret == 0) {
      // C targets

      ret = run(o(platform = Cli.HamrPlatform.Linux))
      ret = run(o(platform = Cli.HamrPlatform.MacOS))
      ret = run(o(platform = Cli.HamrPlatform.Cygwin))
    }

    if(ret == 0) {
      // ARSIT + ACT

      val camkesOutputDir = cDir / "CAmkES_seL4"
      val camkesAuxCodeDir = cDir / "camkes_aux_code"
      val projectAppsDir = appsDir / s"${projName}_sel4"

      ret = run(o(
        platform = Cli.HamrPlatform.SeL4,
        camkesOutputDir = Some(camkesOutputDir.value),
        camkesAuxCodeDirs = ISZ(camkesAuxCodeDir.value)
      ))

      if (ret == 0) {
        camkesOutputDir.copyOverTo(projectAppsDir)
        println(s"Copied CAmkES to code file:///${projectAppsDir.value}/")

        val ext_c = camkesOutputDir / "hamr/ext/ext.c"
        val ext_c_camkes = projectAppsDir / "hamr/ext/ext.c"
        if (ext_c_camkes.exists) {
          ext_c_camkes.remove()
        }
        ext_c.copyOverTo(ext_c_camkes)
        println(s"Copied file:///${ext_c} to file:///${ext_c_camkes}")

        val ext_h = camkesOutputDir / "hamr/ext/ext.h"
        val ext_h_camkes = projectAppsDir / "hamr/ext/ext.h"
        if (ext_h_camkes.exists) {
          ext_h_camkes.remove()
        }
        ext_h.copyOverTo(ext_h_camkes)
        println(s"Copied file:///${ext_h} to file:///${ext_h_camkes}")
      }
    }
  }
}