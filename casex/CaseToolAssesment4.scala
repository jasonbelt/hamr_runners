// #Sireum

package org.sireum.cli.hamr_runners.casex

import org.sireum._
import org.sireum.Cli.HamrPlatform
import org.sireum.cli.hamr_runners.{DotFormat, ReadmeGenerator, ReadmeTemplate, Report}
import org.sireum.hamr.codegen.common.util.ExperimentalOptions
import org.sireum.message.Reporter

object CaseToolAssesment4 extends App {

  @datatype class Project (basePackageName: String,

                           rootDir: Os.Path,
                           aadlDir: Os.Path,
                           hamrDir: Os.Path,

                           slangFile: Os.Path,

                           options: ISZ[Cli.HamrCodeGenOption],
                           shouldSimulate: B,
                           timeout: Z)
  val USE_OSIREUM: B = F


  val shouldReport: B = T
  val skipBuild: B = T
  val replaceReadmes: B = F
  val simulateKillSwitch: B = T

  val defTimeout: Z = 18000
  val vmTimeout: Z = 90000

  val graphFormat: DotFormat.Type = DotFormat.svg

  val linux: Cli.HamrPlatform.Type = Cli.HamrPlatform.Linux
  val sel4: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4
  val sel4_tb: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_TB
  val sel4_only: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_Only

  val case_tool_evaluation_dir: Os.Path = Os.home / "devel/case/case-loonwerks/TA5/tool-assessment-4"

  var experimentalOptions: ISZ[String] = ISZ(ExperimentalOptions.GENERATE_DOT_GRAPHS)

  val nonVmProjects: ISZ[Project] = ISZ(

    gen("basic/test_data_port_periodic_domains", "base", ISZ(linux, sel4)),
    gen("basic/test_event_data_port_periodic_domains", "base", ISZ(linux, sel4)),
    gen("basic/test_event_port_periodic_domains", "base", ISZ(linux, sel4)),

    gen("basic/tutorial", "base", ISZ(linux, sel4)),

    gen("bit-codec/producer-filter-consumer", "pfc", ISZ(linux, sel4)),

    gen("cakeml/attestation-gate", "attestation-gate", ISZ(linux, sel4)),
  )

  val vmProjects: ISZ[Project] = {
    //val phase2: Project = {
    //  val proj = gen("phase2", "hamr", ISZ(linux, sel4))
    //  proj(options = proj.options.map(o => o(slangAuxCodeDirs = ISZ((proj.aadlDir / "c_libraries/CMASI").value, (proj.aadlDir / "c_libraries/hexdump").value, (proj.aadlDir / "c_libraries/dummy_serial_server").value))))
    //}

    ISZ(
      genVM (F,"vm/test_data_port_periodic_domains_VM/receiver_vm", "base", ISZ(sel4)),

      genVM (F,"vm/test_event_data_port_periodic_domains_VM/both_vm", "base", ISZ(sel4)),
      genVM (F,"vm/test_event_data_port_periodic_domains_VM/receiver_vm", "base", ISZ(sel4)),
      genVM (F,"vm/test_event_data_port_periodic_domains_VM/sender_vm", "base", ISZ(sel4)),

      //phase2
    )
  }

  //val tests: ISZ[Project] = nonVmProjects
  val tests: ISZ[Project] = nonVmProjects ++ vmProjects
  //val tests: ISZ[Project] = vmProjects


