// #Sireum

package org.sireum.cli.hamr_runners.iccps20

import org.sireum._
import org.sireum.Cli.HamrPlatform
import org.sireum.cli.hamr_runners.{DotFormat, ReadmeGenerator, ReadmeTemplate, Report}
import org.sireum.hamr.codegen.common.util.ExperimentalOptions
import org.sireum.message.Reporter

@datatype class Project (simpleName: String,
                         projectDir: Os.Path,
                         aadlDir: Os.Path,
                         hamrDir: Os.Path,

                         slangFile: Os.Path,
                         platforms: ISZ[Cli.HamrPlatform.Type],
                         shouldSimulate: B,
                         timeout: Z)

object iccps20 extends App {

  val shouldReport: B = T
  val graphFormat: DotFormat.Type = DotFormat.svg
  val build: B = F
  val defTimeout: Z = 15000
  val vmTimeout: Z = 90000

  val jvm: Cli.HamrPlatform.Type = Cli.HamrPlatform.JVM
  val linux: Cli.HamrPlatform.Type = Cli.HamrPlatform.Linux
  val sel4: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4
  val sel4_tb: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_TB
  val sel4_only: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_Only

  val case_tool_evaluation_dir: Os.Path = Os.home / "devel/slang-embedded/paper-examples"

  var experimentalOptions: ISZ[String] = ISZ(ExperimentalOptions.GENERATE_DOT_GRAPHS)

  def genFull(name: String, rootAadlDir: String, json: String, platforms: ISZ[Cli.HamrPlatform.Type], shouldSimulate: B, timeout: Z): Project = {
    val projectDir = case_tool_evaluation_dir / name
    val aadlDir = projectDir / rootAadlDir
    val hamrDir = projectDir / "hamr"
    val simpleName = Os.path(name).name // get last dir name

    return Project(
      simpleName = simpleName,
      projectDir = projectDir,
      aadlDir = aadlDir,
      hamrDir = hamrDir,
      slangFile = aadlDir / ".slang" / json,
      platforms = platforms,
      shouldSimulate = shouldSimulate,
      timeout = timeout)
  }

  def gen(name: String, rootAadlDir: String, json: String, platforms: ISZ[Cli.HamrPlatform.Type]): Project = {
    return genFull(name, rootAadlDir, json, platforms, T, defTimeout)
  }

  val nonVmProjects: ISZ[Project] = ISZ(

    gen("pca-pump", "aadl/pca", "PCA_System_wrap_pca_imp_Instance.json", ISZ(jvm)),

  )

  val tests: ISZ[Project] = nonVmProjects

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

        /*
        val outputDir: Os.Path = platform match {
          case Cli.HamrPlatform.Linux => project.hamrDir / "Linux"
          case Cli.HamrPlatform.SeL4_TB => project.hamrDir / "CAmkES_seL4_TB"
          case Cli.HamrPlatform.SeL4_Only => project.hamrDir / "CAmkES_seL4_Only"
          case Cli.HamrPlatform.SeL4 => project.hamrDir / "CAmkES_seL4"
          case _ => halt("??")
        }
        */

        val outputDir: Os.Path = project.hamrDir

        val camkesOutputDir: Option[Os.Path] = platform match {
          case Cli.HamrPlatform.SeL4_TB => Some(outputDir)
          case Cli.HamrPlatform.SeL4_Only => Some(outputDir)
          case Cli.HamrPlatform.SeL4 => Some(outputDir / "src/c/CAmkES_seL4")
          case _ => None()
        }

        //outputDir.removeAll()
        val dirs = ISZ("architecture", "art", "bridge").map(m => project.hamrDir / "src" / "main" / m)
        for(d <- dirs) {
          assert(d.exists, d)
          d.removeAll()
        }
        (project.hamrDir / "src" / "test").removeAll()

        val o = Cli.HamrCodeGenOption(
          help = "",
          args = ISZ(project.slangFile.value),
          json = T,
          verbose = T,
          platform = platform,

          packageName = Some(project.simpleName),
          embedArt = T,
          devicesAsThreads = T,
          excludeComponentImpl = F,

          bitWidth = 32,
          maxStringSize = 256,
          maxArraySize = 1,
          runTranspiler = build,

          slangAuxCodeDirs = ISZ(),
          slangOutputCDir = None(),
          outputDir = Some(outputDir.value),

          camkesOutputDir = camkesOutputDir.map(s => s.value),
          camkesAuxCodeDirs = ISZ(),
          aadlRootDir = Some(project.aadlDir.value),

          experimentalOptions = experimentalOptions
        )

        org.sireum.cli.HAMR.codeGen(o)

        if(ops.StringOps(outputDir.value).contains("_VM")) {
          replaceVM()
        }

        if(shouldReport && isSel4(platform)) {
          val gen = ReadmeGenerator(o, reporter)

          if(gen.build()) {
            val timeout: Z = if(platform == Cli.HamrPlatform.SeL4) defTimeout else project.timeout

            val expectedOutput: ST =
              if(project.shouldSimulate) gen.simulate(timeout)
              else st"NEED TO MANUALLY UPDATE EXPECTED OUTPUT"

            val report = Report(
              options = o,
              timeout = project.timeout,
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
        val readme = project.projectDir / "readme_autogen.md"
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

  def replaceVM(): Unit = {

    val dirs: ISZ[String] = ISZ(
      "test_data_port_periodic_domains_VM/both_vm",
      "test_data_port_periodic_domains_VM/receiver_vm",
      "test_data_port_periodic_domains_VM/sender_vm",

      "test_event_data_port_periodic_domains_VM/both_vm",
      "test_event_data_port_periodic_domains_VM/receiver_vm",
      "test_event_data_port_periodic_domains_VM/sender_vm",

      "test_event_data_port_periodic_domains_VMx/receiver_vm",
      "test_event_data_port_periodic_domains_VMx/sender_vm"

    ).flatMap(m => ISZ(
      s"${m}/CAmkES_seL4_Only/components/VM/apps",
      s"${m}/CAmkES_seL4_Only/components/VM/overlay_files"))

    for(d <- dirs) {
      val path = case_tool_evaluation_dir / d
      assert(path.exists, s"$path does not exist")

      val comm: ISZ[String] = ISZ("git", "checkout", d)
      Os.proc(comm).at(case_tool_evaluation_dir).console.runCheck()
    }
  }
}