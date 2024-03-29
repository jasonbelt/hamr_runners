// #Sireum

package org.sireum.cli.hamr_runners.casex

import org.sireum._
import org.sireum.cli.hamr_runners.{DotFormat, ReadmeGenerator, ReadmeTemplate, Report}
import org.sireum.hamr.codegen.common.util.ExperimentalOptions
import org.sireum.message.Reporter

object CaseToolEval4_vm extends App {

  @datatype class Project (simpleName: String,
                           modelDir: Os.Path,
                           slangFile: Os.Path,
                           platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type],
                           shouldSimulate: B,
                           timeout: Z)
  val USE_OSIREUM: B = F

  val shouldReport: B = T
  val graphFormat: DotFormat.Type = DotFormat.svg
  val runTranspiler: B = T
  val defTimeout: Z = 18000
  val vmTimeout: Z = 90000

  val linux: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.Linux
  val sel4: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4
  val sel4_tb: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4_TB
  val sel4_only: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4_Only

  val case_tool_evaluation_dir: Os.Path = Os.home / "devel/case/case-loonwerks/TA5/tool-evaluation-4/HAMR/examples"

  var experimentalOptions: ISZ[String] = ISZ(ExperimentalOptions.GENERATE_DOT_GRAPHS)

  def genFull(name: String, json: String, platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type], shouldSimulate: B, timeout: Z): Project = {
    val modelDir = case_tool_evaluation_dir / name
    val simpleName = Os.path(name).name // get last dir name
    return Project(simpleName, modelDir, modelDir / ".slang" / json, platforms, shouldSimulate, timeout)
  }

  def gen(name: String, json: String, platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type]): Project = {
    return genFull(name, json, platforms, T, defTimeout)
  }

  val nonVmProjects: ISZ[Project] = ISZ(

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
  )

  val vmProjects: ISZ[Project] = ISZ(
    // VMs
    //genFull("test_data_port_periodic_domains_VM/receiver_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), T, vmTimeout),
    //genFull("test_data_port_periodic_domains_VM/sender_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), T, vmTimeout),

    //genFull("test_event_data_port_periodic_domains_VM/receiver_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), T, vmTimeout),
    //genFull("test_event_data_port_periodic_domains_VM/sender_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), T, vmTimeout),

    // VMs with Kent's connector
    //genFull("test_event_data_port_periodic_domains_VMx/receiver_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only), T, vmTimeout),


    // VMs which can't use init trick
    genFull("test_data_port_periodic_domains_VM/both_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), T, vmTimeout),
    //TODO//genFull("test_event_data_port_periodic_domains_VM/both_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), T, vmTimeout),

    // VMs which can't use init trick -- using Kent's connector
    //TODO//genFull("test_event_data_port_periodic_domains_VMx/sender_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only), T, vmTimeout)
  )

  //val tests: ISZ[Project] = nonVmProjects
  //val tests: ISZ[Project] = nonVmProjects ++ vmProjects
  val tests: ISZ[Project] = vmProjects

  def generateRunScript(o: Cli.SireumHamrCodegenOption): Os.Path = {
    o.aadlRootDir match {
      case Some(d) =>
        val aadlDir = Os.path(d)
        val oDir = Os.path(o.outputDir.get)
        val camkesDir = Os.path(o.camkesOutputDir.get)

        val project: Os.Path = if((aadlDir / ".system").exists) aadlDir / ".system" else aadlDir / ".project"
        if(!project.exists) {
          halt(s"${project} doesn't exist")
        }

        val rOutputDir = aadlDir.relativize(oDir).value
        val rCamkesOutputDir = aadlDir.relativize(camkesDir).value

        val bs: String = "\\"

        val sel4Options: Option[ST] =
          if(o.platform == Cli.SireumHamrCodegenHamrPlatform.SeL4) {
            Some(st"""--exclude-component-impl ${bs}
                     |--bit-width ${o.bitWidth} ${bs}
                     |--max-string-size ${o.maxStringSize} ${bs}
                     |--max-array-size ${o.maxArraySize} ${bs}
                     |--run-transpiler ${bs}""")
          }
          else { None() }

        val eOptions: Option[ST] = if(o.experimentalOptions.nonEmpty)
           Some(st"""--experimental-options \"${(o.experimentalOptions, ";")}\" ${bs}""")
        else None()

        val rProject = aadlDir.relativize(project)

        val platform: String = o.platform match {
          case Cli.SireumHamrCodegenHamrPlatform.JVM => "JVM"
          case Cli.SireumHamrCodegenHamrPlatform.Linux => "Linux"
          case Cli.SireumHamrCodegenHamrPlatform.Cygwin => "Cygwin"
          case Cli.SireumHamrCodegenHamrPlatform.MacOS => "MacOs"
          case Cli.SireumHamrCodegenHamrPlatform.SeL4 => "seL4"
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_Only => "seL4_Only"
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_TB => "seL4_TB"
        }

        val s =
          st"""#!/bin/bash -ei
              |
              |SCRIPT_DIR=$$( cd "$$( dirname "$$0" )" &> /dev/null && pwd )
              |AADL_DIR=$$SCRIPT_DIR/..
              |
              |OSIREUM=osireum
              |if [ -f "$$1" ]; then
              |  OSIREUM="$$1 -nosplash -console -consoleLog -data @user.home/.sireum -application org.sireum.aadl.osate.cli"
              |elif command -v $$OSIREUM &> /dev/null ; then
              |  OSIREUM=$$OSIREUM
              |elif [ -n "$${OSIREUM_EXE}" ] && [ -f "$${OSIREUM_EXE}" ]; then
              |  OSIREUM="$$OSIREUM_EXE -nosplash -console -consoleLog -data @user.home/.sireum -application org.sireum.aadl.osate.cli"
              |else
              |  echo "osireum not found.  Run '$$SIREUM_HOME/bin/sireum hamr phantom -h' for instructions on"
              |  echo "how to install the Sireum plugins into OSATE/FMIDE.  Then do one of the following:"
              |  echo "  - Pass in the location of the OSATE/FMIDE executable that contains the Sireum plugins"
              |  echo "  - Add the 'osireum' alias to your .bashrc file"
              |  echo "  - Set the environment variable OSIREUM_EXE to point to the location of"
              |  echo "      the OSATE/FMIDE executable that contains the Sireum plugins"
              |  exit
              |fi
              |
              |eval "$$OSIREUM hamr codegen ${bs}
              |  --verbose ${bs}
              |  --platform $platform ${bs}
              |  --output-dir $$AADL_DIR/${rOutputDir.value} ${bs}
              |  --package-name ${o.packageName.get} ${bs}
              |  ${sel4Options}
              |  --camkes-output-dir $$AADL_DIR/${rCamkesOutputDir.value} ${bs}
              |  --aadl-root-dir $$AADL_DIR ${bs}
              |  $eOptions
              |  $$AADL_DIR/${rProject.value}"
              |"""

        val script = aadlDir / "bin" / s"run-hamr-${o.platform.toString}.sh"
        script.writeOver(s.render)
        script.chmod("700")
        println(s"Wrote ${script}")

        return script
      case _ => halt(o)
    }
  }

  def run(): Unit = {
    val reporter = Reporter.create

    for (project <- tests) {
      var reports: HashSMap[Cli.SireumHamrCodegenHamrPlatform.Type, Report] = HashSMap.empty

      if(!project.modelDir.exists) {
        halt(s"${project.modelDir} does not exist");
      }
      for (platform <- project.platforms) {

        println("***************************************")
        println(s"${project.modelDir} -- ${platform})")
        println("***************************************")

        val outputDir: Os.Path = platform match {
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_TB => project.modelDir / "CAmkES_seL4_TB"
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_Only => project.modelDir / "CAmkES_seL4_Only"
          case Cli.SireumHamrCodegenHamrPlatform.SeL4 => project.modelDir / "CAmkES_seL4"
          case Cli.SireumHamrCodegenHamrPlatform.Linux => project.modelDir / "Linux"
          case _ => halt("??")
        }

        val camkesOutputDir: Os.Path = platform match {
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_TB => outputDir
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_Only => outputDir
          case Cli.SireumHamrCodegenHamrPlatform.SeL4 => outputDir / "src/c/CAmkES_seL4"
          case Cli.SireumHamrCodegenHamrPlatform.Linux => outputDir / ""
          case _ => halt("??")
        }

        if(ops.StringOps(project.modelDir.value).contains("VMx")) {
          experimentalOptions = experimentalOptions :+ ExperimentalOptions.USE_CASE_CONNECTORS
        }

        val o = Cli.SireumHamrCodegenOption(
          help = "",
          args = ISZ(project.slangFile.value),
          msgpack = F,
          verbose = T,
          platform = platform,
          runtimeMonitoring = F,
          packageName = Some(project.simpleName),
          noProyekIve = F,
          noEmbedArt = F,
          devicesAsThreads = F,
          excludeComponentImpl = T,
          genSbtMill = T,

          bitWidth = 32,
          maxStringSize = 256,
          maxArraySize = 1,
          runTranspiler = runTranspiler,

          slangAuxCodeDirs = ISZ(),
          slangOutputCDir = None(),
          outputDir = Some(outputDir.value),

          camkesOutputDir = Some(camkesOutputDir.value),
          camkesAuxCodeDirs = ISZ(),
          aadlRootDir = Some(project.modelDir.value),

          experimentalOptions = experimentalOptions
        )

        outputDir.removeAll()

        val runHamrScript = generateRunScript(o)

        val exitCode: Z =
          if(USE_OSIREUM) {
            val results = Os.procs(runHamrScript.canon.value).console.runCheck()
            results.exitCode
          }
          else { Z(org.sireum.cli.HAMR.codeGen(o, reporter).toInt) }

        if(exitCode != 0) {
          halt(s"${project.simpleName} completed with ${exitCode}")
        }

        if(ops.StringOps(outputDir.value).contains("_VM")) {
          replaceVM()
        }

        if(shouldReport && isSel4(platform)) {
          val gen = ReadmeGenerator(o, reporter)

          if(gen.build()) {
            val timeout: Z = if(platform == Cli.SireumHamrCodegenHamrPlatform.SeL4) defTimeout else project.timeout

            val expectedOutput: ST =
              if(project.shouldSimulate) gen.simulate(timeout)
              else st"NEED TO MANUALLY UPDATE EXPECTED OUTPUT"

            val report = Report(
              readmeDir = project.modelDir,
              options = o,
              runHamrScript = Some(project.modelDir.relativize(runHamrScript)),
              timeout = project.timeout,
              runInstructions = gen.genRunInstructions(project.modelDir, Some(runHamrScript)),
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
        val readme = project.modelDir / "readme_autogen.md"
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


  def isSel4(platform: Cli.SireumHamrCodegenHamrPlatform.Type): B = {
    val ret: B = platform match {
      case Cli.SireumHamrCodegenHamrPlatform.SeL4 => T
      case Cli.SireumHamrCodegenHamrPlatform.SeL4_TB => T
      case Cli.SireumHamrCodegenHamrPlatform.SeL4_Only => T
      case _ => F
    }
    return ret
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