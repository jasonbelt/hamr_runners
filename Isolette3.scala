// #Sireum

package org.sireum.cli.hamr_runners

import org.sireum.Cli.HamrPlatform
import org.sireum._
import org.sireum.hamr.codegen.common.util.ExperimentalOptions
import org.sireum.message.Reporter

object Isolette3 extends App {

  @datatype class Project (simpleName: String,
                           modelDir: Os.Path,
                           slangFile: Os.Path,
                           platforms: ISZ[Cli.HamrPlatform.Type],
                           shouldSimulate: B,
                           timeout: Z)

  val USE_OSIREUM: B = F

  val shouldReport: B = F
  val graphFormat: DotFormat.Type = DotFormat.svg
  val runTranspiler: B = T
  val defTimeout: Z = 15000
  val vmTimeout: Z = 90000

  val jvm: Cli.HamrPlatform.Type = Cli.HamrPlatform.JVM
  val linux: Cli.HamrPlatform.Type = Cli.HamrPlatform.Linux
  val sel4: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4
  val sel4_tb: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_TB
  val sel4_only: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_Only

  val proj_dir: Os.Path = Os.home / "devel/slang-embedded/isolette"

  var experimentalOptions: ISZ[String] = ISZ(ExperimentalOptions.GENERATE_DOT_GRAPHS)

  def genFull(name: String, json: String, platforms: ISZ[Cli.HamrPlatform.Type], shouldSimulate: B, timeout: Z): Project = {
    val modelDir = proj_dir / name
    val simpleName = Os.path(name).name // get last dir name
    return Project(simpleName, modelDir, modelDir / ".slang" / json, platforms, shouldSimulate, timeout)
  }

  def gen(name: String, json: String, platforms: ISZ[Cli.HamrPlatform.Type]): Project = {
    return genFull(name, json, platforms, T, defTimeout)
  }

  // /home/vagrant/temp/vpm_ben/VPM_0/.slang/VPM_VPM_8600B_Ext_impl_Instance.json

  val nonVmProjects: ISZ[Project] = ISZ(

    gen("isolette_v2/aadl", "Isolette_isolette_single_sensor_Instance.json", ISZ(
      //jvm,
      linux,
      //sel4
    )),
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

  def generateRunScript(o: Cli.HamrCodeGenOption): Os.Path = {
    o.aadlRootDir match {
      case Some(d) =>
        val aadlDir = Os.path(d)
        val oDir = Os.path(o.outputDir.get)
        val camkesDir = Os.path(o.camkesOutputDir.get)

        val rOutputDir = aadlDir.relativize(oDir).value
        val rCamkesOutputDir = aadlDir.relativize(camkesDir).value

        val platform: String = o.platform match {
          case Cli.HamrPlatform.JVM => "JVM"
          case Cli.HamrPlatform.Linux => "Linux"
          case Cli.HamrPlatform.Cygwin => "Cygwin"
          case Cli.HamrPlatform.MacOS => "MacOs"
          case Cli.HamrPlatform.SeL4 => "seL4"
          case Cli.HamrPlatform.SeL4_Only => "seL4_Only"
          case Cli.HamrPlatform.SeL4_TB => "seL4_TB"
        }

        var sts: ISZ[ST] = ISZ()
        if(o.verbose) { sts = sts :+ st"--verbose \\"}

        sts = sts :+ st"--platform $platform \\"
        sts = sts :+ st"--output-dir $$AADL_DIR/${rOutputDir.value} \\"
        sts = sts :+ st"--package-name ${o.packageName.get} \\"
        sts = sts :+ st"--aadl-root-dir $$AADL_DIR \\"

        if(o.platform == Cli.HamrPlatform.Linux || o.platform == Cli.HamrPlatform.SeL4) {
            sts = sts ++ ISZ(
              st"--bit-width ${o.bitWidth} \\",
              st"--max-string-size ${o.maxStringSize} \\",
              st"--max-array-size ${o.maxArraySize} \\")
            if(o.excludeComponentImpl) { sts = sts :+ st"--exclude-component-impl \\" }
            if(o.runTranspiler) { sts = sts :+ st"--run-transpiler \\" }
        }

        if(o.platform == Cli.HamrPlatform.SeL4) {
          sts = sts :+ st"--camkes-output-dir $$AADL_DIR/${rCamkesOutputDir.value} \\"
        }

        if(o.experimentalOptions.nonEmpty) {
           sts = sts :+ st"""--experimental-options \"${(o.experimentalOptions, ";")}\" \"""
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
              |eval "$$OSIREUM hamr codegen \
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
      var reports: HashSMap[Cli.HamrPlatform.Type, Report] = HashSMap.empty

      if(!project.modelDir.exists) {
        halt(s"${project.modelDir} does not exist");
      }
      for (platform <- project.platforms) {

        println("***************************************")
        println(s"${project.modelDir} -- ${platform})")
        println("***************************************")

        /*
        val outputDir: Os.Path = platform match {
          case Cli.HamrPlatform.SeL4_TB => project.modelDir / "CAmkES_seL4_TB"
          case Cli.HamrPlatform.SeL4_Only => project.modelDir / "CAmkES_seL4_Only"
          case Cli.HamrPlatform.SeL4 => project.modelDir / "CAmkES_seL4"
          case Cli.HamrPlatform.Linux => project.modelDir / "Linux"
          case _ => halt("??")
        }

        val camkesOutputDir: Os.Path = platform match {
          case Cli.HamrPlatform.SeL4_TB => outputDir
          case Cli.HamrPlatform.SeL4_Only => outputDir
          case Cli.HamrPlatform.SeL4 => outputDir / "src/c/CAmkES_seL4"
          case Cli.HamrPlatform.Linux => outputDir / ""
          case _ => halt("??")
        }
         */

        if(ops.StringOps(project.modelDir.value).contains("VMx")) {
          experimentalOptions = experimentalOptions :+ ExperimentalOptions.USE_CASE_CONNECTORS
        }

        val outputDir = project.modelDir.up / "hamr"
        val camkesOutputDir = outputDir / "src" / "c" / "camkes"

        val o = Cli.HamrCodeGenOption(
          help = "",
          args = ISZ(project.slangFile.value),
          msgpack = F,
          verbose = T,
          platform = platform,

          packageName = Some("isolette"),
          noEmbedArt = F,
          devicesAsThreads = F,
          excludeComponentImpl = F,

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

        //outputDir.removeAll()

        val runHamrScript = generateRunScript(o)
        regenSlangFile(project.modelDir)

        val result: Z = if(USE_OSIREUM) {
          val _result = Os.procs(runHamrScript.canon.value).console.runCheck()
          _result.exitCode
        } else {
          Z(org.sireum.cli.HAMR.codeGen(o).intValue)
        }

        if(result != 0) {
          halt(s"${project.simpleName} completed with ${result}")
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
              runHamrScript = Some(project.modelDir.relativize(runHamrScript)),
              timeout = project.timeout,
              runInstructions = gen.genRunInstructions(project.modelDir, Some(runHamrScript)),
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


  def isSel4(platform: HamrPlatform.Type): B = {
    return platform match {
      case HamrPlatform.SeL4 => T
      case HamrPlatform.SeL4_TB => T
      case HamrPlatform.SeL4_Only => T
      case _ => F
    }
  }
}