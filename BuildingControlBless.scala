package org.sireum.cli.hamr_runners

import org.sireum._
import org.sireum.cli._
import org.sireum.hamr.codegen.common.util.ExperimentalOptions

object BuildingControlBless {

  case class Project (name: String,
                      json: String,
                      basePackage: Option[String],
                      platforms: ISZ[Cli.HamrPlatform.Type])

  val JVM = Cli.HamrPlatform.JVM
  val Linux = Cli.HamrPlatform.Linux
  val MacOS = Cli.HamrPlatform.MacOS
  val Cygwin = Cli.HamrPlatform.Cygwin
  val SeL4 = Cli.HamrPlatform.SeL4

  def main(args: Array[Predef.String]): Unit = {

    val rootDir = Os.home / "devel" / "gumbo" / "gumbo-models"

    var projects: ISZ[Project] = ISZ(
      Project("building-control/building-control-ba-mixed", "BuildingControl_BuildingControlDemo_i_Instance.json", None(), ISZ(JVM)),
    )

    for (project <- projects) {
      val projectDir = rootDir / project.name

      val outputDir = projectDir / "hamr"

      val cDir = outputDir / "src/c"
      val aadlDir = projectDir / "aadl"
      val slangFile = aadlDir / ".slang" / project.json

      if(!projectDir.exists) {
        throw new RuntimeException(s"${projectDir} does not exist");
      }

      for (platform <- project.platforms) {
        val camkesOutputDir: Option[Os.Path] = platform match {
          case Cli.HamrPlatform.SeL4_TB => Some(cDir / "CAmkES_seL4_TB")
          case Cli.HamrPlatform.SeL4_Only => Some(cDir / "CAmkES_seL4_Only")
          case Cli.HamrPlatform.SeL4 => Some(cDir / "CAmkES_seL4")
          case _ => None()
        }

        val o = Util.o(
          args = ISZ(slangFile.value),

          packageName = project.basePackage,

          outputDir = Some(outputDir.value),

          platform = platform,

          excludeComponentImpl = F,
          devicesAsThreads = T,

          bitWidth = 32,
          maxStringSize = 125,
          maxArraySize = 16,

          camkesOutputDir = camkesOutputDir.map(m => m.value),
          camkesAuxCodeDirs = ISZ((cDir / "camkes_aux_code").value),
          aadlRootDir = Some(aadlDir.value),

          experimentalOptions = ISZ(ExperimentalOptions.PROCESS_BTS_NODES)
        )

        cli.HAMR.codeGen(o)
      }
    }
  }
}