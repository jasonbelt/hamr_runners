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

                         title: String,
                         description: Option[ST],

                         slangFile: Os.Path,
                         platforms: ISZ[Cli.HamrPlatform.Type],
                         shouldSimulate: B,
                         excludeComponentImplementation: B,
                         devicesAsThreads: B,
                         timeout: Z,

                        readmeName: Option[String])

object iccps20 extends App {

  val shouldReport: B = T
  val shouldSimulate: B = F
  val shouldRebuild: B = F
  val shouldRunTranspiler: B = T

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
              projDir: Option[Os.Path],
              title: String,
              description: Option[ST],
              rootAadlDir: String, outputDir: String, json: String,
              platforms: ISZ[Cli.HamrPlatform.Type],
              excludeComponentImplementation: B,
              shouldSimulate: B,
              devicesAsThreads: B,
              timeout: Z,
              readmeName: Option[String]): Project = {
    val projectDir: Os.Path = if(projDir.nonEmpty) projDir.get / name else case_tool_evaluation_dir / name
    val aadlDir = projectDir / rootAadlDir

    val hamrDir: Os.Path = projectDir / outputDir

    val simpleName = Os.path(name).name // get last dir name

    return Project(
      simpleName = simpleName,
      basePackageName = basePackageName,
      title = title,
      description = description,
      projectDir = projectDir,
      aadlDir = aadlDir,
      hamrDir = hamrDir,
      slangFile = aadlDir / ".slang" / json,
      platforms = platforms,
      shouldSimulate = shouldSimulate,
      excludeComponentImplementation = excludeComponentImplementation,
      devicesAsThreads = devicesAsThreads,
      timeout = timeout,
      readmeName = readmeName
    )
  }

  val tempControl_Periodic_Excludes: Project = {
  // Periodic Dispatcher Excludes
  // Purpose: Periodic Dispatching
    genFull(
      name = "temperature-control",
      projDir = None(),
      basePackageName = "b",

      title = "Temperature Control with seL4 Periodic Dispatcher",
      description = Some(st"""- C-based behavior code
                             |- seL4 periodic dispatching performed by periodic dispatching component"""),

      rootAadlDir = "aadl",
      outputDir = "hamr",
      json = "TemperatureControl_TempControlSystem_i_Instance.json",
      platforms = ISZ(jvm, linux, sel4),
      excludeComponentImplementation = T,
      devicesAsThreads = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = None())
  }

  val tempControl_Periodic_Just_Camkes: Project = {
    // Periodic Dispatcher - Sel4 Only, Sel4 TB
    // Purpose: TB vs SB camkes connectors (i.e. monitors)
    genFull(
      name = "temperature-control",
      projDir = None(),
      basePackageName = "b",

      title = "Temperature Control with Trusted Build Style Monitors",
      description = Some(st"""Temperature Control system comparing the seL4 monitor based communication used in Trusted Build"""),

      rootAadlDir = "aadl",
      outputDir = "camkes",
      json = "TemperatureControl_TempControlSystem_i_Instance.json",
      platforms = ISZ(sel4_tb, sel4_only),
      excludeComponentImplementation = F,
      devicesAsThreads = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = Some("readme_camkes.md"))
  }

  val tempControl_DS_Excludes: Project = {
    // Domain scheduling - Excludes
    // Purpose: Illustrates C level behavior code
    genFull(
      name = "temperature-control-ds",
      projDir = None(),

      title = "Temperature Control with seL4 Domain Scheduling",
      description = Some(st"""- C-based behavior code
                             |- seL4 domain scheduling"""),

      basePackageName = "b",
      rootAadlDir = "aadl",
      outputDir = "hamr",
      json = "TemperatureControl_TempControlSystem_i_Instance.json",
      platforms = ISZ(//jvm,
         linux,
        //sel4
      ),
      excludeComponentImplementation = T,
      devicesAsThreads = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = None())
  }

  val tempControl_Video: Project = {
    // Domain scheduling - Excludes
    // Purpose: Illustrates C level behavior code
    val dir = Os.path("/home/vagrant/temp/temperature-control")
    val cdir = dir / "hamr" / "src" / "c"
    if(cdir.exists) {
      cdir.removeAll()
    }

    genFull(
      name = "temperature-control",
      projDir = Some(Os.path("/home/vagrant/temp/")),

      title = "Temperature Control with seL4 Domain Scheduling",
      description = Some(st"""- C-based behavior code
                             |- seL4 domain scheduling"""),

      basePackageName = "b",
      rootAadlDir = "aadl",
      outputDir = "hamr",
      json = "TemperatureControl_TempControlSystem_i_Instance.json",
      platforms = ISZ(//jvm,
        linux,
        sel4
      ),
      excludeComponentImplementation = T,
      devicesAsThreads = F,
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
      projDir = None(),

      title = "Temperature Control with seL4 Domain Scheduling",
      description = Some(st"""- Slang-based behavior code
                             |- seL4 domain scheduling"""),

      rootAadlDir = "aadl",
      outputDir = "hamr-nonExcludes",
      json = "TemperatureControl_TempControlSystem_i_Instance.json",
      platforms = ISZ(jvm, linux, sel4),
      excludeComponentImplementation = F,
      devicesAsThreads = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = Some("readme_nonExcludes.md"))
  }

  val isolette: Project = {

    genFull(
      name = "isolette",
      basePackageName = "isolette",
      projDir = None(),

      title = "Isolette",
      description = None(),

      rootAadlDir = "aadl",
      outputDir = "hamr",
      json = "Isolette_isolette_single_sensor_Instance.json",
      platforms = ISZ(jvm, linux, sel4),
      excludeComponentImplementation = F,
      devicesAsThreads = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName= None()
    )
  }

  val pcaPump: Project = {

    genFull(
      name = "pca-pump",
      basePackageName= "pca_pump",
      projDir = None(),


      title = "PCA Pump",
      description = None(),

      rootAadlDir = "aadl/pca",
      outputDir = "hamr",
      json = "PCA_System_wrap_pca_imp_Instance.json",
      platforms = ISZ(jvm),
      excludeComponentImplementation = F,
      devicesAsThreads = T,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = None()
    )
  }

  val tests: ISZ[Project] = ISZ(

    tempControl_Video,

    //tempControl_DS_Excludes,
    /*
    tempControl_DS_NonExcludes,

    tempControl_Periodic_Excludes,

    tempControl_Periodic_Just_Camkes,

    isolette,

    pcaPump

     */
  )

  def removeResources(value: Os.Path): Unit = {
    val buildsbt = value / "build.sbt"
    val buildsc = value / "build.sc"
    //assert(buildsbt.exists, buildsbt)
    //assert(buildsc.exists, buildsc)

    if(buildsbt.exists) {
      buildsbt.remove()
    }
    if(buildsc.exists) {
      buildsc.remove()
    }
  }

  def run(): Unit = {
    val reporter = Reporter.create
    var projReadmes: ISZ[(Os.Path, Project)] = ISZ()

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
          devicesAsThreads = project.devicesAsThreads,
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

        removeResources(outputDir)

        org.sireum.cli.HAMR.codeGen(o)

        if(shouldReport) {
          val gen = IccpsReadmeGenerator(o, project, reporter)

          if(gen.build(shouldRebuild)) {
            /*
            val expectedOutput: Option[ST] =
              if(project.shouldSimulate && (isJVM(o.platform) || isSel4(o.platform))) Some(gen.simulate(project.timeout))
              else None()
            */
            val expectedOutput: Option[ST] = None()

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
        val readmeFilename = project.projectDir / fname
        val readmest = IccpsReadmeTemplate.generateReport(project, reports)
        readmeFilename.writeOver(readmest.render)

        val entry = (readmeFilename, project)
        projReadmes = projReadmes :+ entry
      }
    }

    val masterReadme =  case_tool_evaluation_dir / "readme.md"
    var entries: ISZ[ST] = ISZ()
    for(e <- projReadmes) {
      entries = entries :+
        st"""## [${e._2.title}](${e._1})
            |  ${e._2.description}
          """
    }

    val r =
      st"""# ICCPS 2020 Case Studies
          |
          |${(entries, "\n\n")}
          |"""

    masterReadme.writeOver(r.render)

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