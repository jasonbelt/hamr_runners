package org.sireum.cli.hamr_runners

import org.sireum._
import org.sireum.cli._

object BuildingControlRunner {
  val JVM = Cli.HamrPlatform.JVM
  val Linux = Cli.HamrPlatform.Linux
  val MacOS = Cli.HamrPlatform.MacOS
  val Cygwin = Cli.HamrPlatform.Cygwin
  val SeL4 = Cli.HamrPlatform.SeL4

  def main(args: Array[Predef.String]): Unit = {
    val rootDir = Os.home / "devel/slang-embedded/slang-embedded-building-control"

    val projects: ISZ[(String, String, ISZ[Cli.HamrPlatform.Type])] = ISZ(
      ("building-control-gen", "BuildingControl_BuildingControlDemo_i_Instance.json",               ISZ(JVM, Linux, MacOS, Cygwin)),

      ("building-control-gen-alarm-ui", "BuildingControl_BuildingControlDemo_i_Instance.json",      ISZ(JVM)),

      ("building-control-gen-mixed-ui", "BuildingControl_BuildingControlDemo_i_Instance.json",          ISZ(JVM)),

      ("building-control-gen-shm", "BuildingControl_BuildingControlDemo_i_Instance.json",           ISZ(JVM, Linux, MacOS, Cygwin)),

      ("building-control-gen-shm-libART", "BuildingControl_BuildingControlDemo_i_Instance.json", ISZ(JVM)),

      ("building-control-gen-mixed", "BuildingControl_BuildingControlDemo_i_Instance.json",             ISZ(JVM, SeL4, Linux, MacOS, Cygwin)),  // FIXME transpiler issue  
      
      ("building-control-gen-periodic", "BuildingControl_periodic_BuildingControlDemo_i_Instance.json", ISZ(JVM, Linux, MacOS, Cygwin)), // FIXME transpiler issue

      // ("building-control-mixed-excludes", "BuildingControl_BuildingControlDemo_i_Instance.json",    ISZ(JVM, Linux, MacOS, Cygwin)),   // TODO - do we really want to update this one?
    )

    for (project <- projects) {
      val projectDir = rootDir / project._1
      val cDir = projectDir / "src/c"
      val aadlDir = projectDir / "src/aadl"
      val slangFile = aadlDir / s".slang/${project._2}"

      if(!projectDir.exists) {
        throw new RuntimeException(s"${projectDir} does not exist");
      }

      for (platform <- project._3) {
        val camkesOutputDir: Option[Os.Path] = platform match {
          case Cli.HamrPlatform.SeL4_TB => Some(cDir / "CAmkES_seL4_TB")
          case Cli.HamrPlatform.SeL4_Only => Some(cDir / "CAmkES_seL4_Only")
          case Cli.HamrPlatform.SeL4 => Some(cDir / "CAmkES_seL4")
          case _ => None()
        }

        val o = Util.o(
          args = ISZ(slangFile.value),

          outputDir = Some(projectDir.value),

          platform = platform,

          excludeComponentImpl = F,
          devicesAsThreads = T,

          //bitWidth = 32,
          //maxStringSize = 125,
          //maxArraySize = 16,

          embedArt = if(project._1 == string"building-control-gen-shm-libART") F else T,

          camkesOutputDir = camkesOutputDir.map(m => m.value),
          camkesAuxCodeDirs = ISZ((cDir / "camkes_aux_code").value),
          aadlRootDir = Some(aadlDir.value),
        )

        cli.HAMR.codeGen(o)
      }
    }
  }
}