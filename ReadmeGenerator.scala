// #Sireum

package org.sireum.cli.hamr_runners

import org.sireum._
import org.sireum.Cli.HamrPlatform
import org.sireum.hamr.act.util.PathUtil
import org.sireum.hamr.codegen.common.StringUtil
import org.sireum.hamr.codegen.common.properties.PropertyUtil
import org.sireum.hamr.ir
import org.sireum.message.Reporter
import org.sireum.hamr.codegen.common.symbols.{SymbolResolver, SymbolTable}
import org.sireum.hamr.codegen.common.types.TypeResolver
import org.sireum.hamr.ir.{JSON => irJSON, MsgPack => irMsgPack}

@record class ReadmeGenerator(o: Cli.HamrCodeGenOption, reporter: Reporter) {

  val airFile: Os.Path = {
    val path = Os.path(o.args(0))
    assert(path.exists, s"${path} does not exist")
    path
  }

  val model: ir.Aadl = ReadmeGenerator.getModel(airFile, o.msgpack)

  val symbolTable: SymbolTable = ReadmeGenerator.getSymbolTable(model)

  val camkesOutputDir: Os.Path = Os.path(o.camkesOutputDir.get)
  val slangOutputDir: Os.Path = Os.path(o.outputDir.get)

  val OPT_CAKEML_ASSEMBLIES_PRESENT: String = "-DCAKEML_ASSEMBLIES_PRESENT=ON"
  val OPT_USE_PRECONFIGURED_ROOTFS: String = "-DUSE_PRECONFIGURED_ROOTFS=ON"

  def build(): B = {

    val run_camkes: Os.Path = ReadmeGenerator.getRunCamkesScript(camkesOutputDir)

    assert(run_camkes.exists, s"${run_camkes} not found")

    var continue: B = T
    val transpileSel4: Os.Path = ReadmeGenerator.getTranspileSel4Script(slangOutputDir)
    if(transpileSel4.exists) {
      continue = ReadmeGenerator.run(ISZ(transpileSel4.value), reporter)
    }

    if(continue) {
      var args: ISZ[String] = ISZ(run_camkes.value, "-n")

      var camkesOptions: ISZ[String] = ISZ()
      if (symbolTable.hasCakeMLComponents()) {
        camkesOptions = camkesOptions :+ OPT_CAKEML_ASSEMBLIES_PRESENT
      }
      if(symbolTable.hasVM()) {
        camkesOptions = camkesOptions :+ OPT_USE_PRECONFIGURED_ROOTFS
      }
      if(camkesOptions.nonEmpty){
        args = args :+ "-o" :+ st"""${(camkesOptions, ";")}""".render
      }

      continue = ReadmeGenerator.run(args, reporter)
    }

    return continue
  }

  def simulate(timeout: Z): ST = {
    val simulateScript: Os.Path = ReadmeGenerator.getCamkesSimulatePath(o, symbolTable)
    assert(simulateScript.exists, s"${simulateScript} does not exist")

    println(s"Simulating for ${timeout/1000} seconds")

    val p = Os.proc(ISZ(simulateScript.value)).timeout(timeout)

    val results = p.at(simulateScript.up).run()
    cprint(F, results.out)
    cprint(T, results.err)

    ReadmeGenerator.run(ISZ("pkill", "qemu"), reporter)

    val output = ReadmeGenerator.parseOutput(results.out)
    assert(output.nonEmpty, "Expected output missing")

    return st"${output.get}"
  }

