// #Sireum
package org.sireum.cli.hamr_runners.casex

import org.sireum._
import org.sireum.Cli.SireumHamrCodegenHamrPlatform
import org.sireum.message.Reporter

object QuickRunner extends App{

  ///home/vagrant/devel/case/case-loonwerks/CASE_Simple_Example_V4/Hardened/.slang/MC_MissionComputer_Impl_Instance.json

  val aadlDir: Os.Path = Os.home / "devel/case/case-loonwerks/CASE_Simple_Example_V4/Hardened"
  val json: Os.Path = aadlDir / ".slang/MC_MissionComputer_Impl_Instance.json"
  val outputDir: Os.Path = aadlDir / "hamr"
  val platform: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.SeL4

  val o: Cli.SireumHamrCodegenOption = Cli.SireumHamrCodegenOption(
    help = "",
    args = ISZ(json.value),
    msgpack = F,
    verbose = T,
    platform = platform,
    runtimeMonitoring = F,
    packageName = Some("base"),
    noProyekIve = F,
    noEmbedArt = F,
    devicesAsThreads = F,
    excludeComponentImpl = T,
    genSbtMill = T,

    bitWidth = 32,
    maxStringSize = 256,
    maxArraySize = 1,
    runTranspiler = F,

    slangAuxCodeDirs = ISZ(),
    slangOutputCDir = None(),
    outputDir = Some(outputDir.value),

    camkesOutputDir = None(),
    camkesAuxCodeDirs = ISZ(),
    aadlRootDir = Some(aadlDir.value),

    experimentalOptions = ISZ()
  )

  override def main(args: ISZ[String]): Z = {
    val reporter = Reporter.create
    val exitCode = org.sireum.cli.HAMR.codeGen(o, reporter)

    println(s"${aadlDir.name} completed with ${exitCode}")

    return exitCode
  }
}
