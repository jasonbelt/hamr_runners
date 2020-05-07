package org.sireum.cli.hamr_runners

import org.sireum._

object PFC{

  def main(args: Array[Predef.String]): Unit = {
    val projectDir = Os.home / "temp/producer-filter-consumer"

    val aadlDir = projectDir
    val slang = aadlDir / ".slang/PFC_PFC_Sys_Impl_Instance.json"

    val srcDir = projectDir / "slang"
    val cDir = srcDir / "src/c"

    val projName = "pfc_project"

    val slangAuxCodeDir = projectDir / "slang/src/c/ext-c"
    val slangAuxCodeDirs: ISZ[String] = ISZ(slangAuxCodeDir.value)

    val o = Util.o(
      args = ISZ(slang.value),

      packageName = Some(projName),

      devicesAsThreads = F,
      outputDir = Some(srcDir.value),
      excludeComponentImpl = F,

      bitWidth = 32,
      maxStringSize = 256,
      maxArraySize = 8,

      slangAuxCodeDirs = slangAuxCodeDirs,

      slangOutputCDir = Some(cDir.value),
      aadlRootDir = Some(aadlDir.value),
    )

    def run(o: Cli.HamrCodeGenOption): Int = {
      val ret = cli.HAMR.codeGen(o)
      return ret
    }

    var INCLUDE = T
    var ret = 0

    if(ret == 0) {
      ret = run(o(
        platform = Cli.HamrPlatform.JVM,
      ))
    }

    if(ret == 0 && INCLUDE) {
      // C targets

      ret = run(o(platform = Cli.HamrPlatform.Linux))
      ret = run(o(platform = Cli.HamrPlatform.MacOS))
      ret = run(o(platform = Cli.HamrPlatform.Cygwin))
    }

    INCLUDE = F
    if(ret == 0 && INCLUDE) {
      // C targets but EXCLUDES IMPL

      val slangDir = projectDir / "slang-excludes"
      val cDir = slangDir / "src/c"
      val slangAuxCodeDir = cDir / "ext-c"

      val o_excludes = o (
        excludeComponentImpl = T,

        outputDir = Some(slangDir.value),
        slangOutputCDir = Some(cDir.value),
        slangAuxCodeDirs = ISZ(slangAuxCodeDir.value)
      )

      ret = run(o_excludes(platform = Cli.HamrPlatform.Linux))
      ret = run(o_excludes(platform = Cli.HamrPlatform.MacOS))
      ret = run(o_excludes(platform = Cli.HamrPlatform.Cygwin))
    }

    INCLUDE = T
    if(ret == 0 && INCLUDE) {
      // ARSIT + ACT

      val camkesOutputDir = cDir / "CAmkES_seL4"
      val camkesAuxCodeDir = cDir / "camkes_aux_code"

      ret = run(o(
        platform = Cli.HamrPlatform.SeL4,

        camkesOutputDir = Some(camkesOutputDir.value),
        camkesAuxCodeDirs = ISZ(camkesAuxCodeDir.value)
      ))

      if (ret == 0) {
        val camkesAppsDir = Os.home / "CASE/camkes/projects/camkes/apps"
        val camkesProjectAppsDir = camkesAppsDir / s"${projName}_sel4"

        camkesProjectAppsDir.mklink(camkesOutputDir)

        println(s"Symlinked ${camkesOutputDir} to ${camkesProjectAppsDir}")
      }
    }
  }
}