  def genRunInstructions(root: Os.Path, osireumScript: Option[Os.Path]): ST = {

    val transpileSel4: Os.Path = ReadmeGenerator.getTranspileSel4Script(slangOutputDir)
    val runScript: Os.Path = ReadmeGenerator.getRunCamkesScript(camkesOutputDir)
    val cakeMlScript: Os.Path = transpileSel4.up / "compile-cakeml.sh"
    val caseArmVmSetupScript: Os.Path = runScript.up / Os.path(PathUtil.CAMKES_ARM_VM_SCRIPT_PATH).name

    val caseArmVmSetup: Option[ST] =
      if(caseArmVmSetupScript.exists) { Some(st"./${root.relativize(caseArmVmSetupScript)}") }
      else { None() }

    val cakeML: Option[ST] =
      if(cakeMlScript.exists) { Some(st"./${root.relativize(cakeMlScript)}") }
      else { None() }

    val transpile: Option[ST] =
      if(transpileSel4.exists && osireumScript.isEmpty) { Some(st"./${root.relativize(transpileSel4)}") }
      else { None() }

    var camkesOptions: ISZ[String] = ISZ()
    if(symbolTable.hasCakeMLComponents()) {
      camkesOptions = camkesOptions :+ OPT_CAKEML_ASSEMBLIES_PRESENT
    }
    if(symbolTable.hasVM()) {
      camkesOptions = camkesOptions :+ OPT_USE_PRECONFIGURED_ROOTFS
    }
    val _camkesOptions: Option[ST] =
      if(camkesOptions.nonEmpty) Some(st"""-o "${(camkesOptions, ";")}"""")
      else None()

    assert(runScript.exists, s"${runScript} not found")
    val runCamkes: ST = st"./${root.relativize(runScript)} ${_camkesOptions} -s"

    val osireum: Option[ST] =
      if(osireumScript.nonEmpty) Some(st"./${root.relativize(osireumScript.get)}")
      else None()

    val ret: ST = st"""${osireum}
                      |${caseArmVmSetup}
                      |${cakeML}
                      |${transpile}
                      |${runCamkes}"""
    return ret
  }

  def getCamkesArchDiagram(format: DotFormat.Type): Os.Path = {
    val dot = ReadmeGenerator.getCamkesSimulatePath(o, symbolTable).up / "graph.dot"
    val outputPath = Os.path(o.aadlRootDir.get) / "diagrams" / s"CAmkES-arch-${o.platform.string}.${format.string}"
    ReadmeGenerator.renderDot(dot, outputPath, format, reporter)
    assert(outputPath.exists, s"${outputPath} does not exist")
    return outputPath
  }

  def getHamrCamkesArchDiagram(format: DotFormat.Type): Os.Path = {
    val dot = Os.path(o.camkesOutputDir.get) / "graph.dot"
    val outputPath = Os.path(o.aadlRootDir.get) / "diagrams" / s"CAmkES-HAMR-arch-${o.platform.string}.${format.string}"
    ReadmeGenerator.renderDot(dot, outputPath, format, reporter)
    assert(outputPath.exists, s"${outputPath} does not exist")
    return outputPath
  }

  def getAadlArchDiagram(): Os.Path = {
    val x = Os.path(o.aadlRootDir.get) / "diagrams" / "aadl-arch.png"
    assert(x.exists, s"${x} does not exist")
    return x
  }
}

@enum object RunType {
  'no
  'build
  'build_simulate
}

@enum object DotFormat {
  'svg
  'png
  'gif
  'pdf
}

@sig trait Level {
  def title: String
}

@datatype  class SubLevel(val title: String,
                          content: Option[ST],
                          subs: ISZ[Level]) extends Level

@datatype class ContentLevel(val title: String,
                             content: ST) extends Level

object ReadmeTemplate {

  def generateDiagramsSection(reports: HashSMap[HamrPlatform.Type, Report]): Level = {

    def toLevel(p: Os.Path, title: String) : Option[Level] = {
      if(p.exists) {
        val l: ST = createLink(title, s"${p.up.name}/${p.name}")
        Some(ContentLevel(title, st"!${l}"))
      } else { None() }
    }

    var aadlarch: Option[Os.Path] = None()

    var subLevels: ISZ[Level] = reports.entries.map(e => {
      val platform = e._1
      val report = e._2

      aadlarch = Some(report.aadlArchDiagram)

      val s: ISZ[Option[Level]] = ISZ(
        toLevel(report.camkesArchDiagram, s"${platform.string} CAmkES Arch"),
        toLevel(report.hamrCamkesArchDiagram, s"${platform.string} CAmkES HAMR Arch"))

      SubLevel(
        title = platform.string,
        content = None(),
        subs = s.map(t => t.get)
      )
    })

    subLevels = toLevel(aadlarch.get, "AADL Arch").get +: subLevels

    return SubLevel(
      title = "Diagrams",
      content = None(),
      subs = subLevels)
  }

