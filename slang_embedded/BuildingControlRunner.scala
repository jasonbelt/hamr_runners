package org.sireum.cli.hamr_runners.slang_embedded

import org.sireum._
import org.sireum.cli.hamr_runners.Util

case class Project (name: String,
                    json: String,
                    basePackage: Option[String],
                    platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type])

object BuildingControlRunner {
  val JVM = Cli.SireumHamrCodegenHamrPlatform.JVM
  val Linux = Cli.SireumHamrCodegenHamrPlatform.Linux
  val MacOS = Cli.SireumHamrCodegenHamrPlatform.MacOS
  val Cygwin = Cli.SireumHamrCodegenHamrPlatform.Cygwin
  val SeL4 = Cli.SireumHamrCodegenHamrPlatform.SeL4

  def main(args: Array[Predef.String]): Unit = {
    val rootDir = Os.home / "devel/slang-embedded/slang-embedded-building-control"

    var projects: ISZ[Project] = ISZ(
      Project("building-control-gen", "BuildingControl_BuildingControlDemo_i_Instance.json", None(),ISZ(JVM, Linux, MacOS, Cygwin)),

      Project("building-control-gen-alarm-ui", "BuildingControl_BuildingControlDemo_i_Instance.json", None(), ISZ(JVM)),

      Project("building-control-gen-mixed-ui", "BuildingControl_BuildingControlDemo_i_Instance.json", None(), ISZ(JVM)),

      Project("building-control-gen-shm", "BuildingControl_BuildingControlDemo_i_Instance.json", None(), ISZ(JVM, Linux, MacOS, Cygwin)),

      Project("building-control-gen-shm-libART", "BuildingControl_BuildingControlDemo_i_Instance.json", None(), ISZ(JVM)),

      Project("building-control-gen-mixed", "BuildingControl_BuildingControlDemo_i_Instance.json", None(), ISZ(JVM, SeL4, Linux, MacOS, Cygwin)),  // FIXME transpiler issue
      
      Project("building-control-gen-periodic", "BuildingControl_periodic_BuildingControlDemo_i_Instance.json", None(), ISZ(JVM, Linux, MacOS, Cygwin)), // FIXME transpiler issue

      // ("building-control-mixed-excludes", "BuildingControl_BuildingControlDemo_i_Instance.json",    ISZ(JVM, Linux, MacOS, Cygwin)),   // TODO - do we really want to update this one?
    )

    projects = ISZ(
      Project("building-control-gen-mixed-ui-gd", "BuildingControl_BuildingControlDemo_i_Instance.json", Some("bc"), ISZ(JVM)),
    )

    for (project <- projects) {
      val projectDir = rootDir / project.name
      val cDir = projectDir / "src/c"
      val aadlDir = projectDir / "src/aadl"
      val slangFile = aadlDir / s".slang/${project.json}"

      if(!projectDir.exists) {
        throw new RuntimeException(s"${projectDir} does not exist");
      }

      for (platform <- project.platforms) {
        val camkesOutputDir: Option[Os.Path] = platform match {
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_TB => Some(cDir / "CAmkES_seL4_TB")
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_Only => Some(cDir / "CAmkES_seL4_Only")
          case Cli.SireumHamrCodegenHamrPlatform.SeL4 => Some(cDir / "CAmkES_seL4")
          case _ => None()
        }

        val o = Util.o(
          args = ISZ(slangFile.value),

          packageName = project.basePackage,

          outputDir = Some(projectDir.value),

          platform = platform,

          excludeComponentImpl = F,
          devicesAsThreads = T,

          //bitWidth = 32,
          //maxStringSize = 125,
          //maxArraySize = 16,

          noEmbedArt = if(project.name == string"building-control-gen-shm-libART") T else F,

          camkesOutputDir = camkesOutputDir.map(m => m.value),
          camkesAuxCodeDirs = ISZ((cDir / "camkes_aux_code").value),
          aadlRootDir = Some(aadlDir.value),
        )

        cli.HAMR.codeGen(o)
      }
    }
  }
}