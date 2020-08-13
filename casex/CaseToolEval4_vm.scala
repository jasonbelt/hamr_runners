// #Sireum

package org.sireum.cli.hamr_runners.casex

import org.sireum.Cli.HamrPlatform
import org.sireum._
import org.sireum.cli.hamr_runners.{DotFormat, ReadmeGenerator, ReadmeTemplate, Report}
import org.sireum.hamr.codegen.common.util.ExperimentalOptions
import org.sireum.message.Reporter

object CaseToolEval4_vm extends App {

  val shouldReport: B = T
  val graphFormat: DotFormat.Type = DotFormat.svg
  val build: B = F
  val timeout: Z = 15000

  val sel4: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4
  val sel4_tb: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_TB
  val sel4_only: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_Only

  val case_tool_evaluation_dir: Os.Path = Os.home / "devel/case/CASE-loonwerks/TA5/tool-evaluation-4/HAMR/examples"

  var experimentalOptions: ISZ[String] = ISZ(ExperimentalOptions.GENERATE_DOT_GRAPHS)

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

    // VMs
    gen("test_data_port_periodic_domains_VM/both_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
    gen("test_data_port_periodic_domains_VM/receiver_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
    gen("test_data_port_periodic_domains_VM/sender_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),

    gen("test_event_data_port_periodic_domains_VM/both_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
    gen("test_event_data_port_periodic_domains_VM/receiver_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
    gen("test_event_data_port_periodic_domains_VM/sender_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
 */

    //gen("test_event_data_port_periodic_domains_VMx/receiver_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only)),

    gen("test_event_data_port_periodic_domains_VMx/sender_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only)),

    //gen("test_event_data_port_fan_ins_outs_periodic_domains", "test_event_data_port_fan_ins_outs_periodic_domains_top_impl_Instance.json", ISZ(sel4_only))
  )

  def run(): Unit = {
    val reporter = Reporter.create

    for (project <- tests) {
      val projectDir = project._2
      val slangFile = project._3

      var reports: HashSMap[Cli.HamrPlatform.Type, Report] = HashSMap.empty

      if(!projectDir.exists) {
        halt(s"${projectDir} does not exist");
      }
      for (platform <- project._4) {

        println("***************************************")
        println(s"${projectDir} -- ${platform})")
        println("***************************************")

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

        if(ops.StringOps(project._2.value).contains("VMx")) {
          experimentalOptions = experimentalOptions :+ ExperimentalOptions.USE_CASE_CONNECTORS
        }

        outputDir.removeAll()

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
          runTranspiler = build,

          slangAuxCodeDirs = ISZ(),
          slangOutputCDir = None(),
          outputDir = Some(outputDir.value),

          camkesOutputDir = Some(camkesOutputDir.value),
          camkesAuxCodeDirs = ISZ(),
          aadlRootDir = Some(projectDir.value),

          experimentalOptions = experimentalOptions
        )

        org.sireum.cli.HAMR.codeGen(o)

        if(shouldReport && isSel4(platform)) {
          val gen = ReadmeGenerator(o, reporter)

          if(gen.build()) {
            val expectedOutput = gen.simulate(timeout)

            val report = Report(
              options = o,
              timeout = timeout,
              runInstructions = gen.genRunInstructions(case_tool_evaluation_dir),
              expectedOutput = Some(expectedOutput),
              aadlArchDiagram = gen.getAadlArchDiagram(),
              hamrCamkesArchDiagram = gen.getHamrCamkesArchDiagram(graphFormat),
              camkesArchDiagram = gen.getCamkesArchDiagram(graphFormat)
            )

            reports = reports + (platform ~> report)
          }
        }
      }

      if(shouldReport) {
        val readme = projectDir / "readme_autogen.md"
        val readmest = ReadmeTemplate.generateReport(project._1, reports)
        readme.writeOver(readmest.render)
      }
    }

    if(reporter.hasError) {
      eprintln(s"Reporter Errors:")
      reporter.printMessages()
    }
  }

  def delStaleDiagrams(tool_eval_4_diagrams: Os.Path, fname: String): Unit = {
    for(format <- ISZ("png", "gif", "jpg", "svg", "pdf")) {
      val f = tool_eval_4_diagrams / s"${fname}.${format}"
      if(f.exists) {
        f.remove()
      }
    }
  }

  override def main(args: ISZ[String]): Z = {
    run()
    return 0
  }


  def isSel4(platform: HamrPlatform.Type): B = {
    return platform match {
      case HamrPlatform.SeL4 => T
      case HamrPlatform.SeL4_TB => T
      case HamrPlatform.SeL4_Only => T
      case _ => F
    }
  }

}