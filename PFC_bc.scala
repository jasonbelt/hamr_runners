package org.sireum.cli.hamr_runners

import org.sireum._

object PFC{

  def main(args: Array[Predef.String]): Unit = {
    val projectDir = Os.home / "temp/producer-filter-consumer"

    val aadlDir = projectDir
    val slang = aadlDir / ".slang/PFC_PFC_Sys_Impl_Instance.json"

    val projName = "pfc_project"

    val o = Util.o(
      args = ISZ(slang.value),

      packageName = Some(projName),

      devicesAsThreads = F,

      excludeComponentImpl = F,

      bitWidth = 32,
      maxStringSize = 256,
      maxArraySize = 8,

      aadlRootDir = Some(aadlDir.value),
    )

    def run(o: Cli.HamrCodeGenOption): Int = {
      val ret = cli.HAMR.codeGen(o)
      return ret
    }

    var INCLUDE = T
    var ret = 0

    { // BITCODEC stuff

      val srcDir = projectDir / "slang_embedded_bitcodec"
      val cDir = srcDir / "src/c"
      val slangAuxCodeDir = cDir / "ext-c"
      val slangAuxCodeDirs: ISZ[String] = ISZ(slangAuxCodeDir.value)
      val camkesOutputDir = cDir / "CAmkES_seL4"
      val camkesAuxCodeDir = cDir / "camkes_aux_code"

      val o_bitcodec_base = o(
        outputDir = Some(srcDir.value),
        slangAuxCodeDirs = slangAuxCodeDirs,
        slangOutputCDir = Some(cDir.value),
        camkesOutputDir = Some(camkesOutputDir.value),
        camkesAuxCodeDirs = ISZ(camkesAuxCodeDir.value)
      )

      INCLUDE = T
      if (ret == 0) {
        // JVM with bitcodec

        ret = run(o_bitcodec_base(platform = Cli.HamrPlatform.JVM))
      }

      INCLUDE = T
      if (ret == 0 && INCLUDE) {
        // C with bitcodec

        ret = run(o_bitcodec_base(platform = Cli.HamrPlatform.Linux))
        ret = run(o_bitcodec_base(platform = Cli.HamrPlatform.MacOS))
        ret = run(o_bitcodec_base(platform = Cli.HamrPlatform.Cygwin))
      }

      INCLUDE = T
      if (ret == 0 && INCLUDE) {
        // SeL4 with bitcodec

        ret = run(o_bitcodec_base(platform = Cli.HamrPlatform.SeL4))

        if (ret == 0) {
          val camkesAppsDir = Os.home / "CASE/camkes/projects/camkes/apps"
          val camkesProjectAppsDir = camkesAppsDir / s"${projName}_sel4"

          camkesProjectAppsDir.mklink(camkesOutputDir)

          println(s"Symlinked ${camkesOutputDir} to ${camkesProjectAppsDir}")
        }
      }
    }
  }
}