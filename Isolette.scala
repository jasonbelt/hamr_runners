package org.sireum.cli.hamr_runners

import org.sireum._

object Isolette {

  def main(args: Array[Predef.String]): Unit = {

    // /home/vagrant/devel/slang-embedded/isolette/isolette/src/aadl/.slang/Isolette_isolette_single_sensor_Instance.json

    val rootDir = Os.home / "devel/sel4/home/isolette/isolette"
    val srcDir = rootDir / "src"
    val cDir = srcDir / "c"
    val aadlDir = srcDir / "aadl"
    val slang = aadlDir / ".slang/Isolette_isolette_single_sensor_Instance.json"

    val o = Util.o(
      args = ISZ(slang.value),
      devicesAsThreads = F,
      outputDir = Some(rootDir.value),
      excludeComponentImpl = F,

      bitWidth = 32,
      maxStringSize = 125,
      maxArraySize = 16,
      
      aadlRootDir = Some(aadlDir.value),
    )
    
    val appsDir = Os.path("/home/sireum/devel/sel4/home/camkes-project/projects/camkes/apps")
    
    def run(o: Cli.HamrCodeGenOption): Int = {
      val ret = cli.HAMR.codeGen(o)
      return ret
    }
        
    var ret = 0

    if(ret == 1) {
      // TB
      
      val camkesOutputDir = cDir / "CAmkES_seL4_TB"
      val isoletteAppsDir = cDir / "isolette_sel4_tb"
      
      ret = run(o(
        platform = Cli.HamrPlatform.SeL4_TB,
        camkesOutputDir = Some(camkesOutputDir.value),
        camkesAuxCodeDirs = ISZ()
      ))

      camkesOutputDir.copyOverTo(isoletteAppsDir)
      println(s"Copied CAmkES code to file:///${isoletteAppsDir.value}")
    }

    if(ret == 0) {
      // JVM

      ret = run(o(platform = Cli.HamrPlatform.JVM))

    }
    
    if(ret == 0) {
      // LINUX

      ret = run(o(platform = Cli.HamrPlatform.Linux))
      ret = run(o(platform = Cli.HamrPlatform.MacOS))
      ret = run(o(platform = Cli.HamrPlatform.Cygwin))

    }

    if(ret == 0) {
      // ARSIT + ACT

      val camkesOutputDir = cDir / "CAmkES_seL4"
      val camkesAuxCodeDir = cDir / "camkes_aux_code"
      val isoletteAppsDir = appsDir / "isolette_sel4"

      ret = run(o(
        platform = Cli.HamrPlatform.SeL4,
        camkesOutputDir = Some(camkesOutputDir.value),
        camkesAuxCodeDirs = ISZ(camkesAuxCodeDir.value)
      ))

      if (ret == 0) {
        camkesOutputDir.copyOverTo(isoletteAppsDir)
        println(s"Copied CAmkES to code file:///${isoletteAppsDir.value}/")

        val ext_c = camkesOutputDir / "hamr/ext/ext.c"
        val ext_c_camkes = isoletteAppsDir / "hamr/ext/ext.c"
        if (ext_c_camkes.exists) {
          ext_c_camkes.remove()
        }
        ext_c.copyOverTo(ext_c_camkes)
        println(s"Copied file:///${ext_c} to file:///${ext_c_camkes}")

        val ext_h = camkesOutputDir / "hamr/ext/ext.h"
        val ext_h_camkes = isoletteAppsDir / "hamr/ext/ext.h"
        if (ext_h_camkes.exists) {
          ext_h_camkes.remove()
        }
        ext_h.copyOverTo(ext_h_camkes)
        println(s"Copied file:///${ext_h} to file:///${ext_h_camkes}")
      }
    }
  }
}
