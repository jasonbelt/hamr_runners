package org.sireum.cli.hamr_runners.casex

import org.sireum._

object CASE_phase2_june {

  val rootDir = Os.home / "devel/case/CASETeam/examples/ksu-proprietary"

  val runTranspiler: B = F

  def gen(name: String, json: String): (String, Os.Path, Os.Path) = {
    val modelDir = rootDir / name
    return (name, modelDir, modelDir / ".slang" / json)
  }

  val projectsDirs: ISZ[(String, Os.Path, Os.Path)] = ISZ(

    gen("Phase-2-UAV-Experimental-Platform-June-hamr", "SW_SW_Impl_Instance.json"),

    gen("Phase-2-UAV-Experimental-Platform-June-hamr_dataports", "SW_SW_Impl_Instance.json"),

    gen("Phase-2-UAV-Experimental-Platform-Transformed", "UAV_UAV_Impl_Instance.json")
  )

  val platforms: ISZ[Cli.HamrPlatform.Type] = ISZ(
    //Cli.HamrPlatform.SeL4_TB,
    //Cli.HamrPlatform.SeL4_Only,
    Cli.HamrPlatform.SeL4
  )

  def main(args: Array[Predef.String]): Unit = {

    for (project <- projectsDirs) {
      val projectDir = project._2
      val slangFile = project._3

      if(!projectDir.exists) {
        throw new RuntimeException(s"${projectDir} does not exist");
      }

      var readmeEntries: ISZ[ST] = ISZ()

      val outputDir = projectDir / "june"

      for (platform <- platforms) {
        val camkesOutputDir = platform match {
          case Cli.HamrPlatform.SeL4_TB => outputDir / "src/c/CAmkES_seL4_TB_VM"
          case Cli.HamrPlatform.SeL4_Only => outputDir / "src/c/CAmkES_seL4_Only_VM"
          case Cli.HamrPlatform.SeL4 => outputDir / "src/c/CAmkES_seL4_VM"
          case _ => throw new RuntimeException("??")
        }

        //outputDir.removeAll()

        val o = Util.o(
          args = ISZ(slangFile.value),
          platform = platform,

          outputDir = Some(outputDir.value),

          bitWidth = 32,
          maxStringSize = 256,
          maxArraySize = 1,
          runTranspiler = runTranspiler,

          camkesOutputDir = Some(camkesOutputDir.value),
          aadlRootDir = Some(projectDir.value)
        )

        cli.HAMR.codeGen(o)


        val dot = camkesOutputDir / "graph.dot"

        if(dot.exists) {

          val tool_eval_4_diagrams = projectDir / "diagrams"

          val png = s"CAmkES-arch-${platform}.png"

          //val dotPDFOutput = tool_eval_4_diagrams / s"CAmkES-arch-${platform}.pdf"
          val dotPNGOutput = tool_eval_4_diagrams / png

          //val proc:ISZ[String] = ISZ("dot", "-Tpdf", dot.canon.value, "-o", dotPDFOutput.canon.value)
          //Os.proc(proc).run()

          val proc2:ISZ[String] = ISZ("dot", "-Tpng", dot.canon.value, "-o", dotPNGOutput.canon.value)
          Os.proc(proc2).run()

          //val sel4OnlyArchPDF = "diagrams/CAmkES-arch-SeL4_Only.pdf"
          val readmePath = s"diagrams/${png}"

          readmeEntries = readmeEntries :+ st"""## ${platform} Arch
                                               |  ![${platform}](${readmePath})"""

        }
      }

      val readme = projectDir / "readme_autogen.md"

      val aadlArch = "diagrams/aadl-arch.png"

      val readmest = st"""# ${project._1}
                         |
                         |## AADL Arch
                         |  ![aadl](${aadlArch})
                         |
                         |${(readmeEntries, "\n\n")}
                         |"""

      readme.writeOver(readmest.render)
    }
  }
}