// #Sireum

package org.sireum.cli.hamr_runners.iccps20

import org.sireum._
import org.sireum.Cli.HamrPlatform
import org.sireum.hamr.codegen.common.util.ExperimentalOptions
import org.sireum.message.Reporter

@datatype class Project (simpleName: String,
                         basePackageName: String,
                         projectDir: Os.Path,
                         aadlDir: Os.Path,
                         hamrDir: Os.Path,

                         slangFile: Os.Path,
                         platforms: ISZ[Cli.HamrPlatform.Type],
                         shouldSimulate: B,
                         excludeComponentImplementation: B,
                         timeout: Z,

                        readmeName: Option[String])

object iccps20 extends App {

  val shouldReport: B = T
  val shouldRebuild: B = F
  val shouldRunTranspiler: B = F

  val graphFormat: DotFormat.Type = DotFormat.svg
  val defTimeout: Z = 1
  val vmTimeout: Z = 90000

  val jvm: Cli.HamrPlatform.Type = Cli.HamrPlatform.JVM
  val linux: Cli.HamrPlatform.Type = Cli.HamrPlatform.Linux
  val sel4: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4
  val sel4_tb: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_TB
  val sel4_only: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_Only

  val case_tool_evaluation_dir: Os.Path = Os.home / "devel/slang-embedded/iccps20-case-studies"

  var experimentalOptions: ISZ[String] = ISZ(ExperimentalOptions.GENERATE_DOT_GRAPHS)

  def genFull(name: String, basePackageName: String,
              rootAadlDir: String, outputDir: String, json: String,
              platforms: ISZ[Cli.HamrPlatform.Type],
              excludeComponentImplementation: B, shouldSimulate: B, timeout: Z,
              readmeName: Option[String]): Project = {
    val projectDir = case_tool_evaluation_dir / name
    val aadlDir = projectDir / rootAadlDir

    val hamrDir: Os.Path = projectDir / outputDir

    val simpleName = Os.path(name).name // get last dir name

    return Project(
      simpleName = simpleName,
      basePackageName = basePackageName,
      projectDir = projectDir,
      aadlDir = aadlDir,
      hamrDir = hamrDir,
      slangFile = aadlDir / ".slang" / json,
      platforms = platforms,
      shouldSimulate = shouldSimulate,
      excludeComponentImplementation = excludeComponentImplementation,
      timeout = timeout,
      readmeName = readmeName
    )
  }

  val tempControl_Periodic_Excludes: Project = {
  // Periodic Dispatcher Excludes
  // Purpose: Periodic Dispatching
    genFull(
      name = "temperature-control",
      basePackageName = "b",
      rootAadlDir = "aadl",
      outputDir = "hamr",
      json = "TemperatureControl_TempControlSystem_i_Instance.json",
      platforms = ISZ(jvm, linux, sel4),
      excludeComponentImplementation = T,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = None())
  }

  val tempControl_Periodic_Just_Camkes: Project = {
    // Periodic Dispatcher - Sel4 Only, Sel4 TB
    // Purpose: TB vs SB camkes connectors (i.e. monitors)
    genFull(
      name = "temperature-control",
      basePackageName = "b",
      rootAadlDir = "aadl",
      outputDir = "camkes",
      json = "TemperatureControl_TempControlSystem_i_Instance.json",
      platforms = ISZ(sel4_tb, sel4_only),
      excludeComponentImplementation = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = Some("readme_camkes.md"))
  }

  val tempControl_DS_Excludes: Project = {
    // Domain scheduling - Excludes
    // Purpose: Illustrates C level behavior code
    genFull(
      name = "temperature-control-ds",
      basePackageName = "b",
      rootAadlDir = "aadl",
      outputDir = "hamr",
      json = "TemperatureControl_TempControlSystem_i_Instance.json",
      platforms = ISZ(jvm, linux, sel4),
      excludeComponentImplementation = T,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = None())
  }

  val tempControl_DS_NonExcludes: Project = {
    // Domain scheduling - Non-Excludes
    // Purpose: Illustrates use of Slang extension mechanisms
    genFull(
      name = "temperature-control-ds",
      basePackageName = "b",
      rootAadlDir = "aadl",
      outputDir = "hamr-nonExcludes",
      json = "TemperatureControl_TempControlSystem_i_Instance.json",
      platforms = ISZ(jvm, linux, sel4),
      excludeComponentImplementation = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = Some("readme_nonExcludes.md"))
  }

  val isolette: Project = {

    genFull(
      name = "isolette",
      basePackageName = "isolette",
      rootAadlDir = "aadl",
      outputDir = "hamr",
      json = "Isolette_isolette_single_sensor_Instance.json",
      platforms = ISZ(jvm, linux, sel4),
      excludeComponentImplementation = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName= None()
    )
  }

  val pcaPump: Project = {

    genFull(
      name = "pca-pump",
      basePackageName= "pca_pump",
      rootAadlDir = "aadl/pca",
      outputDir = "hamr",
      json = "PCA_System_wrap_pca_imp_Instance.json",
      platforms = ISZ(jvm),
      excludeComponentImplementation = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = None()
    )
  }