  def run(): Unit = {
    val reporter = Reporter.create

    for (project <- tests) {
      var reports: HashSMap[Cli.HamrPlatform.Type, Report] = HashSMap.empty

      if(!project.rootDir.exists) {
        halt(s"${project.rootDir} does not exist");
      }

      for (option <- project.options) {

        println("***************************************")
        println(s"${project.rootDir} -- ${option.platform})")
        println("***************************************")

        //outputDir.removeAll()

        val runHamrScript = generateRunScript(option)

        val exitCode: Z =
          if(USE_OSIREUM) {
            val results = Os.procs(runHamrScript.canon.value).console.runCheck()
            results.exitCode
          }
          else { Z(org.sireum.cli.HAMR.codeGen(option).toInt) }

        if(exitCode != 0) {
          halt(s"${project.rootDir.name} completed with ${exitCode}")
        }

        if(shouldReport) {
          if(isNix(option.platform)) {
            val gen = ReadmeGenerator(option, reporter)

            val outputDir = Os.path(option.outputDir.get)
            val cDir = Os.path(option.slangOutputCDir.get)

            val report = Report(
              readmeDir = project.rootDir,
              options = option,
              runHamrScript = Some(runHamrScript),
              timeout = 0,
              runInstructions = gen.genLinuxRunInstructions(project.rootDir, Some(runHamrScript), outputDir, cDir),
              expectedOutput = None(),
              aadlArchDiagram = gen.getAadlArchDiagram(),
              hamrCamkesArchDiagram = None(),
              camkesArchDiagram = None(),
              symbolTable = Some(gen.symbolTable)
            )
            reports = reports + (option.platform ~> report)
          }

          if(isSel4(option.platform)) {
            val gen = ReadmeGenerator(option, reporter)

            if(skipBuild || gen.build()) {
              val timeout: Z = project.timeout

              val expectedOutput: Option[ST] =
                if(!skipBuild && !simulateKillSwitch && project.shouldSimulate) Some(gen.simulate(timeout))
                else None()

              val report = Report(
                readmeDir = project.rootDir,
                options = option,
                runHamrScript = Some(runHamrScript),
                timeout = project.timeout,
                runInstructions = gen.genRunInstructions(project.rootDir, Some(runHamrScript)),
                expectedOutput = expectedOutput,
                aadlArchDiagram = gen.getAadlArchDiagram(),
                hamrCamkesArchDiagram = gen.getHamrCamkesArchDiagram(graphFormat),
                camkesArchDiagram = gen.getCamkesArchDiagram(graphFormat),
                symbolTable = Some(gen.symbolTable)
              )

              reports = reports + (option.platform ~> report)
            }
          }
        }
      }

      if(shouldReport) {
        val readme = project.rootDir / "readme.md"

        if(replaceReadmes && readme.exists){
          readme.remove()
          println(s"Removed: ${readme}")
        }

        ReadmeTemplate.replaceExampleOutputSections = !skipBuild

        if(readme.exists){
          ReadmeTemplate.existingReadmeContents = ops.StringOps(readme.read)
        }

        val readmest = ReadmeTemplate.generateReport(project.rootDir.name, reports)

        if(readme.exists){
          readme.writeOver(ReadmeTemplate.existingReadmeContents.s)
        } else {
          readme.writeOver(readmest.render)
        }
        println(s"Wrote: ${readme}")
      }
    }

    if(reporter.hasError) {
      eprintln(s"Reporter Errors:")
      reporter.printMessages()
    }
  }


