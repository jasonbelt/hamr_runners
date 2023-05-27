// #Sireum

package org.sireum.cli.hamr_runners.fmics23

import org.sireum._
import org.sireum.Cli.SireumHamrCodegenHamrPlatform
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
                         platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type],
                         shouldSimulate: B,
                         excludeComponentImplementation: B,
                         devicesAsThreads: B,
                         timeout: Z,

                         readmeName: Option[String],
                         dscCoverageLink: String
                        )

object Tccoe22 extends App {

  val shouldReport: B = T
  val shouldSimulate: B = T
  val shouldRebuild: B = T

  val graphFormat: DotFormat.Type = DotFormat.svg
  val defTimeout: Z = 1
  val vmTimeout: Z = 90000

  val jvm: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.JVM
  val linux: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.Linux

  val gumboxCSRoot: Os.Path = Os.home / "devel" / "gumbox-case-studies"

  def genFull(name: String, basePackageName: String,
              projectDir: Os.Path,
              title: String,
              description: Option[ST],
              rootAadlDir: Os.Path,
              slangOutputDir: Os.Path,
              json: String,
              platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type],
              excludeComponentImplementation: B,
              shouldSimulate: B,
              devicesAsThreads: B,
              timeout: Z,
              readmeName: Option[String],
              dscCoverageLink: String): Project = {

    val simpleName = Os.path(name).name // get last dir name

    return Project(
      simpleName = simpleName,
      basePackageName = basePackageName,
      title = title,
      description = description,
      projectDir = projectDir,
      aadlDir = rootAadlDir,
      hamrDir = slangOutputDir,
      slangFile = rootAadlDir / ".slang" / json,
      platforms = platforms,
      shouldSimulate = shouldSimulate,
      excludeComponentImplementation = excludeComponentImplementation,
      devicesAsThreads = devicesAsThreads,
      timeout = timeout,
      readmeName = readmeName,
      dscCoverageLink = dscCoverageLink
    )
  }

  val tempControlPeriodicRoot: Os.Path = gumboxCSRoot / "temp_control" / "periodic"
  val tempControlPeriodic: Project = {
    genFull(
      name = "temperature-control-periodic",
      projectDir = tempControlPeriodicRoot,
      basePackageName = "tc",

      title = "Periodic Temperature Control",
      description = None(),

      rootAadlDir = tempControlPeriodicRoot / "aadl",
      slangOutputDir = tempControlPeriodicRoot / "hamr",
      json = "TempControlSoftwareSystem_TempControlSoftwareSystem_p_Instance.json",
      platforms = ISZ(jvm),
      excludeComponentImplementation = F,
      devicesAsThreads = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = None(),
      dscCoverageLink = "https://people.cs.ksu.edu/~santos_jenkins/dsc_results/tc/tc.html")
  }

  val isoletteRoot: Os.Path = gumboxCSRoot / "isolette"
  val isolette: Project = {

    genFull(
      name = "isolette",
      basePackageName = "isolette",
      projectDir = isoletteRoot,

      title = "Isolette",
      description = None(),

      rootAadlDir = isoletteRoot / "aadl",
      slangOutputDir = isoletteRoot / "hamr",
      json = "Isolette_isolette_single_sensor_Instance.json",
      platforms = ISZ(jvm),
      excludeComponentImplementation = F,
      devicesAsThreads = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = None(),
      dscCoverageLink = "https://people.cs.ksu.edu/~santos_jenkins/dsc_results/isolette/isolette.html"
    )
  }

  val rtsRoot: Os.Path = gumboxCSRoot / "rts"
  val rts: Project = {

    genFull(
      name = "rst",
      basePackageName = "RTS",
      projectDir = rtsRoot,

      title = "RTS",
      description = None(),

      rootAadlDir = rtsRoot / "aadl",
      slangOutputDir = rtsRoot / "hamr",
      json = "RTS_RTS_i_Instance.json",
      platforms = ISZ(jvm),
      excludeComponentImplementation = F,
      devicesAsThreads = F,
      shouldSimulate = T,
      timeout = defTimeout,
      readmeName = None(),
      dscCoverageLink = "https://people.cs.ksu.edu/~santos_jenkins/dsc_results/rts/rts.html"
    )
  }


  val tests: ISZ[Project] = ISZ(
    tempControlPeriodic,
    isolette,
    rts
  )

  def run(): Unit = {
    val reporter = Reporter.create
    var projReadmes: ISZ[(Os.Path, Project)] = ISZ()

    for (project <- tests) {
      var reports: HashSMap[Cli.SireumHamrCodegenHamrPlatform.Type, Report] = HashSMap.empty

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

        //val outputDir: Os.Path = project.hamrDir

        val slangDir = project.hamrDir / "slang"
        val cDir = project.hamrDir / "c"


        val o = Cli.SireumHamrCodegenOption(
          help = "",
          args = ISZ(project.slangFile.value),
          msgpack = F,
          verbose = T,
          platform = platform,

          packageName = Some(project.basePackageName),
          noProyekIve = F,
          noEmbedArt = F,
          devicesAsThreads = project.devicesAsThreads,
          excludeComponentImpl = project.excludeComponentImplementation,
          genSbtMill = F,

          bitWidth = 32,
          maxStringSize = 256,
          maxArraySize = 1,
          runTranspiler = F,

          slangAuxCodeDirs = ISZ(),
          slangOutputCDir = Some(cDir.value),
          outputDir = Some(slangDir.value),

          camkesOutputDir = None(),
          camkesAuxCodeDirs = ISZ(),
          aadlRootDir = Some(project.aadlDir.value),

          experimentalOptions = ISZ()
        )

        org.sireum.cli.HAMR.codeGen(o, reporter)

        if(shouldReport) {
          val gen = TccoeReadmeGenerator(o, project, reporter)

          if(gen.build(shouldRebuild)) {
            /*
            val expectedOutput: Option[ST] =
              if(project.shouldSimulate && (isJVM(o.platform) || isSel4(o.platform))) Some(gen.simulate(project.timeout))
              else None()
            */
            val expectedOutput: Option[ST] = None()

            val aadlMetrics: ST = gen.getAadlMetrics()

            val report: Report =
                JvmReport(
                  options = o,
                  timeout = project.timeout,
                  runInstructions = gen.genRunInstructions(gumboxCSRoot),
                  expectedOutput = expectedOutput,
                  aadlArchDiagram = gen.getAadlArchDiagram(),
                  aadlMetrics = aadlMetrics,
                  codeMetrics = gen.getCodeMetrics()
                )

            reports = reports + (platform ~> report)
          }
        }
      }

      if(shouldReport) {
        val fname: String = if(project.readmeName.isEmpty) "readme.md" else project.readmeName.get
        val readmeFilename = project.projectDir / fname
        val readmest = IccpsReadmeTemplate.generateReport(project, reports)
        readmeFilename.writeOver(readmest.render)

        println(s"Wrote: ${readmeFilename}")

        val entry = (readmeFilename, project)
        projReadmes = projReadmes :+ entry
      }
    }

    val masterReadme =  gumboxCSRoot / "readme.md"
    var entries: ISZ[ST] = ISZ()
    for(e <- projReadmes) {
      entries = entries :+
        st"""## [${e._2.title}](${e._1})
            |  ${e._2.description}
          """
    }

    val r =
      st"""# GUMBOX 2023 Case Studies
          |
          |${(entries, "\n\n")}
          |"""

    masterReadme.writeOver(r.render)

    println(s"Wrote: ${masterReadme}")

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

  def isJVM(platform: Cli.SireumHamrCodegenHamrPlatform.Type): B = {return platform == Cli.SireumHamrCodegenHamrPlatform.JVM}

  def isSel4(platform: Cli.SireumHamrCodegenHamrPlatform.Type): B = {
    val ret: B = platform match {
      case Cli.SireumHamrCodegenHamrPlatform.SeL4 => T
      case Cli.SireumHamrCodegenHamrPlatform.SeL4_TB => T
      case Cli.SireumHamrCodegenHamrPlatform.SeL4_Only => T
      case _ => F
    }
    return ret
  }
}