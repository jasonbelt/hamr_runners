package org.sireum.cli.hamr_runners.casex

import org.sireum._

object CaseTool_Junaid {

  val rootDir = Os.home / "devel/case/CASETeam/examples/phase2-integration-examples/component-integration-examples"

  def gen(name: String, json: String): (String, Os.Path, Os.Path) = {
    val modelDir = rootDir / name
    return (name, modelDir, modelDir / ".slang" / json)
  }

  val projectsDirs: ISZ[(String, Os.Path, Os.Path)] = ISZ(
    /*
    gen("simple_uav", "UAV_UAV_Impl_Instance.json"),
    gen("test_data_port", "test_data_port_top_impl_Instance.json"),
    gen("test_data_port_periodic", "test_data_port_periodic_top_impl_Instance.json"),
    gen("test_data_port_periodic_fan_out", "test_data_port_periodic_fan_out_top_impl_Instance.json"),
    gen("test_event_data_port", "test_event_data_port_top_impl_Instance.json"),
    gen("test_event_data_port_fan_out", "test_event_data_port_fan_out_top_impl_Instance.json"),
    gen("test_event_port", "test_event_port_top_impl_Instance.json"),
    gen("test_event_port_fan_out", "test_event_port_fan_out_top_impl_Instance.json")
    */
    gen("vm-uxas-wpm-hamr", "SysContext_top_Impl_Instance.json"),
  )

  val platforms: ISZ[Cli.HamrPlatform.Type] = ISZ(
    Cli.HamrPlatform.Linux,
    Cli.HamrPlatform.MacOS,
    Cli.HamrPlatform.SeL4,
    //Cli.HamrPlatform.SeL4_TB,
    //Cli.HamrPlatform.SeL4_Only,
    //Cli.HamrPlatform.SeL4
  )

  def main(args: Array[Predef.String]): Unit = {

    for (project <- projectsDirs) {
      val projectDir = project._2
      val slangFile = project._3

      if(!projectDir.exists) {
        throw new RuntimeException(s"${projectDir} does not exist");
      }

      val excludes: B = T
      val dir = if(excludes) "hamr_excludes" else "hamr"

      val outputDir = projectDir / dir

      for (platform <- platforms) {
        val camkesOutputDir: Option[Os.Path] = platform match {
          case Cli.HamrPlatform.SeL4_TB => Some(outputDir / "src/c/CAmkES_seL4_TB_VM")
          case Cli.HamrPlatform.SeL4_Only => Some(outputDir / "src/c/CAmkES_seL4_Only_VM")
          case Cli.HamrPlatform.SeL4 => Some(outputDir / "src/c/CAmkES_seL4_VM")
          case _ => None()
        }

        if(camkesOutputDir.nonEmpty) {
          camkesOutputDir.get.removeAll()
        }

        val o = Util.o(
          args = ISZ(slangFile.value),
          platform = platform,

          packageName = Some("base"),
          outputDir = Some(outputDir.value),

          excludeComponentImpl = excludes,

          camkesOutputDir = if(camkesOutputDir.isEmpty) None() else Some(camkesOutputDir.get.value),
          aadlRootDir = Some(projectDir.value)
        )

        cli.HAMR.codeGen(o)

        if(camkesOutputDir.nonEmpty) {

          val dot = camkesOutputDir.get / "graph.dot"
          val parent = dot.up.up

          val tool_eval_4_diagrams = camkesOutputDir.get / "diagrams"

          tool_eval_4_diagrams.mkdir()

          val dotPDFOutput = tool_eval_4_diagrams / s"CAmkES-arch-${platform}.pdf"
          val dotPNGOutput = tool_eval_4_diagrams / s"CAmkES-arch-${platform}.png"

          val proc:ISZ[String] = ISZ("dot", "-Tpdf", dot.canon.value, "-o", dotPDFOutput.canon.value)
          Os.proc(proc).console.run()

          val proc2:ISZ[String] = ISZ("dot", "-Tpng", dot.canon.value, "-o", dotPNGOutput.canon.value)
          Os.proc(proc2).console.run()

          val readme = camkesOutputDir.get / "readme.md"

          val aadlArch = "diagrams/aadl-arch.png"

          val sel4OnlyArchPDF = "diagrams/CAmkES-arch-SeL4_Only.pdf"
          val sel4OnlyArchPNG = "diagrams/CAmkES-arch-SeL4_Only.png"

          val sel4TBArchPDF = "diagrams/CAmkES-arch-SeL4_TB.pdf"
          val sel4TBArchPNG = "diagrams/CAmkES-arch-SeL4_TB.png"

          val readmest = st"""# ${project._1}
                             |
                             |## AADL Arch
                             |  ![aadl](${aadlArch})
                             |  
                             |## seL4_Only Arch
                             |  [pdf-version](${sel4OnlyArchPDF})
                             |  ![SeL4_Only](${sel4OnlyArchPNG})
                             |
                             |## seL4_TB Arch
                             |  [pdf-version](${sel4TBArchPDF})
                             |  ![SeL4_TB](${sel4TBArchPNG})
                             |"""

          readme.writeOver(readmest.render)
        }
      }
    }
  }
}