  def generateRunScript(o: Cli.HamrCodeGenOption): Os.Path = {
    o.aadlRootDir match {
      case Some(d) =>
        val aadlDir = Os.path(d)
        val rootDir = aadlDir.up
        val slangDir = Os.path(o.outputDir.get)
        val cDir = Os.path(o.slangOutputCDir.get)
        val camkesDir = Os.path(o.camkesOutputDir.get)

        val project: Os.Path = if((aadlDir / ".system").exists) aadlDir / ".system" else aadlDir / ".project"
        if(!project.exists) {
          halt(s"${project} doesn't exist")
        }

        val rOutputDir = rootDir.relativize(slangDir).value
        val rCOutputDir = rootDir.relativize(cDir).value
        val rCamkesOutputDir = rootDir.relativize(camkesDir).value

        val sel4Options: Option[ST] =
          if(o.platform == Cli.HamrPlatform.SeL4) Some(st"""--camkes-output-dir $$ROOT_DIR/${rCamkesOutputDir.value} \""")
          else None()

        val sharedCOptions: Option[ST] =
          if(o.platform == Cli.HamrPlatform.SeL4 || o.platform == Cli.HamrPlatform.Linux) {
            val auxCodeDirOpt: Option[ST] = if(o.slangAuxCodeDirs.nonEmpty) {
                val entries: ISZ[String] = o.slangAuxCodeDirs.map(acd => {
                  assert(Os.path(acd).exists, s"${acd} doesn't exist")
                  s"$$AADL_DIR/${aadlDir.relativize(Os.path(acd))}"
                })
                Some(st"""--aux-code-dirs ${(entries, ";")} \""")
              } else { None() }

            Some(st"""--output-c-dir $$ROOT_DIR/${rCOutputDir} \
                     |--exclude-component-impl \
                     |--bit-width ${o.bitWidth} \
                     |--max-string-size ${o.maxStringSize} \
                     |--max-array-size ${o.maxArraySize} \
                     |${auxCodeDirOpt}
                     |--run-transpiler \""")
            } else {None()}

        val eOptions: Option[ST] = if(o.experimentalOptions.nonEmpty)
          Some(st"""--experimental-options \"${(o.experimentalOptions, ";")}\" \""")
        else None()

        val rProject = aadlDir.relativize(project)

        val platform: String = o.platform match {
          case Cli.HamrPlatform.JVM => "JVM"
          case Cli.HamrPlatform.Linux => "Linux"
          case Cli.HamrPlatform.Cygwin => "Cygwin"
          case Cli.HamrPlatform.MacOS => "MacOs"
          case Cli.HamrPlatform.SeL4 => "seL4"
          case Cli.HamrPlatform.SeL4_Only => "seL4_Only"
          case Cli.HamrPlatform.SeL4_TB => "seL4_TB"
        }

        val s =
          st"""#!/bin/bash -ei
              |
              |SCRIPT_DIR=$$( cd "$$( dirname "$$0" )" &> /dev/null && pwd )
              |AADL_DIR=$$SCRIPT_DIR/..
              |ROOT_DIR=$$SCRIPT_DIR/../..
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
              |eval "$$OSIREUM hamr codegen \
              |  --verbose \
              |  --platform $platform \
              |  --package-name ${o.packageName.get} \
              |  --output-dir $$ROOT_DIR/${rOutputDir.value} \
              |  ${sharedCOptions}
              |  ${sel4Options}
              |  --aadl-root-dir $$AADL_DIR \
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


  def isNix(platform: HamrPlatform.Type): B = {
    val ret: B = platform match {
      case HamrPlatform.Linux => T
      case HamrPlatform.Cygwin => T
      case HamrPlatform.MacOS => T
      case _ => F
    }
    return ret
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


  def genFull(path: String, basePackageName: String, platforms: ISZ[Cli.HamrPlatform.Type], shouldSimulate: B, timeout: Z): Project = {
    val rootDir = case_tool_evaluation_dir / path
    val aadlDir = rootDir / "aadl"
    val hamrDir = rootDir / "hamr"
    val json: Os.Path = {
      val cands = (aadlDir / ".slang").list.filter(f => f.ext == "json")
      assert(cands.size == 1, s"${aadlDir}: ${cands.size}")
      cands(0)
    }
    var options: ISZ[Cli.HamrCodeGenOption] = ISZ()
    for(platform <- platforms) {
      platform match {
        case Cli.HamrPlatform.SeL4_Only => halt("")
        case Cli.HamrPlatform.SeL4_TB =>halt("")
        case _ =>
      }

      val slangOutputDir: Os.Path = hamrDir / "slang"
      val cOutputDir: Os.Path = hamrDir / "c"
      val camkesOutputDir: Os.Path = hamrDir / "camkes"

      hamrDir.mkdir()
      (aadlDir / "hamr").mklink(hamrDir)

      //if(ops.StringOps(project.modelDir.value).contains("VMx")) {
      //  experimentalOptions = experimentalOptions :+ ExperimentalOptions.USE_CASE_CONNECTORS
      //}

      val maxArraySize: Z = if(basePackageName == "pfc") 3 else 1

      val o = Cli.HamrCodeGenOption(
        help = "",
        args = ISZ(json.value),
        msgpack = F,
        verbose = T,
        platform = platform,

        packageName = Some(basePackageName),
        noEmbedArt = F,
        devicesAsThreads = F,
        excludeComponentImpl = T,

        bitWidth = 32,
        maxStringSize = 256,
        maxArraySize = maxArraySize,
        runTranspiler = !skipBuild,

        slangAuxCodeDirs = ISZ(),
        slangOutputCDir = Some(cOutputDir.value),
        outputDir = Some(slangOutputDir.value),

        camkesOutputDir = Some(camkesOutputDir.value),
        camkesAuxCodeDirs = ISZ(),
        aadlRootDir = Some(aadlDir.value),

        experimentalOptions = experimentalOptions
      )
      options = options :+ o
    }
    return Project(basePackageName, rootDir, aadlDir, hamrDir, json, options, shouldSimulate, timeout)
  }

  def gen(path: String, basePackageName: String, platforms: ISZ[Cli.HamrPlatform.Type]): Project = {
    return genFull(path, basePackageName, platforms, T, defTimeout)
  }

  def genVM(shouldSim: B, path: String, basePackageName: String, platforms: ISZ[Cli.HamrPlatform.Type]): Project = {
    return genFull(path, basePackageName, platforms, shouldSim, vmTimeout)
  }
}