  def generateExpectedOutputSection(reports: HashSMap[HamrPlatform.Type, Report]): Level = {
    val subLevels: ISZ[Option[Level]] = reports.entries.map(e => {
      val platform = e._1
      val report = e._2
      report.expectedOutput match {
        case Some(x) =>
          val packageNameOption: Option[ST] =
            if(report.options.packageName.nonEmpty) { Some(st"| package-name | ${report.options.packageName.get} |")}
            else { None() }

          var config = st"""|HAMR Codegen Configuration| |
                           ||--|--|"""
          if(report.runHamrScript.nonEmpty) {
            config = st"""${config}
                         || refer to [${report.runHamrScript.get.value}](${report.runHamrScript.get.value}) |
                         |"""
          }
          else {
            config = st"""${config}
                         ||${packageNameOption}
                         || exclude-component-impl | ${report.options.excludeComponentImpl} |
                         || bit-width | ${report.options.bitWidth} |
                         || max-string-size | ${report.options.maxStringSize} |
                         || max-array-size | ${report.options.maxArraySize} |
                         |"""
          }

          Some(ContentLevel(s"${platform.string} Expected Output: Timeout = ${report.timeout / 1000} seconds",
            st"""
                |  ${config}
                |
                |  **How To Run**
                |  ```
                |  ${report.runInstructions}
                |  ```
                |
                |  ```
                |  ${x}
                |  ```"""))
        case _ => None()
      }
    })

    return SubLevel(
      title = "Example Output",
      content = Some(st"*NOTE:* actual output may differ due to issues related to thread interleaving"),
      subs = subLevels.map(s => s.get))
  }

  def expand(count: Z, c: C) : String = {
    var ret: ST = st""
    for(i <- 0 until count) { ret = st"${ret}${c}" }
    return ret.render
  }

  def createLink(title: String, target: String): ST = {
    return st"[${title}](${target})"
  }

  def toGithubLink(s: String): String = {
    val lc = StringUtil.toLowerCase(s)
    val d = StringUtil.replaceAll(lc, " ", "-")
    val cis = conversions.String.toCis(d)

    // only keep numbers, lowercase letters, '-' and '_'
    val cis_ = cis.filter(c =>
      (c.value >= 48 && c.value <= 57) || (c.value >= 97 && c.value <= 122) ||
      (c == '-') || (c == '_'))
    val d_ = conversions.String.fromCis(cis_)
    return s"#${d_}"
  }

  def generateTOC(levels: ISZ[Level]): Level = {

    def gen(level: Level, levelNum: Z): ST = {
      val spaces = expand(levelNum, ' ')

      var ret = st"${spaces}* ${createLink(level.title, toGithubLink(level.title))}"

      level match {
        case s: SubLevel =>
          val subs = s.subs.map(sub => gen(sub, levelNum + 2))
          ret = st"""${ret}
                    |${(subs,"\n")}"""
        case s: ContentLevel =>
      }

      return ret
    }

    val subs = levels.map(level => gen(level, 2))
    val content: ST = st"${(subs, "\n")}"

    return ContentLevel(
      title = "Table of Contents",
      content = content)
  }

  def level2ST(level: Level, levelNum: Z): ST = {
    //val spaces = expand(levelNum, ' ')
    val hashes = expand(levelNum, '#')
    val title = s"${hashes} ${level.title}"
    val content: ST = level match {
      case s: ContentLevel => s.content
      case s: SubLevel => {
        val x = s.subs.map(sub => level2ST(sub, levelNum + 1))
        st"""${s.content}
            |${(x, "\n\n")}"""
      }
    }
    val ret: ST = st"""${title}
                      |${content}"""
    return ret
  }

