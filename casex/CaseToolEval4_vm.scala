// #Sireum

package org.sireum.cli.hamr_runners.casex

import org.sireum.Os.Proc
import org.sireum._
import org.sireum.hamr.codegen.CodeGenPlatform

object CaseToolEval4_vm extends App {

  val build: B = T
  val simulate: B = T
  val timeout: Z = 15000

  val sel4: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4
  val sel4_tb: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_TB
  val sel4_only: Cli.HamrPlatform.Type = Cli.HamrPlatform.SeL4_Only

  val case_tool_evaluation_dir: Os.Path = Os.home / "devel/case/CASE-loonwerks/TA5/tool-evaluation-4/HAMR/examples"

  def gen(name: String, json: String, platforms: ISZ[Cli.HamrPlatform.Type]): (String, Os.Path, Os.Path, ISZ[Cli.HamrPlatform.Type]) = {
    val modelDir = case_tool_evaluation_dir / name
    val simpleName = Os.path(name).name // get last dir name
    return (simpleName, modelDir, modelDir / ".slang" / json, platforms)
  }

  val tests: ISZ[(String, Os.Path, Os.Path, ISZ[Cli.HamrPlatform.Type])] = ISZ(

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

    // VMs
    gen("test_data_port_periodic_domains_VM/both_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
    gen("test_data_port_periodic_domains_VM/receiver_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
    gen("test_data_port_periodic_domains_VM/sender_vm", "test_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),

    gen("test_event_data_port_periodic_domains_VM/both_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
    gen("test_event_data_port_periodic_domains_VM/receiver_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),
    gen("test_event_data_port_periodic_domains_VM/sender_vm", "test_event_data_port_periodic_domains_top_impl_Instance.json", ISZ(sel4_only, sel4)),

    //gen("test_event_data_port_fan_ins_outs_periodic_domains", "test_event_data_port_fan_ins_outs_periodic_domains_top_impl_Instance.json", ISZ(sel4_only))
  )

  def run(): Unit = {

    for (project <- tests) {
      val projectDir = project._2
      val slangFile = project._3

      val hasVMs = ops.StringOps(projectDir.name).endsWith("vm")

      println("***************************************")
      println(projectDir)
      println("***************************************")

      if(!projectDir.exists) {
        halt(s"${projectDir} does not exist");
      }

      var readmeEntries: ISZ[ST] = ISZ()
      var expectedOutputEntries: ISZ[ST] = ISZ()

      for (platform <- project._4) {

        val outputDir: Os.Path = platform match {
          case Cli.HamrPlatform.SeL4_TB => projectDir / "CAmkES_seL4_TB"
          case Cli.HamrPlatform.SeL4_Only => projectDir / "CAmkES_seL4_Only"
          case Cli.HamrPlatform.SeL4 => projectDir / "CAmkES_seL4"
          case _ => halt("??")
        }

        val camkesOutputDir: Os.Path = platform match {
          case Cli.HamrPlatform.SeL4_TB => outputDir
          case Cli.HamrPlatform.SeL4_Only => outputDir
          case Cli.HamrPlatform.SeL4 => outputDir / "src/c/CAmkES_seL4"
          case _ => halt("??")
        }

        outputDir.removeAll()

        val o = Cli.HamrCodeGenOption(
          help = "",
          args = ISZ(slangFile.value),
          json = T,
          verbose = T,
          platform = platform,

          packageName = Some(project._1),
          embedArt = T,
          devicesAsThreads = F,
          excludeComponentImpl = T,

          bitWidth = 32,
          maxStringSize = 256,
          maxArraySize = 1,
          runTranspiler = build,

          slangAuxCodeDirs = ISZ(),
          slangOutputCDir = None(),
          outputDir = Some(outputDir.value),

          camkesOutputDir = Some(camkesOutputDir.value),
          camkesAuxCodeDirs = ISZ(),
          aadlRootDir = Some(projectDir.value)
        )

        org.sireum.cli.HAMR.codeGen(o)

        var expectedOutput: Option[String] = None()

        if(build) {
          val script = camkesOutputDir / "bin" / "run-camkes.sh"

          val args: ISZ[String] = ISZ(script.value, "-n")

          Os.proc(args).at(camkesOutputDir).console.runCheck()

          if(simulate) {
            val _camkesDir: String =
              if(ops.StringOps(projectDir.name).endsWith("vm")) "camkes-arm-vm"
              else "camkes"

            val simulateScript = Os.home / "CASE" / _camkesDir / s"build_${camkesOutputDir.name}" / "simulate"
            if(simulateScript.exists) {
              val p = Proc(ISZ("simulate"), Os.cwd, Map.empty, T, None(), F, F, F, F, F, timeout, F)
              val results = p.at(simulateScript.up).run()
              cprint(F, results.out)
              cprint(T, results.err)

              Os.proc(ISZ("pkill", "qemu")).console.runCheck()

              expectedOutput = parseOutput(results.out)
            }
          }
        }

        val dot = camkesOutputDir / "graph.dot"
        val tool_eval_4_diagrams = projectDir / "diagrams"

        val dotFormat: String = "svg"

        if(dot.exists) {
          {
            //val dotPDFOutput = tool_eval_4_diagrams / s"CAmkES-arch-${platform}.pdf"
            //val sel4OnlyArchPDF = "diagrams/CAmkES-arch-SeL4_Only.pdf"
            //val proc:ISZ[String] = ISZ("dot", "-Tpdf", dot.canon.value, "-o", dotPDFOutput.canon.value)
            //Os.proc(proc).run()
          }

          {
            val fname = s"CAmkES-HAMR-arch-${platform}"
            delStaleDiagrams(tool_eval_4_diagrams, fname)
            val dotPNGOutput = tool_eval_4_diagrams / s"${fname}.${dotFormat}"
            val proc2: ISZ[String] = ISZ("dot", s"-T${dotFormat}", dot.canon.value, "-o", dotPNGOutput.canon.value)
            Os.proc(proc2).console.run()

            val readmePath = s"diagrams/${fname}.${dotFormat}"

            readmeEntries = readmeEntries :+ st"""### CAmkES HAMR ${platform} Arch
                                                 |  ![${platform}](${readmePath})"""
          }

          val camkesDir: Os.Path =
            if(hasVMs)  Os.home / "CASE" / "camkes-arm-vm"
            else Os.home / "CASE" / "camkes"

          val camkesArch = camkesDir / s"build_${camkesOutputDir.name}" / "graph.dot"
          if(camkesArch.canon.exists) {
            val fname = s"CAmkES-arch-${platform}"
            delStaleDiagrams(tool_eval_4_diagrams, fname)
            val dotCamkesPNGOutput = tool_eval_4_diagrams / s"${fname}.${dotFormat}"
            val proc:ISZ[String] = ISZ("dot", s"-T${dotFormat}", camkesArch.canon.value, "-o", dotCamkesPNGOutput.canon.value)
            Os.proc(proc).console.runCheck()

            val readmePath = s"diagrams/${fname}.${dotFormat}"

            readmeEntries = readmeEntries :+ st"""### CAmkES ${platform} Arch
                                                 |  ![${platform}](${readmePath})"""
          }
        }

        if(expectedOutput.nonEmpty) {
          expectedOutputEntries = expectedOutputEntries :+
            st"""### CAmkES ${platform} Expected Output
                |  ${expectedOutput}"""
        }
      }

      val aadlArch = projectDir / "diagrams" / "aadl-arch.png"
      if(aadlArch.exists) {
        readmeEntries = st"""### AADL Arch
                            |  ![aadl](diagrams/${aadlArch.name})""" +: readmeEntries
      }

      val readme = projectDir / "readme_autogen.md"

      val expected: Option[ST] = if(expectedOutputEntries.nonEmpty) {
        val t = timeout / 1000
        Some(st"""## Expected Output : Timeout = $t seconds
                 |
                 |  ${(expectedOutputEntries, "\n\n")}""")
      } else { None() }

      val readmest = st"""# ${project._1}
                         |
                         |## Diagrams
                         |
                         |${(readmeEntries, "\n\n")}
                         |
                         |${expected}
                         |"""

      readme.writeOver(readmest.render)
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

  def parseOutput(out: String): Option[String] = {
    val o = ops.StringOps(out)
    val pos = o.stringIndexOf("Booting all finished")
    if(pos > 0) {
      return Some(o.substring(pos, o.size))
    } else {
      return None()
    }
  }

  override def main(args: ISZ[String]): Z = {
    run()
    return 0
  }
}