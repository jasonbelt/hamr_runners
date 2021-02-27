// #Sireum
package org.sireum.cli.hamr_runners.casex

import org.sireum._
import org.sireum.cli.hamr_runners.{DotFormat, ReadmeGenerator, ReadmeTemplate, Report, RunType}
import org.sireum.message.Reporter

object Attestation_Gate extends App {

  val shouldReport: B = T
  val build: RunType.Type = RunType.build_simulate
  val timeout: Z = 18000
  val graphFormat: DotFormat.Type = DotFormat.svg

  val sel4: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4
  val sel4_tb: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_TB
  val sel4_only: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_Only

  val case_tool_evaluation_dir: Os.Path = Os.home / "devel/case/CASETeam/examples/ksu-proprietary"

  def gen(name: String, json: String, platforms: ISZ[Cli.HamrPlatform.Type]): (String, Os.Path, Os.Path, ISZ[Cli.HamrPlatform.Type]) = {
    val modelDir = case_tool_evaluation_dir / name
    val simpleName = Os.path(name).name // get last dir name
    return (simpleName, modelDir, modelDir / ".slang" / json, platforms)
  }

  val tests: ISZ[(String, Os.Path, Os.Path, ISZ[Cli.HamrPlatform.Type])] = ISZ(

    gen("attestation-gate", "SysContext_top_Impl_Instance.json", ISZ(sel4)),

  )

  def run(): Unit = {

    val reporter = Reporter.create

    for (project <- tests) {
      val projectDir = project._2
      val slangFile = project._3

      println("***************************************")
      println(projectDir)
      println("***************************************")

      if(!projectDir.exists) {
        halt(s"${projectDir} does not exist");
      }

      var reports: HashSMap[Cli.HamrPlatform.Type, Report] = HashSMap.empty

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
          msgpack = F,
          verbose = T,
          platform = platform,

          packageName = Some(project._1),
          noEmbedArt = F,
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
          aadlRootDir = Some(projectDir.value),
          experimentalOptions = ISZ()
        )

        org.sireum.cli.HAMR.codeGen(o)

        if(shouldReport) {

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
}