  def generateReport(title: String, reports: HashSMap[Cli.HamrPlatform.Type, Report]): ST = {
    val platformsDiagrams: Level = generateDiagramsSection(reports)

    val platformExpectedOutput: Level = generateExpectedOutputSection(reports)

    val toc: Level = generateTOC(ISZ(platformsDiagrams, platformExpectedOutput))

    val ret: ST = st"""# ${title}
                      |
                      |${level2ST(toc, 0)}
                      |
                      |${level2ST(platformsDiagrams, 2)}
                      |
                      |${level2ST(platformExpectedOutput, 2)}"""
    return ret
  }
}

object ReadmeGenerator {
  def run(args: ISZ[String], reporter: Reporter): B = {
    val results = Os.proc(args).console.run()
    if(!results.ok) {
      reporter.error(None(), "", results.err)
    }
    return results.ok
  }

  def renderDot(dot: Os.Path, outputPath: Os.Path, format: DotFormat.Type, reporter: Reporter): B = {
    val args: ISZ[String] = ISZ("dot", s"-T${format.string}", dot.canon.value, "-o", outputPath.value)
    val results = Os.proc(args).console.run()
    if(!results.ok) {
      reporter.error(None(), "", results.err)
    }
    return results.ok
  }


  def parseOutput(out: String): Option[String] = {
    val o = ops.StringOps(out)
    val pos = o.stringIndexOf("Booting all finished")
    if(pos > 0) {
      return Some(o.substring(pos, o.size))
    } else {
      return Some("Didn't find 'Booting all finished'!")
    }
  }

  def getCamkesSimulatePath(o: Cli.HamrCodeGenOption, symbolTable: SymbolTable): Os.Path = {
    val camkesPath: Os.Path = ReadmeGenerator.getCamkesDir(symbolTable)
    assert(camkesPath.exists, s"${camkesPath} doesn't exist")

    val camkesOutputDir = Os.path(o.camkesOutputDir.get)

    return camkesPath / s"build_${camkesOutputDir.name}" / "simulate"
  }

  def getCamkesDir(symbolTable: SymbolTable): Os.Path = {
    val ret: String =
      if(symbolTable.hasVM()) "camkes-arm-vm"
      else "camkes"

    return Os.home / "CASE" / ret
  }

  def getTranspileSel4Script(slangOutputDir: Os.Path): Os.Path = {
    return slangOutputDir / "bin" / "transpile-sel4.sh"
  }

  def getRunCamkesScript(camkesOutputDir: Os.Path): Os.Path = {
    return camkesOutputDir / "bin" / "run-camkes.sh"
  }


  def getModel(inputFile: Os.Path, isMsgpack: B): ir.Aadl = {
    val input: String = if (inputFile.exists && inputFile.isFile) {
      inputFile.read
    } else {
      halt(s"AIR input file ${inputFile} not found")
    }

    val model: ir.Aadl = if (isMsgpack) {
      org.sireum.conversions.String.fromBase64(input) match {
        case Either.Left(u) =>
          irMsgPack.toAadl(u) match {
            case Either.Left(m) => m
            case Either.Right(m) =>
              halt(s"MsgPack deserialization error at offset ${m.offset}: ${m.message}")
          }
        case Either.Right(m) =>
          halt(m)
      }
    }
    else {
      irJSON.toAadl(input) match {
        case Either.Left(m) => m
        case Either.Right(m) =>
          halt(s"Json deserialization error at (${m.line}, ${m.column}): ${m.message}")
      }
    }
    return model
  }

  def getSymbolTable(model: ir.Aadl): SymbolTable = {
    val reporter = Reporter.create

    val rawConnections: B = PropertyUtil.getUseRawConnection(model.components(0).properties)
    val aadlTypes = TypeResolver.processDataTypes(model, rawConnections, "")

    val s = SymbolResolver.resolve(model, None(), T, aadlTypes, reporter)
    reporter.printMessages()
    return s
  }
}

@record class Report (options: Cli.HamrCodeGenOption,
                      runHamrScript: Option[Os.Path],
                      timeout: Z,

                      runInstructions: ST,
                      expectedOutput: Option[ST],

                      aadlArchDiagram: Os.Path,
                      hamrCamkesArchDiagram: Os.Path,
                      camkesArchDiagram: Os.Path)