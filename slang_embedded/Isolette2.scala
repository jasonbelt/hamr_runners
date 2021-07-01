// #Sireum

package org.sireum.cli.hamr_runners.slang_embedded

import org.sireum.Cli.{HamrCodeGenOption, HamrPlatform}
import org.sireum._
import org.sireum.cli.hamr_runners.{DotFormat, ReadmeGenerator, ReadmeTemplate, Report}
import org.sireum.hamr.codegen.common.util.ExperimentalOptions
import org.sireum.message.Reporter

object Isolette2 extends App {

  val shouldReport: B = T
  val graphFormat: DotFormat.Type = DotFormat.svg
  val build: B = F
  val timeout: Z = 25000

  val jvm: HamrPlatform.Type = HamrPlatform.JVM
  val cygwin: HamrPlatform.Type = HamrPlatform.Cygwin
  val linux: HamrPlatform.Type = HamrPlatform.Linux
  val macos: HamrPlatform.Type = HamrPlatform.MacOS
  val sel4: HamrPlatform.Type = HamrPlatform.SeL4
  val sel4_tb: HamrPlatform.Type = HamrPlatform.SeL4_TB
  val sel4_only: HamrPlatform.Type = HamrPlatform.SeL4_Only

  val evaluation_dir: Os.Path = Os.home / "devel/slang-embedded/isolette"

  var experimentalOptions: ISZ[String] = ISZ(ExperimentalOptions.GENERATE_DOT_GRAPHS)

  def gen(name: String, json: String, platforms: ISZ[HamrPlatform.Type]): (String, Os.Path, Os.Path, ISZ[HamrPlatform.Type]) = {
    val modelDir = evaluation_dir / name
    val simpleName = Os.path(name).name // get last dir name
    return (simpleName, modelDir, modelDir / ".slang" / json, platforms)
  }

  val tests: ISZ[(String, Os.Path, Os.Path, ISZ[HamrPlatform.Type])] = ISZ(

    gen("src/aadl", "Isolette_isolette_single_sensor_Instance.json",
      ISZ(jvm, linux, macos, cygwin, sel4)),

  )

  def run(): Unit = {
    val reporter = Reporter.create

    for (project <- tests) {
      val projectDir = project._2
      val slangFile = project._3

      var reports: HashSMap[HamrPlatform.Type, Report] = HashSMap.empty

      if(!projectDir.exists) {
        halt(s"${projectDir} does not exist");
      }
      for (platform <- project._4) {

        println("***************************************")
        println(s"${projectDir} -- ${platform})")
        println("***************************************")

        val outputDir: Os.Path = evaluation_dir

        val camkesOutputDir: Os.Path = evaluation_dir / "src" / "c" / "CAmkES_seL4"

        //outputDir.removeAll()

        val o = HamrCodeGenOption(
          help = "",
          args = ISZ(slangFile.value),
          msgpack = F,
          verbose = T,
          platform = platform,

          packageName = Some("isolette"),
          noEmbedArt = F,
          devicesAsThreads = F,
          excludeComponentImpl = F,

          bitWidth = 32,
          maxStringSize = 250,
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
              readmeDir  = evaluation_dir,
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
        val readme = evaluation_dir / "readme_autogen.md"
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

  def isSel4(platform: HamrPlatform.Type): B = {
    val ret: B = platform match {
      case HamrPlatform.SeL4 => T
      case HamrPlatform.SeL4_TB => T
      case HamrPlatform.SeL4_Only => T
      case _ => F
    }
    return ret
  }

  override def main(args: ISZ[String]): Z = {
    run()
    return 0
  }

}