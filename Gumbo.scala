package org.sireum.cli.hamr_runners

import org.sireum._
import org.sireum.cli._
import org.sireum.hamr.codegen.common.util.ExperimentalOptions

object Gumbo {

  case class Project (basePackage: Option[String],
                      platforms: ISZ[Cli.HamrPlatform.Type],

                      projectDir: Os.Path,
                      aadlDir: Option[String],
                      json: String)

  val JVM = Cli.HamrPlatform.JVM
  val Linux = Cli.HamrPlatform.Linux
  val MacOS = Cli.HamrPlatform.MacOS
  val Cygwin = Cli.HamrPlatform.Cygwin
  val SeL4 = Cli.HamrPlatform.SeL4

  def main(args: Array[Predef.String]): Unit = {

    val gumboDir = Os.home / "devel" / "gumbo" / "gumbo-models"
    val buildingControlDir = gumboDir / "building-control"
    val sirfurDir = Os.home / "devel" / "surfur" / "models" / "aadl"

    var projects: ISZ[Project] = ISZ(
      Project(None(), ISZ(JVM), buildingControlDir / "building-control-ba-mixed", Some("aadl"), "BuildingControl_BuildingControlDemo_i_Instance.json"),
      //Project(None(), ISZ(JVM), buildingControlDir / "building-control-bless-mixed", Some("aadl"), "BuildingControl_Bless_BuildingControlDemo_i_Instance.json"),

      Project(None(), ISZ(JVM), sirfurDir / "RedundantSensors", None(), "SensorSystem_redundant_sensors_impl_Instance.json")
    )

    for (project <- projects) {

      val outputDir = project.projectDir / "hamr"

      val cDir = outputDir / "src" / "c"
      val aadlDir = if(project.aadlDir.isEmpty) project.projectDir else project.projectDir / project.aadlDir.get
      val slangFile = aadlDir / ".slang" / project.json

      if(!project.projectDir.exists) {
        throw new RuntimeException(s"${project.projectDir} does not exist");
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