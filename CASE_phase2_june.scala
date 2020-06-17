package org.sireum.cli.hamr_runners

import org.sireum._

object CASE_phase2_june {

  ///home/vagrant/
  //val rootDir = Os.home / "devel/case/case-ta6-experimental-platform-models"
  //val rootDir = Os.home / "devel/case/case-ta6-experimental-platform-models"
  val rootDir = Os.home / "devel/case/CASETeam/examples/ksu-proprietary"
  ///Phase-2-UAV-Experimental-Platform-June-hamr_dataports

  def gen(name: String, json: String): (String, Os.Path, Os.Path) = {
    val modelDir = rootDir / name
    return (name, modelDir, modelDir / ".slang" / json)
  }

  val projectsDirs: ISZ[(String, Os.Path, Os.Path)] = ISZ(

    gen("Phase-2-UAV-Experimental-Platform-June-hamr", "SW_SW_Impl_Instance.json"),

    gen("Phase-2-UAV-Experimental-Platform-June-hamr_dataports", "SW_SW_Impl_Instance.json")
  )

  val platforms: ISZ[Cli.HamrPlatform.Type] = ISZ(
    //Cli.HamrPlatform.SeL4_TB,
    //Cli.HamrPlatform.SeL4_Only,
    Cli.HamrPlatform.SeL4
  )

  def main(args: Array[Predef.String]): Unit = {

    for (project <- projectsDirs) {
      val projectDir = project._2
      val slangFile = project._3

      if(!projectDir.exists) {
        throw new RuntimeException(s"${projectDir} does not exist");
      }

      val outputDir = projectDir / "june"

      for (platform <- platforms) {
        val camkesOutputDir = platform match {
          case Cli.HamrPlatform.SeL4_TB => outputDir / "src/c/CAmkES_seL4_TB_VM"
          case Cli.HamrPlatform.SeL4_Only => outputDir / "src/c/CAmkES_seL4_Only_VM"
          case Cli.HamrPlatform.SeL4 => outputDir / "src/c/CAmkES_seL4_VM"
          case _ => throw new RuntimeException("??")
        }

        outputDir.removeAll()

        val o = Util.o(
          args = ISZ(slangFile.value),
          platform = platform,

          outputDir = Some(outputDir.value),

          bitWidth = 32,
          maxStringSize = 256,
          maxArraySize = 1,

          camkesOutputDir = Some(camkesOutputDir.value),
          aadlRootDir = Some(projectDir.value)
        )

        cli.HAMR.codeGen(o)

      }
    }
  }
}