  ///home/vagrant/devel/slang-embedded/iccps20-case-studies/Phase-2-UAV-Experimental-Platform-Transformed/.slang/UAV_UAV_Impl_Instance.json
  val uav: Project = {
    genFull(
      name = "Phase-2-UAV-Experimental-Platform-Transformed",
      basePackageName = "uav",
      rootAadlDir = "aadl",
      outputDir = "hamr",
      json = "UAV_UAV_Impl_Instance.json",
      platforms = ISZ(sel4),
      excludeComponentImplementation = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = None()
    )
  }
  val tests: ISZ[Project] = ISZ(
    /*
    tempControl_Periodic_Excludes,
    tempControl_Periodic_Just_Camkes,
    tempControl_DS_Excludes,
    tempControl_DS_NonExcludes,
    isolette,
    pcaPump
    */
    uav
  )

  def run(): Unit = {
    val reporter = Reporter.create

    for (project <- tests) {
      var reports: HashSMap[Cli.HamrPlatform.Type, Report] = HashSMap.empty

      assert(project.projectDir.exists, project.projectDir)
      assert(project.aadlDir.exists, project.aadlDir)
      assert(project.slangFile.exists, project.slangFile)

      if(!project.projectDir.exists) {
        halt(s"${project.projectDir} does not exist");
      }

      for (platform <- project.platforms) {

        println("***************************************")
        println(s"${project.projectDir} -- ${platform})")
        println("***************************************")

        val outputDir: Os.Path = project.hamrDir

        val camkesOutputDir: Option[Os.Path] = platform match {
          case Cli.HamrPlatform.SeL4_TB => Some(outputDir / "CAkES_seL4_tb")
          case Cli.HamrPlatform.SeL4_Only => Some(outputDir / "CAmkES_seL4_only")
          case Cli.HamrPlatform.SeL4 => Some(outputDir / "src/c/CAmkES_seL4")
          case _ => None()
        }

        val o = Cli.HamrCodeGenOption(
          help = "",
          args = ISZ(project.slangFile.value),
          json = T,
          verbose = T,
          platform = platform,

          packageName = Some(project.basePackageName),
          embedArt = T,
          devicesAsThreads =F ,
          excludeComponentImpl = project.excludeComponentImplementation,

          bitWidth = 32,
          maxStringSize = 256,
          maxArraySize = 1,
          runTranspiler = shouldRunTranspiler,

          slangAuxCodeDirs = ISZ(),
          slangOutputCDir = None(),
          outputDir = Some(outputDir.value),

          camkesOutputDir = camkesOutputDir.map(s => s.value),
          camkesAuxCodeDirs = ISZ(),
          aadlRootDir = Some(project.aadlDir.value),

          experimentalOptions = experimentalOptions
        )

        org.sireum.cli.HAMR.codeGen(o)

        if(shouldReport) {
          val gen = IccpsReadmeGenerator(o, project, reporter)

          if(gen.build(shouldRebuild)) {
            val expectedOutput: Option[ST] =
              if(project.shouldSimulate && (isJVM(o.platform) || isSel4(o.platform))) Some(gen.simulate(project.timeout))
              else None()

            val aadlMetrics: ST = gen.getAadlMetrics()

            val report: Report =
              if(isJVM(o.platform)) {
                JvmReport(
                  options = o,
                  timeout = project.timeout,
                  runInstructions = gen.genRunInstructions(case_tool_evaluation_dir),
                  expectedOutput = expectedOutput,
                  aadlArchDiagram = gen.getAadlArchDiagram(),
                  aadlMetrics = aadlMetrics,
                  codeMetrics = gen.getCodeMetrics()
                )
              } else if(isSel4(o.platform)) {
                CamkesReport(
                  options = o,
                  timeout = project.timeout,
                  runInstructions = gen.genRunInstructions(case_tool_evaluation_dir),
                  expectedOutput = expectedOutput,
                  aadlArchDiagram = gen.getAadlArchDiagram(),
                  hamrCamkesArchDiagram = gen.getHamrCamkesArchDiagram(graphFormat),
                  camkesArchDiagram = gen.getCamkesArchDiagram(graphFormat),
                  aadlMetrics = aadlMetrics,
                  codeMetrics = gen.getCodeMetrics()
                )
              } else {
                NixReport(
                  options = o,
                  timeout = project.timeout,
                  runInstructions = gen.genRunInstructions(case_tool_evaluation_dir),
                  expectedOutput = expectedOutput,
                  aadlArchDiagram = gen.getAadlArchDiagram(),
                  aadlMetrics = aadlMetrics,
                  codeMetrics = gen.getCodeMetrics()
                )
              }

            reports = reports + (platform ~> report)
          }
        }
      }

      if(shouldReport) {
        val fname: String = if(project.readmeName.isEmpty) "readme.md" else project.readmeName.get
        val readme = project.projectDir / fname
        val readmest = IccpsReadmeTemplate.generateReport(project.simpleName, project.projectDir, reports)
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

  def isJVM(platform: HamrPlatform.Type): B = {return platform == HamrPlatform.JVM}

  def isSel4(platform: HamrPlatform.Type): B = {
    val ret: B = platform match {
      case HamrPlatform.SeL4 => T
      case HamrPlatform.SeL4_TB => T
      case HamrPlatform.SeL4_Only => T
      case _ => F
    }
    return ret
  }
}