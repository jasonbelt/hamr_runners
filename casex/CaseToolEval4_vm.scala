// #Sireum

package org.sireum.cli.hamr_runners.casex

import org.sireum._
import org.sireum.hamr.codegen.CodeGenPlatform

object CaseToolEval4_vm extends App {

  val sel4: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4
  val sel4_tb: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_TB
  val sel4_only: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_Only

  val case_tool_evaluation_dir: Os.Path = Os.home / "devel/case/CASE-loonwerks/TA5/tool-evaluation-4/HAMR/examples"

  def gen(name: String, json: String, platforms: ISZ[Cli.HamrPlatform.Type]): (String, Os.Path, Os.Path, ISZ[Cli.HamrPlatform.Type]) = {
    val modelDir = case_tool_evaluation_dir / name
    val simpleName = Os.path(name).name // get last dir name
    return (simpleName, modelDir, modelDir / ".slang" / json, platforms)
  }

  val tests: ISZ[(String, Os.Path, Os.Path, ISZ[Cli.HamrPlatform.Type])] = ISZ(
    /*
    gen("simple_uav", "UAV_UAV_Impl_Instance.json", ISZ(sel4_tb, sel4_only)),

    gen("test_data_port", "test_data_port_top_impl_Instance.json", ISZ(sel4_tb, sel4_only)),
    gen("test_data_port_periodic", "test_data_port_periodic_top_impl_Instance.json", ISZ(sel4_tb, sel4_only)),
    gen("test_data_port_periodic_fan_out", "test_data_port_periodic_fan_out_top_impl_Instance.json", ISZ(sel4_tb, sel4_only)),

    gen("test_event_data_port", "test_event_data_port_top_impl_Instance.json", ISZ(sel4_tb, sel4_only)),
    gen("test_event_data_port_fan_out", "test_event_data_port_fan_out_top_impl_Instance.json", ISZ(sel4_tb, sel4_only)),

    gen("test_event_port", "test_event_port_top_impl_Instance.json", ISZ(sel4_tb, sel4_only)),
    gen("test_event_port_fan_out", "test_event_port_fan_out_top_impl_Instance.json", ISZ(sel4_tb, sel4_only)),

    gen("test_data_port_periodic_domains", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_tb, sel4_only, sel4)),
    gen("test_event_data_port_periodic_domains", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_tb, sel4_only, sel4)),
    gen("test_event_port_periodic_domains", "test_event_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_tb, sel4_only, sel4)),
    */

    // VMs
    gen("test_data_port_periodic_domains_VM/both_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
    gen("test_data_port_periodic_domains_VM/receiver_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
    gen("test_data_port_periodic_domains_VM/sender_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),

    //gen("test_event_data_port_periodic_domains_VM/both_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
    //gen("test_event_data_port_periodic_domains_VM/receiver_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
    //gen("test_event_data_port_periodic_domains_VM/sender_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
  )

  def run(): Unit = {

    for (project <- tests) {
      val projectDir = project._2
      val slangFile = project._3

      if(!projectDir.exists) {
        halt(s"${projectDir} does not exist");
      }

      var readmeEntries: ISZ[ST] = ISZ()

      for (platform <- project._4) {

        val outputDir: Os.Path = platform match {
          case Cli.HamrPlatform.SeL4_TB => projectDir / "CAmkES_seL4_TB"
          case Cli.HamrPlatform.SeL4_Only => projectDir / "CAmkES_seL4_Only"
          case Cli.HamrPlatform.SeL4 => projectDir / "CAmkES_seL4"
          case _ => halt("??")
        }

        val camkesOutputDir: Os.Path = platform match {
          case Cli.HamrPlatform.SeL4_TB => outputDir
          case Cli.HamrPlatform.SeL4_Only => outputDir
          case Cli.HamrPlatform.SeL4 => outputDir / "src/c/CAmkES_seL4"
          case _ => halt("??")
        }

        //outputDir.removeAll()

        val o = Cli.HamrCodeGenOption(
          help = "",
          args = ISZ(slangFile.value),
          json = T,
          verbose = T,
          platform = platform,

          packageName = Some(project._1),
          embedArt = T,
          devicesAsThreads = F,
          excludeComponentImpl = T,

          bitWidth = 32,
          maxStringSize = 256,
          maxArraySize = 1,
          runTranspiler = F,

          slangAuxCodeDirs = ISZ(),
          slangOutputCDir = None(),
          outputDir = Some(outputDir.value),

          camkesOutputDir = Some(camkesOutputDir.value),
          camkesAuxCodeDirs = ISZ(),
          aadlRootDir = Some(projectDir.value)
        )

        org.sireum.cli.HAMR.codeGen(o)

        val dot = camkesOutputDir / "graph.dot"

        if(dot.exists) {

          val tool_eval_4_diagrams = projectDir / "diagrams"

          val png = s"CAmkES-arch-${platform}.png"

          //val dotPDFOutput = tool_eval_4_diagrams / s"CAmkES-arch-${platform}.pdf"
          val dotPNGOutput = tool_eval_4_diagrams / png

          //val proc:ISZ[String] = ISZ("dot", "-Tpdf", dot.canon.value, "-o", dotPDFOutput.canon.value)
          //Os.proc(proc).run()

          val proc2:ISZ[String] = ISZ("dot", "-Tpng", dot.canon.value, "-o", dotPNGOutput.canon.value)
          Os.proc(proc2).run()

          //val sel4OnlyArchPDF = "diagrams/CAmkES-arch-SeL4_Only.pdf"
          val readmePath = s"diagrams/${png}"

          readmeEntries = readmeEntries :+ st"""## ${platform} Arch
                                               |  ![${platform}](${readmePath})"""
        }
      }

      val readme = projectDir / "readme_autogen.md"

      val aadlArch = "diagrams/aadl-arch.png"

      val readmest = st"""# ${project._1}
                         |
                         |## AADL Arch
                         |  ![aadl](${aadlArch})
                         |
                         |${(readmeEntries, "\n\n")}
                         |"""

      readme.writeOver(readmest.render)
    }
  }

  override def main(args: ISZ[String]): Z = {
    run()
    return 0
  }
}