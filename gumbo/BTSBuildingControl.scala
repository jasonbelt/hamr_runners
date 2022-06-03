package org.sireum.cli.hamr_runners.gumbo

import org.sireum.cli.hamr_runners.Util
import org.sireum.cli.hamr_runners.slang_embedded.Project
import org.sireum.message.Reporter
import org.sireum.{Cli, F, ISZ, None, Option, Os, Some, T, cli}

object BTSBuildingControl {
  val JVM = Cli.SireumHamrCodegenHamrPlatform.JVM
  val Linux = Cli.SireumHamrCodegenHamrPlatform.Linux
  val MacOS = Cli.SireumHamrCodegenHamrPlatform.MacOS
  val Cygwin = Cli.SireumHamrCodegenHamrPlatform.Cygwin
  val SeL4 = Cli.SireumHamrCodegenHamrPlatform.SeL4

  val DEL_OUTPUT_DIR = T;

  def main(args: Array[Predef.String]): Unit = {
    val rootDir = Os.home / "temp"

    var projects: ISZ[Project] = ISZ(
      Project("building-control-bless", "BuildingControl_BA_BuildingControlDemo_i_Instance.json", None(),ISZ(JVM)),
    )

    for (project <- projects) {
      val projectDir = rootDir / project.name
      val hamrOutDir = projectDir / "hamr_bts"
      val cDir = hamrOutDir / "src" / "c"
      val aadlDir = projectDir / "aadl"
      val slangFile = aadlDir / ".slang" / project.json

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

        /*
        if(DEL_OUTPUT_DIR && hamrOutDir.exists) {
          val bridges = hamrOutDir / "src" / "main" / "bridge"
          val components = hamrOutDir / "src" / "main" / "component"
          if(bridges.exists) {
            bridges.removeAll()
          }
          if(components.exists){
            components.removeAll()
          }
        }
        */

        val o = Util.o(
          args = ISZ(slangFile.value),

          packageName = project.basePackage,

          outputDir = Some(hamrOutDir.value),

          platform = platform,

          excludeComponentImpl = F,
          devicesAsThreads = T,

          //bitWidth = 32,
          //maxStringSize = 125,
          //maxArraySize = 16,

          noEmbedArt = F,

          camkesOutputDir = camkesOutputDir.map(m => m.value),
          camkesAuxCodeDirs = ISZ((cDir / "camkes_aux_code").value),
          aadlRootDir = Some(aadlDir.value),

          experimentalOptions = ISZ("PROCESS_BTS_NODES")
        )

        val reporter = Reporter.create
        cli.HAMR.codeGen(o, reporter)
      }
    }
  }
}
