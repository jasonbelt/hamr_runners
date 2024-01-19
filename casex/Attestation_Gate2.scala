// #Sireum

package org.sireum.cli.hamr_runners.casex

import org.sireum._
import org.sireum.cli.hamr_runners.{DotFormat, ReadmeGenerator, ReadmeTemplate, Report}
import org.sireum.hamr.codegen.common.util.ExperimentalOptions
import org.sireum.message.Reporter

object Attestation_Gate2 extends App {

  @datatype class Project (simpleName: String,
                           modelDir: Os.Path,
                           slangFile: Os.Path,
                           platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type],
                           shouldSimulate: B,
                           timeout: Z)

  val USE_OSIREUM: B = F // if using osireum then may need to rebuild sireum.jar

  val shouldReport: B = F
  val skipBuild: B = T
  val regenReadmes: B = F

  val graphFormat: DotFormat.Type = DotFormat.svg
  val defTimeout: Z = 20000
  val vmTimeout: Z = 90000

  val jvm: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.JVM
  val linux: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.Linux
  val sel4: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4
  val sel4_tb: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4_TB
  val sel4_only: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4_Only

  val proj_dir: Os.Path = Os.home / "devel/case/case-loonwerks/TA5/tool-assessment-4/cakeml/"

  var experimentalOptions: ISZ[String] = ISZ(ExperimentalOptions.GENERATE_DOT_GRAPHS)

  def genFull(name: String, json: String, platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type], shouldSimulate: B, timeout: Z): Project = {
    val modelDir = proj_dir / name
    val simpleName = Os.path(name).name // get last dir name
    return Project(simpleName, modelDir, modelDir / ".slang" / json, platforms, shouldSimulate, timeout)
  }

  def gen(name: String, json: String, platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type]): Project = {
    return genFull(name, json, platforms, T, defTimeout)
  }

  val nonVmProjects: ISZ[Project] = ISZ(

    gen("attestation-gate", "SysContext_top_Impl_Instance.json", ISZ(
      //jvm,
      linux,
      sel4
    ))
  )

    //genFull("test_event_data_port_periodic_domains_VMx/sender_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only), T, vmTimeout)

  val tests: ISZ[Project] = nonVmProjects

  def getSystemOrProject(aadlDir: Os.Path): Os.Path = {
    val project: Os.Path = if((aadlDir / ".system").exists) aadlDir / ".system" else aadlDir / ".project"
    if(!project.exists) {
      halt(s"${project} doesn't exist")
    }
    return project
  }

  def regenSlangFile(aadlDir: Os.Path): B = {
    val results = Os.procs(s"sireum hamr phantom ${aadlDir}").console.runCheck()
    if(!results.ok) {
      halt("Regenerating AIR failed")
    }
    return results.ok
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

        val outputDir: Os.Path = project.modelDir / "CAmkES_seL4_2021"

        val camkesOutputDir: Option[String] = platform match {
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_TB => Some(outputDir.value)
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_Only => Some(outputDir.value)
          case Cli.SireumHamrCodegenHamrPlatform.SeL4 => Some((outputDir / "src/c/CAmkES_seL4").value)
          case _ => None()
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
          runTranspiler = !skipBuild,

          slangAuxCodeDirs = ISZ(),
          slangOutputCDir = None(),
          outputDir = Some(outputDir.value),

          camkesOutputDir = camkesOutputDir,
          camkesAuxCodeDirs = ISZ(),
          aadlRootDir = Some(project.modelDir.value),

          experimentalOptions = experimentalOptions
        )

        //outputDir.removeAll()

        val runHamrScript = generateRunScript(o)
        //regenSlangFile(project.modelDir)

        val result: Z = if(USE_OSIREUM) {
          val _result = Os.procs(runHamrScript.canon.value).console.runCheck()
          _result.exitCode
        } else {
          Z(org.sireum.cli.HAMR.codeGen(o, reporter).toInt)
        }

        if(result != 0) {
          halt(s"${project.simpleName} completed with ${result}")
        }

        if(shouldReport) {

          if(isSel4(platform)) {
            val gen = ReadmeGenerator(o, reporter)

            if(skipBuild ||gen.build()) {
              val timeout: Z = project.timeout

              val expectedOutput: Option[ST] =
                if(!skipBuild && project.shouldSimulate) Some(gen.simulate(timeout))
                else None()

              val report = Report(
                readmeDir = project.modelDir,
                options = o,
                runHamrScript = Some(project.modelDir.relativize(runHamrScript)),
                timeout = project.timeout,
                runInstructions = gen.genRunInstructions(project.modelDir, Some(runHamrScript)),
                expectedOutput = expectedOutput,
                aadlArchDiagram = gen.getAadlArchDiagram(),
                hamrCamkesArchDiagram = gen.getHamrCamkesArchDiagram(graphFormat),
                camkesArchDiagram = gen.getCamkesArchDiagram(graphFormat),
                symbolTable = None()
              )

              reports = reports + (platform ~> report)
            }
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


  def generateRunScript(o: Cli.SireumHamrCodegenOption): Os.Path = {
    o.aadlRootDir match {
      case Some(d) =>
        val aadlDir = Os.path(d)
        val oDir = Os.path(o.outputDir.get)

        val rOutputDir = aadlDir.relativize(oDir).value

        val platform: String = o.platform match {
          case Cli.SireumHamrCodegenHamrPlatform.JVM => "JVM"
          case Cli.SireumHamrCodegenHamrPlatform.Linux => "Linux"
          case Cli.SireumHamrCodegenHamrPlatform.Cygwin => "Cygwin"
          case Cli.SireumHamrCodegenHamrPlatform.MacOS => "MacOs"
          case Cli.SireumHamrCodegenHamrPlatform.SeL4 => "seL4"
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_Only => "seL4_Only"
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_TB => "seL4_TB"
        }

        val bs: String = "\\"

        var sts: ISZ[ST] = ISZ()
        if(o.verbose) { sts = sts :+ st"--verbose ${bs}"}

        sts = sts :+ st"--platform $platform ${bs}"
        sts = sts :+ st"--output-dir $$AADL_DIR/${rOutputDir.value} ${bs}"
        sts = sts :+ st"--package-name ${o.packageName.get} ${bs}"
        sts = sts :+ st"--aadl-root-dir $$AADL_DIR ${bs}"

        if(o.platform == Cli.SireumHamrCodegenHamrPlatform.Linux || o.platform == Cli.SireumHamrCodegenHamrPlatform.SeL4) {
          sts = sts ++ ISZ(
            st"--bit-width ${o.bitWidth} ${bs}",
            st"--max-string-size ${o.maxStringSize} ${bs}",
            st"--max-array-size ${o.maxArraySize} ${bs}")
          if(o.excludeComponentImpl) { sts = sts :+ st"--exclude-component-impl ${bs}" }
          if(o.runTranspiler) { sts = sts :+ st"--run-transpiler ${bs}" }
        }

        if(o.platform == Cli.SireumHamrCodegenHamrPlatform.SeL4) {
          val camkesDir = Os.path(o.camkesOutputDir.get)
          val rCamkesOutputDir = aadlDir.relativize(camkesDir).value
          sts = sts :+ st"--camkes-output-dir $$AADL_DIR/${rCamkesOutputDir.value} ${bs}"
        }

        if(o.experimentalOptions.nonEmpty) {
          sts = sts :+ st"""--experimental-options \"${(o.experimentalOptions, ";")}\" ${bs}"""
        }

        val project = getSystemOrProject(aadlDir)
        val rProject = aadlDir.relativize(project)

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
              |  ${(sts, "\n")}
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