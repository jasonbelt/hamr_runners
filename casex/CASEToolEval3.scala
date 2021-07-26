package org.sireum.cli.hamr_runners.casex

import org.sireum._
import org.sireum.cli._

object CASEToolEval3 {

  val rootDir = Os.home / "devel/sel4/home/jab-CASE/TA5/tool-evaluation-3/HAMR/examples"

  val projectsDirs: ISZ[(String, String)] = ISZ(
    ("test_data_port", "test_data_port_top_impl_Instance.json"),
    ("test_data_port_periodic", "test_data_port_periodic_top_impl_Instance.json"),
    ("test_event_port", "test_event_port_top_impl_Instance.json"),
    ("test_event_data_port", "test_event_data_port_top_impl_Instance.json"),
    ("simple_uav", "UAV_UAV_Impl_Instance.json")
  )

  val platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type] = ISZ(Cli.SireumHamrCodegenHamrPlatform.SeL4_TB, Cli.SireumHamrCodegenHamrPlatform.SeL4_Only)

  def main(args: Array[Predef.String]): Unit = {
    for (project <- projectsDirs) {
      val projectDir = rootDir / project._1
      val slangFile = projectDir / s".slang/${project._2}"

      if(!projectDir.exists) {
        throw new RuntimeException(s"${projectDir} does not exist");
      }

      for (platform <- platforms) {
        val camkesOutputDir = platform match {
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_TB => projectDir / "CAmkES_seL4_TB"
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_Only => projectDir / "CAmkES_seL4_Only"
          case _ => throw new RuntimeException("??")
        }

        camkesOutputDir.removeAll()

        val o = Util.o(
          args = ISZ(slangFile.value),
          platform = platform,
          camkesOutputDir = Some(camkesOutputDir.value),
          aadlRootDir = Some(projectDir.value)
        )

        cli.HAMR.codeGen(o)
      }
    }
  }
}