// #Sireum

package org.sireum.cli.hamr_runners.hamr_case_examples.tool_eval_4

import org.sireum._
import org.sireum.Cli.SireumHamrCodegenHamrPlatform
import org.sireum.hamr.codegen.common.util.ExperimentalOptions
import org.sireum.message.Reporter

object CaseToolEval4_vm extends App {

  @datatype class Project (simpleName: String,
                           rootDir: Os.Path,
                           aadlDir: Os.Path,
                           slangFile: Os.Path,
                           platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type],
                           shouldSimulate: B,
                           timeout: Z,
                           gitReplace: ISZ[String])
  val USE_OSIREUM: B = F

  val shouldReport: B = T
  val graphFormat: DotFormat.Type = DotFormat.svg
  val runTranspiler: B = T
  val defTimeout: Z = 18000
  val vmTimeout: Z = 90000

  //val linux: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.Linux
  val sel4: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4
  val sel4_tb: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4_TB
  val sel4_only: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4_Only

  val case_tool_evaluation_dir: Os.Path = Os.home / "devel/hamr-case-examples/case/tool-evaluation-4"

  var experimentalOptions: ISZ[String] = ISZ(ExperimentalOptions.GENERATE_DOT_GRAPHS)

  def genFull(name: String, json: String, platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type], shouldSimulate: B, timeout: Z, gitReplace: ISZ[String]): Project = {
    val rootDir = case_tool_evaluation_dir / name
    val aadlDir = rootDir / "aadl"
    val simpleName = Os.path(name).name // get last dir name
    return Project(simpleName, rootDir, aadlDir, aadlDir / ".slang" / json, platforms, shouldSimulate, timeout, gitReplace)
  }

  def gen(name: String, json: String, platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type], gitReplace: ISZ[String]): Project = {
    return genFull(name, json, platforms, T, defTimeout, gitReplace)
  }

  val nonVmProjects: ISZ[Project] = ISZ(
    gen("simple_uav", "UAV_UAV_Impl_Instance.json", ISZ(sel4_tb, sel4_only), ISZ()),

    gen("test_data_port", "test_data_port_top_impl_Instance.json", ISZ(sel4_tb, sel4_only), ISZ()),
    gen("test_data_port_periodic", "test_data_port_periodic_top_impl_Instance.json", ISZ(sel4_tb, sel4_only), ISZ()),
    gen("test_data_port_periodic_fan_out", "test_data_port_periodic_fan_out_top_impl_Instance.json", ISZ(sel4_tb, sel4_only), ISZ()),
    gen("test_event_data_port", "test_event_data_port_top_impl_Instance.json", ISZ(sel4_tb, sel4_only), ISZ()),
    gen("test_event_data_port_fan_out", "test_event_data_port_fan_out_top_impl_Instance.json", ISZ(sel4_tb, sel4_only), ISZ()),


    gen("test_event_port", "test_event_port_top_impl_Instance.json", ISZ(sel4_tb, sel4_only), ISZ()),

    gen("test_event_port_fan_out", "test_event_port_fan_out_top_impl_Instance.json", ISZ(sel4_tb, sel4_only), ISZ()),
    gen("test_data_port_periodic_domains", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_tb, sel4_only, sel4), ISZ(
      "hamr_seL4/c/ext-c/destination_thread_impl_destination_process_component_destination_thread_component/destination_thread_impl_destination_process_component_destination_thread_component.c",
      "hamr_seL4/c/ext-c/source_thread_impl_source_process_component_source_thread_component/source_thread_impl_source_process_component_source_thread_component.c"
    )),
    gen("test_event_data_port_periodic_domains", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_tb, sel4_only, sel4), ISZ(
      "hamr_seL4/c/ext-c/emitter_t_impl_src_process_src_thread/emitter_t_impl_src_process_src_thread.c",
      "hamr_seL4/c/ext-c/consumer_t_impl_dst_process_dst_thread/consumer_t_impl_dst_process_dst_thread.c"
    )),
    gen("test_event_port_periodic_domains", "test_event_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_tb, sel4_only, sel4), ISZ(
      "hamr_seL4/c/ext-c/consumer_t_impl_dst_process_dst_thread/consumer_t_impl_dst_process_dst_thread.c",
      "hamr_seL4/c/ext-c/emitter_t_impl_src_process_src_thread/emitter_t_impl_src_process_src_thread.c"
    )),
  )

  val vmProjects: ISZ[Project] = ISZ(
    // VMs
    // DATA PORTS
    genFull("test_data_port_periodic_domains_VM/receiver_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), F, vmTimeout, ISZ(
      "hamr_seL4_Only/camkes/components/VM/apps/vmdst_process",

      "hamr_seL4/camkes/components/VM/apps",
      "hamr_seL4/c/ext-c/emitter_t_impl_src_process_src_thread/emitter_t_impl_src_process_src_thread.c"
    )),
    genFull("test_data_port_periodic_domains_VM/sender_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only),
      // note seL4 is not supported as the sender being in a VM means the native receiver's data port will not be
      // initialized when it enter's it's compute phase and therefore will crash with UNEXPECTED TYPE: 0
      F, vmTimeout, ISZ(
      "hamr_seL4_Only/camkes/components/VM/apps/vmsrc_process", // entire app directory
    )),

    genFull("test_data_port_periodic_domains_VM/both_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), F, vmTimeout, ISZ(
      "hamr_seL4_Only/camkes/components/VM/apps",
      "hamr_seL4/camkes/components/VM/apps"
    )),


    // EVENT DATA PORTS
    genFull("test_event_data_port_periodic_domains_VM/receiver_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), F, vmTimeout, ISZ(
      "hamr_seL4_Only/camkes/components/VM/apps/vmdst_process",

      "hamr_seL4/c/ext-c/emitter_t_impl_src_process_src_thread/emitter_t_impl_src_process_src_thread.c",
      "hamr_seL4/camkes/components/VM/apps/vmdst_process"
    )),

    genFull("test_event_data_port_periodic_domains_VM/sender_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), F, vmTimeout, ISZ(
      "hamr_seL4_Only/camkes/components/VM/apps/vmsrc_process",

      "hamr_seL4/c/ext-c/consumer_t_impl_dst_process_dst_thread/consumer_t_impl_dst_process_dst_thread.c",
     "hamr_seL4/camkes/components/VM/apps/vmsrc_process",
    )),

    genFull("test_event_data_port_periodic_domains_VM/both_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), F, vmTimeout, ISZ(
      "hamr_seL4_Only/camkes/components/VM/apps",

      "hamr_seL4/camkes/components/VM/apps"
    )),

    // VMs with Kent's connector
    //genFull("test_event_data_port_periodic_domains_VMx/receiver_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only), T, vmTimeout),


    // VMs which can't use init trick
    //genFull("test_data_port_periodic_domains_VM/both_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), T, vmTimeout, ISZ()),
    //TODO//genFull("test_event_data_port_periodic_domains_VM/both_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4), T, vmTimeout),

    // VMs which can't use init trick -- using Kent's connector
    //TODO//genFull("test_event_data_port_periodic_domains_VMx/sender_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only), T, vmTimeout)
  )

  //val tests: ISZ[Project] = nonVmProjects
  //val tests: ISZ[Project] = nonVmProjects ++ vmProjects
  val tests: ISZ[Project] = vmProjects

  def isCamkesProject(platform: SireumHamrCodegenHamrPlatform.Type): Boolean = {
    val ret: B = platform match {
      case SireumHamrCodegenHamrPlatform.SeL4 => T
      case SireumHamrCodegenHamrPlatform.SeL4_TB => T
      case SireumHamrCodegenHamrPlatform.SeL4_Only => T
      case _ => F
    }
    return ret
  }

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

        val slangOptions: Option[ST] = if(o.platform == Cli.SireumHamrCodegenHamrPlatform.SeL4) {
          val exclude: Option[ST] = if(o.excludeComponentImpl) Some(st"--exclude-component-impl ${bs}") else None()
          Some(st"""--output-dir $$AADL_DIR/${rOutputDir.value} ${bs}
                   |--package-name ${o.packageName.get} ${bs}
                   |${exclude}
                   |--bit-width ${o.bitWidth} ${bs}
                   |--max-string-size ${o.maxStringSize} ${bs}
                   |--max-array-size ${o.maxArraySize} ${bs}
                   |--run-transpiler ${bs}""")
        } else { None() }

        val sel4Options: Option[ST] =
          if(isCamkesProject(o.platform)) {
            Some(st"""--camkes-output-dir $$AADL_DIR/${rCamkesOutputDir.value} ${bs}""")
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
              |  ${slangOptions}
              |  ${sel4Options}
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

      if(!project.rootDir.exists || !project.aadlDir.exists) {
        halt(s"${project.rootDir} or ${project.aadlDir} does not exist");
      }

      for (platform <- project.platforms) {

        println("***************************************")
        println(s"${project.rootDir} -- ${platform})")
        println("***************************************")

        val rootOutputDir: Os.Path = platform match {
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_TB => project.rootDir / "hamr_seL4_TB"
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_Only => project.rootDir / "hamr_seL4_Only"
          case Cli.SireumHamrCodegenHamrPlatform.SeL4 => project.rootDir / "hamr_seL4"
          //case Cli.SireumHamrCodegenHamrPlatform.Linux => project.modelDir / "Linux"
          case _ => halt("??")
        }

        val cOutputDir = rootOutputDir / "c"
        val camkesOutputDir: Os.Path = rootOutputDir / "camkes"
        val slangOutputDir = rootOutputDir / "slang"

        if(ops.StringOps(project.rootDir.value).contains("VMx")) {
          experimentalOptions = experimentalOptions :+ ExperimentalOptions.USE_CASE_CONNECTORS
        }

        val o = Cli.SireumHamrCodegenOption(
          help = "",
          args = ISZ(project.slangFile.value),
          msgpack = F,
          verbose = T,
          platform = platform,

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
          slangOutputCDir = Some(cOutputDir.value),
          outputDir = Some(slangOutputDir.value),

          camkesOutputDir = Some(camkesOutputDir.value),
          camkesAuxCodeDirs = ISZ(),
          aadlRootDir = Some(project.aadlDir.value),

          experimentalOptions = experimentalOptions
        )

        rootOutputDir.removeAll()

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

        for(r <- project.gitReplace) {
          proc"git checkout ${r}".at(project.rootDir).console.runCheck()
        }

        if(shouldReport && isSel4(platform)) {
          val gen = ReadmeGenerator_te4(o, reporter)

          if(gen.build()) {
            val timeout: Z = if(platform == Cli.SireumHamrCodegenHamrPlatform.SeL4) defTimeout else project.timeout

            val expectedOutput: ST =
              if(project.shouldSimulate) gen.simulate(timeout)
              else st"NEED TO MANUALLY UPDATE EXPECTED OUTPUT"

            val report = Report(
              readmeDir = project.rootDir,
              options = o,
              runHamrScript = Some(runHamrScript),
              timeout = project.timeout,
              runInstructions = gen.genRunInstructions(project.rootDir, Some(runHamrScript)),
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

      println()

      if(shouldReport) {
        val readme = project.rootDir / "readme_autogen.md"

        if(readme.exists) {
          ReadmeTemplate_te4.existingReadmeContents = ops.StringOps(readme.read)
        }

        val readmest = ReadmeTemplate_te4.generateReport(project.simpleName, reports)

        if(readme.exists) {
          readme.writeOver(ReadmeTemplate_te4.existingReadmeContents.s)
        } else {
          readme.writeOver(readmest.render)
        }

        readme.writeOver(readmest.render)
        println(s"Wrote: ${readme}")
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

}