package org.sireum.cli.hamr

import org.sireum._
import org.sireum.cli._

object buildingControl_Runner {

  def main(args: Array[Predef.String]): Unit = {

    val projName = "building-control-mixed"

    val srcDir = Os.home / "devel/slang-embedded/slang-embedded-building-control/building-control-gen-mixed"
    val aadlDir = srcDir / "src/aadl"
    val cDir = srcDir / "src/c"
    val slang = aadlDir / ".slang/BuildingControl_BuildingControlDemo_i_Instance.json"

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
      // TB

      val camkesOutputDir = cDir / "CAmkES_seL4_TB"
      val projectAppsDir = appsDir / s"${projName}_sel4_tb"

      ret = run(o(
        platform = Cli.HamrPlatform.SeL4_TB,
        camkesOutputDir = Some(camkesOutputDir.value),
        camkesAuxCodeDirs = ISZ()
      ))

      camkesOutputDir.copyOverTo(projectAppsDir)
      println(s"Copied CAmkES code to file:///${projectAppsDir.value}")
    }

    if(ret == 0) {
      // C targets

      ret = run(o(platform = Cli.HamrPlatform.Linux))

      if(ret == 0) {
        ret = run(o(platform = Cli.HamrPlatform.MacOS))
      }

      if(ret == 0) {
        ret = run(o(platform = Cli.HamrPlatform.Cygwin))
      }
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