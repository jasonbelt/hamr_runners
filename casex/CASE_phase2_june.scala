// #Sireum
package org.sireum.cli.hamr_runners.casex

import org.sireum._
import org.sireum.cli.hamr_runners.DotFormat

object CASE_phase2_june extends App {

  @datatype class Project (simpleName: String,
                           projectDir: Os.Path,
                           aadlDir: Os.Path,
                           slangFile: Os.Path,
                           platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type],
                           shouldSimulate: B,
                           timeout: Z)

  val shouldReport: B = T
  val graphFormat: DotFormat.Type = DotFormat.svg
  val build: B = F
  val defTimeout: Z = 15000
  val vmTimeout: Z = 90000

  val linux: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.Linux
  val sel4: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4
  val sel4_tb: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4_TB
  val sel4_only: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4_Only

  val rootDir: Os.Path = Os.home / "devel" / "case" / "case-ku" / "examples" / "ksu-proprietary" / "Phase-2-UAV-Experimental-Platform-June-step6-hamr"

  val runTranspiler: B = F

  def genFull(rDir: Os.Path, name: String, json: String, platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type],
              shouldSimulate: B, timeout: Z): Project = {
    val projectDir = rDir / name
    val aadlDir = projectDir / "aadl"
    val jsonFile = aadlDir / ".slang" / json

    assert(aadlDir.exists, aadlDir.value)
    assert(jsonFile.exists, jsonFile.value)

    return Project(name, projectDir, aadlDir, jsonFile, platforms, shouldSimulate, timeout)
  }

  def gen(rDir: Os.Path, name: String, json: String, platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type]): Project = {
    return genFull(rDir, name, json, platforms, T, defTimeout)
  }

  val projectsDirs: ISZ[Project] = ISZ(

    //gen("Phase-2-UAV-Experimental-Platform-June-hamr", "SW_SW_Impl_Instance.json"),

    //gen("Phase-2-UAV-Experimental-Platform-June-hamr_dataports", "SW_SW_Impl_Instance.json"),

    //gen("Phase-2-UAV-Experimental-Platform-Transformed", "UAV_UAV_Impl_Instance.json")

   // gen(rootDir,"cakeml", "UAV_UAV_Impl_Instance.json", ISZ(linux)),

    gen(rootDir, "cakeml", "UAV_UAV_Impl_Instance.json", ISZ(linux, sel4)),

    gen(rootDir, "vm", "UAV_UAV_Impl_Instance.json", ISZ(linux, sel4))
  )

  override def main(args: ISZ[String]): Z = {

    for (project <- projectsDirs) {
      val projectDir = project.projectDir
      val slangFile = project.slangFile

      var readmeEntries: ISZ[ST] = ISZ()

      val outputDir = projectDir / "hamr"

      var slangAuxCodeDirs:ISZ[String] = ISZ()

      val clib = project.aadlDir / "c_libraries"

      if((clib / "CMASI").exists){
        slangAuxCodeDirs = slangAuxCodeDirs :+ (clib / "CMASI" ).canon.value
      }

      if((clib / "hexdump").exists){
        slangAuxCodeDirs = slangAuxCodeDirs :+ (clib / "hexdump" ).canon.value
      }

      if((clib / "dummy_serial_server" ).exists) {
        slangAuxCodeDirs = slangAuxCodeDirs :+ (clib / "dummy_serial_server" ).canon.value
      }

      for (platform <- project.platforms) {
        val camkesOutputDir: Option[Os.Path] = platform match {
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_TB => Some(outputDir / "src" / "c" / "CAmkES_seL4_TB_VM")
          case Cli.SireumHamrCodegenHamrPlatform.SeL4_Only => Some(outputDir / "src" / "c" / "CAmkES_seL4_Only_VM")
          case Cli.SireumHamrCodegenHamrPlatform.SeL4 => Some(outputDir / "src" / "c" / "CAmkES_seL4_VM")
          case _ => None()
        }

        //outputDir.removeAll()

        val o = Util.o(
          args = ISZ(slangFile.value),
          platform = platform,
          packageName = Some("hamr"),
          excludeComponentImpl = T,

          outputDir = Some(outputDir.value),

          bitWidth = 32,
          maxStringSize = 256,
          maxArraySize = 1,
          runTranspiler = runTranspiler,

          slangAuxCodeDirs = slangAuxCodeDirs,

          aadlRootDir = Some(project.aadlDir.value)
        )

        cli.HAMR.codeGen(o)


        val _dot: Option[Os.Path] = camkesOutputDir.map(m => m / "graph.dot")

        if(_dot.nonEmpty) {
          val dot = _dot.get

          val tool_eval_4_diagrams = projectDir / "diagrams"

          //val png = s"CAmkES-arch-${platform}.png"
          val svg = s"CAmkES-arch-${platform}.svg"

          //val dotPDFOutput = tool_eval_4_diagrams / s"CAmkES-arch-${platform}.pdf"
          //val dotPNGOutput = tool_eval_4_diagrams / png
          val dotPNGOutput = tool_eval_4_diagrams / svg

          //val proc:ISZ[String] = ISZ("dot", "-Tpdf", dot.canon.value, "-o", dotPDFOutput.canon.value)
          //Os.proc(proc).run()

          val proc2:ISZ[String] = ISZ("dot", "-Tsvg", dot.canon.value, "-o", dotPNGOutput.canon.value)
          Os.proc(proc2).run()

          //val sel4OnlyArchPDF = "diagrams/CAmkES-arch-SeL4_Only.pdf"
          val readmePath = s"diagrams/${svg}"

          readmeEntries = readmeEntries :+ st"""## ${platform} Arch
                                               |  ![${platform}](${readmePath})"""

        }
      }

      val readme = projectDir / "readme_autogen.md"

      val aadlArch = "diagrams/aadl-arch.png"

      val readmest = st"""# ${project.simpleName}
                         |
                         |## AADL Arch
                         |  ![aadl](${aadlArch})
                         |
                         |${(readmeEntries, "\n\n")}
                         |"""

      readme.writeOver(readmest.render)
    }
    return 0
  }
}