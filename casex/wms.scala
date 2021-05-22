// #Sireum

package org.sireum.cli.hamr_runners.casex

import org.sireum._
import org.sireum.Cli.HamrPlatform
import org.sireum.cli.hamr_runners.{DotFormat, ReadmeGenerator, ReadmeTemplate, Report}
import org.sireum.hamr.codegen.common.util.ExperimentalOptions
import org.sireum.message.Reporter

@datatype class Projecty (simpleName: String,
                          basePackageName: String,

                          projectDir: Os.Path,

                          aadlDir: Os.Path,
                          slangFile: Os.Path,

                          rootOutputDir: Os.Path,

                          platforms: ISZ[Cli.HamrPlatform.Type],
                          shouldSimulate: B,
                          timeout: Z)

object WMS extends App {

  val shouldReport: B = F
  val graphFormat: DotFormat.Type = DotFormat.svg
  val build: B = F
  val defTimeout: Z = 15000

  val jvm: Cli.HamrPlatform.Type = Cli.HamrPlatform.JVM
  val linux: Cli.HamrPlatform.Type = Cli.HamrPlatform.Linux
  val sel4: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4
  val sel4_tb: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_TB
  val sel4_only: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_Only

  val root_dir: Os.Path = Os.home / "devel" / "slang-embedded"

  var experimentalOptions: ISZ[String] = ISZ(ExperimentalOptions.GENERATE_DOT_GRAPHS)

  def genFull(rootProjDirName: String,
              aadlDir: String,
              codegenDir: String,
              _basePackageName: String,
              json: String,
              platforms: ISZ[Cli.HamrPlatform.Type], shouldSimulate: B, timeout: Z): Projecty = {
    val _projDir = root_dir / rootProjDirName
    val _aadlDir = _projDir / aadlDir
    val _rootOutDir = _projDir / codegenDir
    val _json = _aadlDir / ".slang" / json

    assert(_projDir.exists, _projDir)
    assert(_aadlDir.exists, _aadlDir)
    assert(_rootOutDir.exists, _rootOutDir)
    assert(_json.exists, _json)

    return Projecty(
      simpleName = rootProjDirName,
      basePackageName = _basePackageName,
      projectDir = _projDir,
      aadlDir = _aadlDir,
      slangFile = _json,
      rootOutputDir = _rootOutDir,
      platforms = platforms,
      shouldSimulate = shouldSimulate,
      timeout = timeout)
  }

  def gen(rootProjDirName: String, aadlDir: String, codegenDir: String, basePackageName: String, json: String, platforms: ISZ[Cli.HamrPlatform.Type]): Projecty = {
    return genFull(rootProjDirName, aadlDir, codegenDir, basePackageName, json, platforms, T, defTimeout)
  }

  val projects: ISZ[Projecty] = ISZ(
    gen("wms", "aadl", "hamr", "base", "wbs_wms_impl_Instance.json",
      ISZ(jvm, linux, sel4))
  )

  def run(): Unit = {
    val reporter = Reporter.create

    for (project <- projects) {
      var reports: HashSMap[Cli.HamrPlatform.Type, Report] = HashSMap.empty

      for (platform <- project.platforms) {

        println("***************************************")
        println(s"${project.projectDir} -- ${platform})")
        println("***************************************")

        val camkesOutputDir: Os.Path = platform match {
          case Cli.HamrPlatform.SeL4 => project.rootOutputDir / "src" / "c" / "CAmkES_seL4"
          case _ => Os.tempDir()
        }

        val o = Cli.HamrCodeGenOption(
          help = "",
          args = ISZ(project.slangFile.value),
          msgpack = F,
          verbose = T,
          platform = platform,

          packageName = Some(project.basePackageName),
          noEmbedArt = F,
          devicesAsThreads = F,
          excludeComponentImpl = T,

          bitWidth = 32,
          maxStringSize = 256,
          maxArraySize = 1,
          runTranspiler = build,

          slangAuxCodeDirs = ISZ(),
          slangOutputCDir = None(),
          outputDir = Some(project.rootOutputDir.value),

          camkesOutputDir = Some(camkesOutputDir.value),
          camkesAuxCodeDirs = ISZ(),
          aadlRootDir = Some(project.aadlDir.value),

          experimentalOptions = experimentalOptions
        )

        org.sireum.cli.HAMR.codeGen(o)

        if(shouldReport && isSel4(platform)) {
          val gen = ReadmeGenerator(o, reporter)

          if(gen.build()) {
            val expectedOutput: ST =
              if(project.shouldSimulate) gen.simulate(project.timeout)
              else st"NEED TO MANUALLY UPDATE EXPECTED OUTPUT"

            val report = Report(
              readmeDir = project.rootOutputDir,
              options = o,
              runHamrScript = None(),
              timeout = project.timeout,
              runInstructions = gen.genRunInstructions(root_dir, None()),
              expectedOutput = Some(expectedOutput),
              aadlArchDiagram = gen.getAadlArchDiagram(),
              hamrCamkesArchDiagram = gen.getHamrCamkesArchDiagram(graphFormat),
              camkesArchDiagram = gen.getCamkesArchDiagram(graphFormat),
              symbolTable = Some(gen.symbolTable)
            )

            reports = reports + (platform ~> report)
          }
        }
      }

      if(shouldReport) {
        val readme = project.rootOutputDir / "readme_autogen.md"
        val readmest = ReadmeTemplate.generateReport(project.simpleName, reports)
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