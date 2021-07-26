package org.sireum.cli.hamr_runners.casex

import org.sireum._

object PFC_Periodic{

  val bs = "\\"

  def main(args: Array[Predef.String]): Unit = {
    val projectDir = Os.home / "temp/producer-filter-consumer/periodic"

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

    def run(o: Cli.SireumHamrCodegenOption): Int = {
      val ret = cli.HAMR.codeGen(o)
      return ret.toInt
    }

    var INCLUDE = T
    var ret = 0

    val nonbitcodec: ST = st"""
 _   _                        ____  _ _      _____          _
| ${bs} | |                      |  _ ${bs}(_) |    / ____|        | |
|  ${bs}| | ___  _ __    ______  | |_) |_| |_  | |     ___   __| | ___  ___
| . ` |/ _ ${bs}| '_ ${bs}  |______| |  _ <| | __| | |    / _ ${bs} / _` |/ _ ${bs}/ __|
| |${bs}  | (_) | | | |          | |_) | | |_  | |___| (_) | (_| |  __/ (__
|_| ${bs}_|${bs}___/|_| |_|          |____/|_|${bs}__|  ${bs}_____${bs}___/ ${bs}__,_|${bs}___|${bs}___|
"""
    for(i <- 0 until 100) {
      println(nonbitcodec.render)
    }

    { // REGULAR

      val srcDir = projectDir / "slang_embedded"
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

        ret = run(o_bitcodec_base(platform = Cli.SireumHamrCodegenHamrPlatform.JVM))
      }

      INCLUDE = T

      if (ret == 0 && INCLUDE) {
        // C with bitcodec

        ret = run(o_bitcodec_base(platform = Cli.SireumHamrCodegenHamrPlatform.Linux))
        ret = run(o_bitcodec_base(platform = Cli.SireumHamrCodegenHamrPlatform.MacOS))
        ret = run(o_bitcodec_base(platform = Cli.SireumHamrCodegenHamrPlatform.Cygwin))
      }

      INCLUDE = T
      if (ret == 0 && INCLUDE) {
        // SeL4 with bitcodec

        ret = run(o_bitcodec_base(platform = Cli.SireumHamrCodegenHamrPlatform.SeL4))

        if (ret == 0) {
          val camkesAppsDir = Os.home / "CASE/camkes/projects/camkes/apps"
          val camkesProjectAppsDir = camkesAppsDir / s"${projName}_sel4"

          camkesProjectAppsDir.mklink(camkesOutputDir)

          println(s"Symlinked ${camkesOutputDir} to ${camkesProjectAppsDir}")
        }
      }
    }




    val nonbitcodecexcludes: ST = st"""
 _   _             ____   _____       ______          _           _
| ${bs} | |           |  _ ${bs} / ____|     |  ____|        | |         | |
|  ${bs}| | ___       | |_) | |          | |__  __  _____| |_   _  __| | ___  ___
| . ` |/ _ ${bs}      |  _ <| |          |  __| ${bs} ${bs}/ / __| | | | |/ _` |/ _ ${bs}/ __|
| |${bs}  | (_) |     | |_) | |____      | |____ >  < (__| | |_| | (_| |  __/${bs}__ ${bs}
|_| ${bs}_|${bs}___/      |____/ ${bs}_____|     |______/_/${bs}_${bs}___|_|${bs}__,_|${bs}__,_|${bs}___||___/
"""
    for(i <- 0 until 100) {
      println(nonbitcodecexcludes.render)
    }

    { // REGULAR

      val srcDir = projectDir / "slang_embedded_excludes"
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
        camkesAuxCodeDirs = ISZ(camkesAuxCodeDir.value),

        excludeComponentImpl = T
      )

      INCLUDE = T
      if (ret == 0) {
        // JVM with bitcodec

        ret = run(o_bitcodec_base(platform = Cli.SireumHamrCodegenHamrPlatform.JVM))
      }

      INCLUDE = T

      if (ret == 0 && INCLUDE) {
        // C with bitcodec

        ret = run(o_bitcodec_base(platform = Cli.SireumHamrCodegenHamrPlatform.Linux))
        ret = run(o_bitcodec_base(platform = Cli.SireumHamrCodegenHamrPlatform.MacOS))
        ret = run(o_bitcodec_base(platform = Cli.SireumHamrCodegenHamrPlatform.Cygwin))
      }

      INCLUDE = T
      if (ret == 0 && INCLUDE) {
        // SeL4 with bitcodec

        ret = run(o_bitcodec_base(platform = Cli.SireumHamrCodegenHamrPlatform.SeL4))

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