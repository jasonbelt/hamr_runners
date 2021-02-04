// #Sireum
package org.sireum.cli.hamr_runners.iccps20

import org.sireum._
import org.sireum.Cli.HamrPlatform
import org.sireum.Os.Proc
import org.sireum.hamr.act.util.PathUtil
import org.sireum.hamr.act.vm.VM_Template
import org.sireum.hamr.codegen.common.{CommonUtil, StringUtil}
import org.sireum.hamr.codegen.common.properties.PropertyUtil
import org.sireum.hamr.ir
import org.sireum.message.Reporter
import org.sireum.hamr.codegen.common.symbols.{SymbolResolver, SymbolTable}
import org.sireum.hamr.codegen.common.transformers.Transformers
import org.sireum.hamr.codegen.common.types.TypeResolver
import org.sireum.hamr.ir.{JSON => irJSON, MsgPack => irMsgPack}

@record class IccpsReadmeGenerator(o: Cli.HamrCodeGenOption,
                                   project: Project,
                                   reporter: Reporter) {

  val airFile: Os.Path = {
    val path = Os.path(o.args(0))
    assert(path.exists, s"${path} does not exist")
    path
  }

  val model: ir.Aadl = IccpsReadmeGenerator.getModel(airFile, o.json)

  val symbolTable: SymbolTable = IccpsReadmeGenerator.getSymbolTable(model, o.packageName.get)

  val camkesOutputDir: Option[Os.Path] = if(o.camkesOutputDir.nonEmpty) Some(Os.path(o.camkesOutputDir.get)) else None()
  val slangOutputDir: Os.Path = Os.path(o.outputDir.get)

  val OPT_CAKEML_ASSEMBLIES_PRESENT: String = "-DCAKEML_ASSEMBLIES_PRESENT=ON"

  def build(shouldRebuild: B): B = {
    if(!shouldRebuild) {
      return T
    }  else {
      if (isCamkes(o.platform)) {
        return buildCamkes()
      } else if (isJvm(o.platform)) {
        buildJVM()
      } else if (isNix(o.platform)) {
        buildLinux()
      } else {
        halt(s"${o.platform}")
      }
    }
  }

  def buildLinux(): B = {
    var continue: B = T
    val transpileScript: Os.Path = IccpsReadmeGenerator.getTranspileScript(slangOutputDir)
    if(transpileScript.exists) {
      continue = IccpsReadmeGenerator.run(ISZ(transpileScript.value), slangOutputDir, reporter)
    }
    if(continue) {
      val nixBuildScript = slangOutputDir / "bin" / "compile-linux.sh"
      continue = IccpsReadmeGenerator.run(ISZ(nixBuildScript.value), slangOutputDir, reporter)
    }
    return continue
  }

  def buildJVM(): B = {
    val buildsbt: Os.Path = slangOutputDir / "build.sbt"
    assert(buildsbt.exists && buildsbt.isFile, s"${buildsbt}")

    return IccpsReadmeGenerator.run(ISZ("sbt", "compile"), slangOutputDir, reporter)
  }

  def buildCamkes(): B = {

    val run_camkes: Os.Path = IccpsReadmeGenerator.getRunCamkesScript(camkesOutputDir.get)

    assert(run_camkes.exists, s"${run_camkes} not found")

    var continue: B = T
    val transpileSel4: Os.Path = IccpsReadmeGenerator.getTranspileSel4Script(slangOutputDir)
    if(transpileSel4.exists) {
      continue = IccpsReadmeGenerator.run(ISZ(transpileSel4.value), slangOutputDir, reporter)
    }

    if(continue) {
      var args: ISZ[String] = ISZ(run_camkes.value, "-n")

      if (symbolTable.hasCakeMLComponents()) {
        args = args :+ "-o" :+ OPT_CAKEML_ASSEMBLIES_PRESENT
      }

      continue = IccpsReadmeGenerator.run(args, camkesOutputDir.get, reporter)
    }

    return continue
  }

  def simulate(timeout: Z): ST = {
    if(isCamkes(o.platform)) {
      return simulateCamkes(timeout)
    } else if(isJvm(o.platform)) {
      return simulateJVM(timeout)
    } else {
      halt(s"${o.platform}")
    }
  }

  def simulateJVM(timeout: Z): ST = {
    val buildsbt = slangOutputDir / "build.sbt"
    assert(buildsbt.exists && buildsbt.isFile)

    println(s"Simulating for ${timeout/1000} seconds")

    val p = Os.proc(ISZ("sbt", "run")).at(slangOutputDir).timeout(timeout).input("hi")

    val results = p.at(buildsbt.up).run()
    cprint(F, results.out)
    cprint(T, results.err)

    val output = IccpsReadmeGenerator.parseOutput(results.out)
    assert(output.nonEmpty, "Expected output missing")

    return st"${output.get}"
  }

  def simulateCamkes(timeout: Z): ST = {
    val simulateScript: Os.Path = IccpsReadmeGenerator.getCamkesSimulatePath(o, symbolTable)
    assert(simulateScript.exists, s"${simulateScript} does not exist")

    println(s"Simulating for ${timeout/1000} seconds")

    val p = Os.proc(ISZ(simulateScript.value)).timeout(timeout)

    val results = p.at(simulateScript.up).run()
    cprint(F, results.out)
    cprint(T, results.err)

    IccpsReadmeGenerator.run(ISZ("pkill", "qemu"), simulateScript, reporter)

    val output = IccpsReadmeGenerator.parseOutput(results.out)
    assert(output.nonEmpty, "Expected output missing")

    return st"${output.get}"
  }

  def isJvm(platform: HamrPlatform.Type): B = { return platform == Cli.HamrPlatform.JVM }

  def isNix(platform: HamrPlatform.Type): B = {
    return platform == Cli.HamrPlatform.Linux || platform == Cli.HamrPlatform.MacOS || platform == Cli.HamrPlatform.Cygwin
  }

  def isCamkes(platform: HamrPlatform.Type): B = {
    return platform == Cli.HamrPlatform.SeL4_TB || platform == Cli.HamrPlatform.SeL4 || platform == Cli.HamrPlatform.SeL4_Only
  }

  def genRunInstructions(root: Os.Path): ST = {
    if(isJvm(o.platform)) {
      return genRunInstructionsJVM(root)
    }
    else if(isNix(o.platform)) {
      return genRunInstructionsNix(root)
    }
    else if(isCamkes(o.platform)) {
      return genRunInstructionsCamkes(root)
    } else {
      halt(s"${o.platform}")
    }
  }

  def genRunInstructionsJVM(root: Os.Path): ST = {
    val buildsbt = slangOutputDir / "build.sbt"
    assert(buildsbt.exists, s"${buildsbt}")
    val rpath = root.relativize(buildsbt.up)
    return st"""cd $rpath
               |sbt run"""
  }

  def genRunInstructionsNix(root: Os.Path): ST = {
    val bindir = slangOutputDir / "bin"
    assert(bindir.exists && bindir.isDir)

    val transpileScript = bindir / "transpile.sh"
    val compileScript = bindir / "compile-linux.sh"
    val runScript = bindir / "run-linux.sh"
    val stopScript = bindir / "stop.sh"

    assert(transpileScript.exists)
    assert(compileScript.exists)
    assert(runScript.exists)
    assert(stopScript.exists)

    val ret: ST =
      st"""${root.relativize(transpileScript)}
          |${root.relativize(compileScript)}
          |${root.relativize(runScript)}
          |${root.relativize(stopScript)}"""

    return ret
  }

  def genRunInstructionsCamkes(root: Os.Path): ST = {

    val transpileSel4: Os.Path = IccpsReadmeGenerator.getTranspileSel4Script(slangOutputDir)
    val runScript: Os.Path = IccpsReadmeGenerator.getRunCamkesScript(camkesOutputDir.get)
    val cakeMlScript: Os.Path = transpileSel4.up / "compile-cakeml.sh"
    val caseArmVmSetupScript: Os.Path = runScript.up / Os.path(PathUtil.CAMKES_ARM_VM_SCRIPT_PATH).name

    val caseArmVmSetup: Option[ST] =
      if(caseArmVmSetupScript.exists) { Some(st"${root.relativize(caseArmVmSetupScript)}") }
      else { None() }

    val cakeML: Option[ST] =
      if(cakeMlScript.exists) { Some(st"${root.relativize(cakeMlScript)}") }
      else { None() }

    val transpile: Option[ST] =
      if(transpileSel4.exists) { Some(st"${root.relativize(transpileSel4)}") }
      else { None() }

    var options: ISZ[String] = ISZ("-s")
    if(symbolTable.hasCakeMLComponents()) {
      options = options :+ "-o" :+ OPT_CAKEML_ASSEMBLIES_PRESENT
    }

    assert(runScript.exists, s"${runScript} not found")
    val runCamkes: ST = st"${root.relativize(runScript)} ${(options, " ")}"

    val ret: ST = st"""${caseArmVmSetup}
                      |${cakeML}
                      |${transpile}
                      |${runCamkes}"""
    return ret
  }

  def getAadlMetrics(): ST = {
    val threads = symbolTable.getThreads()

    val connections = symbolTable.connections

    var ports: Z = 0
    for(t <- threads) {
      for(fe <- t.getFeatureEnds()){
        if(CommonUtil.isPort(fe)) {
          ports = ports + 1
        }
      }
    }

    val ret: ST =
      st"""| | |
          ||--|--|
          ||Threads|${threads.size}|
          ||Ports|${ports}|
          ||Connections|${connections.size}|"""

    return ret
  }

  def getCodeMetrics(): ST = {
    val ret: ST =
      if (isJvm(o.platform)) {
        getCodeMetricsJVM()
      } else if (isNix(o.platform)) {
        getCodeMetricsNix()
      } else if (isCamkes(o.platform)) {
        getCodeMetricsCamkes()
      } else {
        halt("??")
      }
    return ret
  }

  def processUserCloc(root: Os.Path): ST = {
    var code: Z = 0
    var log: Z = 0

    def process(d: Os.Path): Unit = {
      if(d.isDir) {
        for(f <- d.list) {
          process(f)
        }
      } else {
        if(d.ext == "loc") {
          val props = d.properties
          code = code + Z(props.getOrElse("code", "0")).get
          log = log + Z(props.getOrElse("log", "0")).get
        }
      }
    }
    process(root)

    val ret: ST =
      st"""|Type|code |
          ||--|--:|
          ||Behavior|${(code - log)}|
          ||Log|${log}|
          ||--------|--------|
          ||SUM:|${code}|"""

    return ret
  }

  def dirsScanned(dirs: ISZ[Os.Path]): ST = {
    val rdirs = dirs.map(m => project.projectDir.relativize(m)).map(m => st"- [${m}]($m)")

    val _dirs: ST = st"""Directories Scanned Using [https://github.com/AlDanial/cloc](https://github.com/AlDanial/cloc) v1.88:
                        |${(rdirs, "\n")}"""
    return _dirs
  }

  def getCodeMetricsJVM(): ST = {
    val mainDir = slangOutputDir / "src" / "main"
    assert(mainDir.exists && mainDir.isDir)
    val cloc = runCloc(ISZ(mainDir))

    val _dirsScanned = dirsScanned(ISZ(mainDir))

    val userCloc: ST = processUserCloc(mainDir)
    val ret: ST =
      st"""${_dirsScanned}
          |
          |<u><b>Total LOC</b></u>
          |
          |Total number of HAMR-generated and developer-written lines of code
          |${cloc}
          |
          |<u><b>User LOC</b></u>
          |
          |The number of lines of code written by the developer.
          |"Log" are lines of code used for logging that
          |likely would be excluded in a release build
          | ${userCloc}"""
    return ret
  }

  def getCodeMetricsNix(): ST = {
    val extDir = slangOutputDir / "src" / "c" / "ext-c"
    val nixDir = slangOutputDir / "src" / "c" / "nix"

    val dirs = ISZ(extDir, nixDir)
    val cloc = runCloc(dirs)

    val _dirsScanned = dirsScanned(dirs)

    val cCode: String =
      if(o.excludeComponentImpl)
        "The Slang-based component implementations were excluded by the transpiler so this represents the number of lines of C code needed to realize the component behaviors."
      else "The Slang-based component implementations were included by the transpiler so this represents the number of lines of C that implement Slang extensions."

    val userCloc: ST = processUserCloc(extDir)
    val ret: ST =
      st"""${_dirsScanned}
          |
          |<u><b>Total LOC</b></u>
          |
          |Total number of HAMR-generated (transpiled) and developer-written lines of code
          |${cloc}
          |
          |<u><b>User LOC</b></u>
          |
          |The number of lines of code written by the developer.
          |${cCode}
          |"Log" are lines of code used for logging that
          |likely would be excluded in a release build
          |${userCloc}"""
    return ret
  }

  def getCodeMetricsCamkes(): ST = {
    return st"Not counting CAmkES/seL4 code.  Notable is that the developer did not have to write any additional LOC for this profile."
  }

  def runCloc(dirs: ISZ[Os.Path]): ST = {
    //dirs.foreach(d => assert(d.exists && d.isDir, s"$d"))

    val args: ISZ[String] = ISZ[String](
      "cloc",
      "--md",
      "--exclude-lang=make,CMake"
    ) ++ dirs.map(m => m.value)

    val results = Os.proc(args).run()
    val s = results.out.native

    val ret = s.split("\n".native).drop(3.toInt).mkString("\n".native)

    return st"${ret}"
  }

  def getCamkesArchDiagram(format: DotFormat.Type): Os.Path = {
    val dot = IccpsReadmeGenerator.getCamkesSimulatePath(o, symbolTable).up / "graph.dot"
    val outputPath = Os.path(o.aadlRootDir.get) / "diagrams" / s"CAmkES-arch-${o.platform.string}.${format.string}"
    IccpsReadmeGenerator.renderDot(dot, outputPath, format, reporter)
    assert(outputPath.exists, s"${outputPath} does not exist")
    return outputPath
  }

  def getHamrCamkesArchDiagram(format: DotFormat.Type): Os.Path = {
    val dot = Os.path(o.camkesOutputDir.get) / "graph.dot"
    val outputPath = Os.path(o.aadlRootDir.get) / "diagrams" / s"CAmkES-HAMR-arch-${o.platform.string}.${format.string}"
    IccpsReadmeGenerator.renderDot(dot, outputPath, format, reporter)
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

object IccpsReadmeTemplate {

  def generateDiagramsSection(projectRoot: Os.Path, reports: HashSMap[HamrPlatform.Type, Report]): Level = {

    def toLevel(p: Os.Path, title: String) : Option[Level] = {
      if(p.exists) {
        val relPath = projectRoot.relativize(p)
        val l: ST = createLink(title, relPath.value) //s"${p.up.name}/${p.name}")
        Some(ContentLevel(title, st"!${l}"))
      } else { None() }
    }

    var aadlarch: Option[Os.Path] = None()

    var subLevels: ISZ[Level] = ISZ()

    for(e <- reports.entries) {
      val platform = e._1
      val report = e._2

      assert (aadlarch.isEmpty || aadlarch == Some(report.aadlArchDiagram))

      aadlarch = Some(report.aadlArchDiagram)

      report match {
        case r: JvmReport => // do nothing
        case r: NixReport => // do nothing
        case r: CamkesReport =>
          val s: ISZ[Option[Level]] = ISZ(
            toLevel(r.camkesArchDiagram, s"${platform.string} CAmkES Arch"),
            toLevel(r.hamrCamkesArchDiagram, s"${platform.string} CAmkES HAMR Arch"))

          subLevels = subLevels :+ SubLevel(
            title = platform.string,
            content = None(),
            subs = s.map(t => t.get)
          )
      }
    }

    subLevels = toLevel(aadlarch.get, "AADL Arch").get +: subLevels

    return SubLevel(
      title = "Diagrams",
      content = None(),
      subs = subLevels)
  }

  def generateMetricsSection(reports: HashSMap[HamrPlatform.Type, Report]): Level = {

    val aadlMetrics = reports.values(0).aadlMetrics

    var subLevels: ISZ[Level] = ISZ(
      ContentLevel(
        "AADL Metrics",
        aadlMetrics))

    for(r <- reports.entries) {
      val platform = r._1
      val report: Report = r._2

      subLevels = subLevels :+ ContentLevel(
        s"${platform} Metrics",
        report.codeMetrics
      )
    }

    return SubLevel(
      title = "Metrics",
      content = None(),
      subs = subLevels
    )
  }

  def generateExpectedOutputSection(reports: HashSMap[HamrPlatform.Type, Report]): Level = {
    val subLevels: ISZ[Level] = reports.entries.map(e => {
      val platform = e._1
      val report = e._2

      val packageNameOption: Option[ST] =
        if(report.options.packageName.nonEmpty) { Some(st"| package-name | ${report.options.packageName.get} |")}
        else { None() }

      val config = st"""|HAMR Codegen Configuration| |
                        ||--|--|
                        |${packageNameOption}
                        || exclude-component-impl | ${report.options.excludeComponentImpl} |
                        || bit-width | ${report.options.bitWidth} |
                        || max-string-size | ${report.options.maxStringSize} |
                        || max-array-size | ${report.options.maxArraySize} |
                        |"""

      val eoutput: Option[ST] = report.expectedOutput match {
        case Some(x) => Some(
          st"""**Expected Output: Timeout = ${report.timeout / 1000} seconds**
              |```
              |  ${x}
              |```""")
        case _ => None[ST]()
          }

      ContentLevel(s"${platform.string}",
        st"""
            |  ${config}
            |
            |  **How To Run**
            |  ```
            |  ${report.runInstructions}
            |  ```
            |  ${eoutput}""")
    })

    return SubLevel(
      title = "Run Instructions",
      content = Some(st"*NOTE:* actual output may differ due to issues related to thread interleaving"),
      subs = subLevels
    )
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

  def generateReport(project: Project,
                     reports: HashSMap[Cli.HamrPlatform.Type, Report]): ST = {
    val platformsDiagrams: Level = generateDiagramsSection(project.projectDir, reports)

    val platformMetrics: Level = generateMetricsSection(reports)

    val platformExpectedOutput: Level = generateExpectedOutputSection(reports)

    val toc: Level = generateTOC(ISZ(platformsDiagrams, platformMetrics, platformExpectedOutput))

    val ret: ST = st"""# ${project.title}
                      |
                      |${level2ST(toc, 0)}
                      |
                      |${level2ST(platformsDiagrams, 2)}
                      |
                      |${level2ST(platformMetrics, 2)}
                      |
                      |${level2ST(platformExpectedOutput, 2)}"""
    return ret
  }
}

object IccpsReadmeGenerator {
  def run(args: ISZ[String], at: Os.Path, reporter: Reporter): B = {
    val results = Os.proc(args).at(at).console.run()
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
    var pos = o.stringIndexOf("Booting all finished")
    if(pos > 0) {
      return Some(o.substring(pos, o.size))
    } else {
      pos = o.stringIndexOf("[info] running ")
      if(pos > 0) {
        return Some(o.substring(pos, o.size))
      } else {
        return Some("Didn't find 'Booting all finished'!")
      }
    }
  }

  def getCamkesSimulatePath(o: Cli.HamrCodeGenOption, symbolTable: SymbolTable): Os.Path = {
    val camkesPath: Os.Path = IccpsReadmeGenerator.getCamkesDir(symbolTable)
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

  def getTranspileScript(slangOutputDir: Os.Path): Os.Path = {
    return slangOutputDir / "bin" / "transpile.sh"
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

  def getSymbolTable(model: ir.Aadl, basePackageName: String): SymbolTable = {
    val reporter = Reporter.create

    var _model = model

    val result = ir.Transformer(Transformers.MissingTypeRewriter(reporter)).transformAadl(Transformers.CTX(F, F), _model)
    _model = if (result.resultOpt.nonEmpty) result.resultOpt.get else model

    val rawConnections: B = PropertyUtil.getUseRawConnection(_model.components(0).properties)
    val aadlTypes = TypeResolver.processDataTypes(_model, rawConnections, basePackageName)

    val s = SymbolResolver.resolve(_model, None(), T, aadlTypes, reporter)
    if(reporter.hasError) {
      println("**********************************************")
      println("***  Messages from ICCPS Readme Gen")
      println("**********************************************")
      reporter.printMessages()
      println("**********************************************")
      println("***  END OF Messages from ICCPS Readme Gen")
      println("**********************************************")

    }
    return s
  }
}

@msig trait Report{
  def options: Cli.HamrCodeGenOption
  def timeout: Z

  def runInstructions: ST
  def expectedOutput: Option[ST]

  def aadlArchDiagram: Os.Path

  def aadlMetrics: ST
  def codeMetrics: ST
}

@record class NixReport (options: Cli.HamrCodeGenOption,
                         timeout: Z,

                         runInstructions: ST,
                         expectedOutput: Option[ST],

                         aadlArchDiagram: Os.Path,

                         aadlMetrics: ST,
                         codeMetrics: ST) extends Report

@record class JvmReport (options: Cli.HamrCodeGenOption,
                          timeout: Z,

                          runInstructions: ST,
                          expectedOutput: Option[ST],

                          aadlArchDiagram: Os.Path,

                          aadlMetrics: ST,
                          codeMetrics: ST) extends Report

@record class CamkesReport (options: Cli.HamrCodeGenOption,
                            timeout: Z,

                            runInstructions: ST,
                            expectedOutput: Option[ST],

                            aadlArchDiagram: Os.Path,

                            hamrCamkesArchDiagram: Os.Path,
                            camkesArchDiagram: Os.Path,

                            aadlMetrics: ST,
                            codeMetrics: ST) extends Report