// #Sireum

package org.sireum.cli.hamr_runners

import org.sireum.Cli.HamrPlatform
import org.sireum.Os.Proc
import org.sireum._
import org.sireum.hamr.codegen.common.StringUtil
import org.sireum.hamr.ir
import org.sireum.message.Reporter
import org.sireum.hamr.codegen.common.symbols.{SymbolResolver, SymbolTable}
import org.sireum.hamr.ir.{JSON => irJSON, MsgPack => irMsgPack}

@record class ReadmeGenerator(o: Cli.HamrCodeGenOption) {

  val airFile: Os.Path = {
    val path = Os.path(o.args(0))
    assert(path.exists, s"${path} does not exist")
    path
  }

  val model: ir.Aadl = ReadmeGenerator.getModel(airFile, o.json)

  val symbolTable: SymbolTable = ReadmeGenerator.getSymbolTable(model)

  val camkesOutputDir: Os.Path = Os.path(o.camkesOutputDir.get)
  val slangOutputDir: Os.Path = Os.path(o.outputDir.get)

  def build(): Z = {

    val run_camkes: Os.Path = ReadmeGenerator.getRunCamkesScript(camkesOutputDir)

    assert(run_camkes.exists, "Couldn't find run_camkes.sh")

    var exitCode: Z = 0
    val transpileSel4: Os.Path = ReadmeGenerator.getTranspileSel4Script(slangOutputDir)
    if(transpileSel4.exists) {
      val results = Os.proc(ISZ(transpileSel4.value)).console.run()
      exitCode = results.exitCode
    }

    if(exitCode == 0) {
      var args: ISZ[String] = ISZ(run_camkes.value, "-n")

      if (symbolTable.hasCakeMLComponents()) {
        args = args :+ "-o" :+ "-DCAKEML_ASSEMBLIES_PRESENT=ON"
      }

      val results = Os.proc(args).console.run()
      exitCode = results.exitCode
    }

    return exitCode
  }

  def simulate(timeout: Z): ST = {
    val simulateScript: Os.Path = ReadmeGenerator.getCamkesSimulatePath(o, symbolTable)
    assert(simulateScript.exists, s"${simulateScript} does not exist")

    val p = Proc(ISZ(simulateScript.value), Os.cwd, Map.empty, T, None(), F, F, F, F, F, timeout, F)
    val results = p.at(simulateScript.up).run()
    cprint(F, results.out)
    cprint(T, results.err)

    Os.proc(ISZ("pkill", "qemu")).console.runCheck()

    val output = ReadmeGenerator.parseOutput(results.out)
    assert(output.nonEmpty, "Expected output missing")

    return st"${output.get}"
  }

  def genConfigurationEntry(): ST = {
    val ret: ST = st"""
                      |"""
    return ret
  }

  def genRunInstructions(): ST = {
    val ret: ST = st"""
                      |"""
    return ret
  }

  def getCamkesArchDiagram(format: DotFormat.Type): Os.Path = {
    val dot = ReadmeGenerator.getCamkesSimulatePath(o, symbolTable).up / "graph.dot"
    val outputPath = Os.path(o.aadlRootDir.get) / "diagrams" / s"CAmkES-arch-${o.platform.string}.${format.string}"
    ReadmeGenerator.renderDot(dot, outputPath, format)
    assert(outputPath.exists, s"${outputPath} does not exist")
    return outputPath
  }

  def getHamrCamkesArchDiagram(format: DotFormat.Type): Os.Path = {
    val dot = Os.path(o.camkesOutputDir.get) / "graph.dot"
    val outputPath = Os.path(o.aadlRootDir.get) / "diagrams" / s"CAmkES-HAMR-arch-${o.platform.string}.${format.string}"
    ReadmeGenerator.renderDot(dot, outputPath, format)
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
                          subs: ISZ[Level]) extends Level

@datatype class ContentLevel(val title: String,
                             content: ST) extends Level

object ReadmeTemplate {

  def generateDiagramsSection(reports: HashSMap[HamrPlatform.Type, Report]): Level = {
    var subLevels: ISZ[Level] = ISZ()

    return SubLevel(
      title = "Diagrams",
      subs = subLevels)
  }

  def generateExpectedOutputSection(reports: HashSMap[HamrPlatform.Type, Report]): Level = {
    var subLevels: ISZ[Level] = ISZ()

    return SubLevel(
      title = "Expected Output",
      subs = subLevels)
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
    return s"#${d}"
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
    val spaces = expand(levelNum, ' ')
    val hashes = expand(levelNum, '#')
    val title = s"${spaces}${hashes} ${level.title}"
    val content: ST = level match {
      case s: ContentLevel => s.content
      case s: SubLevel => {
        val x = s.subs.map(sub => level2ST(sub, levelNum + 1))
        st"${(x, "\n\n")}"
      }
    }
    val ret: ST = st"""${title}
                      |${spaces}${content}"""
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
  def renderDot(dot: Os.Path, outputPath: Os.Path, format: DotFormat.Type): Z = {
    val args: ISZ[String] = ISZ("dot", s"-T${format.string}", dot.canon.value, "-o", outputPath.value)
    val result = Os.proc(args).console.runCheck()
    return result.exitCode
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


  def getModel(inputFile: Os.Path, isJson: B): ir.Aadl = {
    val input: String = if (inputFile.exists && inputFile.isFile) {
      inputFile.read
    } else {
      halt(s"AIR input file ${inputFile} not found")
    }

    val model: ir.Aadl = if (isJson) {
      irJSON.toAadl(input) match {
        case Either.Left(m) => m
        case Either.Right(m) =>
          halt(s"Json deserialization error at (${m.line}, ${m.column}): ${m.message}")
      }
    }
    else {
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
    return model
  }

  def getSymbolTable(model: ir.Aadl): SymbolTable = {
    val reporter = Reporter.create
    val s = SymbolResolver.resolve(model, None(), reporter)
    reporter.printMessages()
    return s
  }
}

@record class Report (options: Cli.HamrCodeGenOption,

                      configuration: ST,
                      runInstructions: ST,
                      expectedOutput: Option[ST],

                      aadlArchDiagram: Os.Path,
                      hamrCamkesArchDiagram: Os.Path,
                      camkesArchDiagram: Os.Path)