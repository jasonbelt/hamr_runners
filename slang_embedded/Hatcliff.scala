// #Sireum

package org.sireum.cli.hamr_runners.slang_embedded

import org.sireum._
import org.sireum.cli.hamr_runners.{DotFormat, ReadmeGenerator, ReadmeTemplate, Report}
import org.sireum.hamr.codegen.common.util.ExperimentalOptions
import org.sireum.message.Reporter

object Hatcliff extends App {

  val shouldReport: B = F
  val graphFormat: DotFormat.Type = DotFormat.svg
  val build: B = F
  val timeout: Z = 15000

  val jvm: Cli.HamrPlatform.Type = Cli.HamrPlatform.JVM
  val sel4: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4
  val sel4_tb: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_TB
  val sel4_only: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_Only

  ///home/vagrant/devel/slang-embedded/HAMR-Examples/datatype-examples/aadl/.slang/DatatypesSystem_Sys_i_Instance.json
  val evaluation_dir: Os.Path = Os.home / "devel/slang-embedded/HAMR-Examples/datatype-examples"

  var experimentalOptions: ISZ[String] = ISZ(ExperimentalOptions.GENERATE_DOT_GRAPHS)

  def gen(name: String, json: String, platforms: ISZ[Cli.HamrPlatform.Type]): (String, Os.Path, Os.Path, ISZ[Cli.HamrPlatform.Type]) = {
    val modelDir = evaluation_dir / name
    val simpleName = Os.path(name).name // get last dir name
    return (simpleName, modelDir, modelDir / ".slang" / json, platforms)
  }

  val tests: ISZ[(String, Os.Path, Os.Path, ISZ[Cli.HamrPlatform.Type])] = ISZ(

    gen("aadl", "DatatypesSystem_Sys_i_Instance.json", ISZ(jvm)),

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
          case Cli.HamrPlatform.JVM => projectDir.up / "hamr2"
          case Cli.HamrPlatform.SeL4_TB => projectDir / "CAmkES_seL4_TB"
          case Cli.HamrPlatform.SeL4_Only => projectDir / "CAmkES_seL4_Only"
          case Cli.HamrPlatform.SeL4 => projectDir / "CAmkES_seL4"
          case _ => halt("??")
        }

        val camkesOutputDir: Os.Path = platform match {
          case Cli.HamrPlatform.SeL4_TB => outputDir
          case Cli.HamrPlatform.SeL4_Only => outputDir
          case Cli.HamrPlatform.SeL4 => outputDir / "src/c/CAmkES_seL4"
          case _ => outputDir
        }

        if(ops.StringOps(project._2.value).contains("VMx")) {
          experimentalOptions = experimentalOptions :+ ExperimentalOptions.USE_CASE_CONNECTORS
        }

        outputDir.removeAll()

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

        if(shouldReport) {
          val gen = ReadmeGenerator(o, reporter)

          if(gen.build()) {
            val expectedOutput = gen.simulate(timeout)

            val report = Report(
              readmeDir = projectDir,
              options = o,
              runHamrScript = None(),
              timeout = timeout,
              runInstructions = gen.genRunInstructions(evaluation_dir, None()),
              expectedOutput = Some(expectedOutput),
              aadlArchDiagram = gen.getAadlArchDiagram(),
              hamrCamkesArchDiagram = gen.getHamrCamkesArchDiagram(graphFormat),
              camkesArchDiagram = gen.getCamkesArchDiagram(graphFormat),
              symbolTable = None()

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
}