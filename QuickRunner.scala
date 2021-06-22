// #Sireum
package org.sireum.cli.hamr_runners

import org.sireum.Cli.HamrPlatform
import org.sireum._

object QuickRunner extends App{


//Wrote: /home/vagrant/devel/gumbo/gumbo-models/building-control/building-control-bless-mixed/aadl/.slang/BuildingControl_Bless_BuildingControlDemo_i_Instance.json
  val aadlDir: Os.Path = Os.home / "devel/gumbo/gumbo-models/building-control/building-control-bless-mixed/aadl"
  val json: Os.Path = aadlDir / ".slang/BuildingControl_Bless_BuildingControlDemo_i_Instance.json"
  val outputDir: Os.Path = aadlDir.up / "hamr3"
  val platform: HamrPlatform.Type = HamrPlatform.JVM

  val o: Cli.HamrCodeGenOption = Cli.HamrCodeGenOption(
    help = "",
    args = ISZ(json.value),
    msgpack = F,
    verbose = T,
    platform = platform,

    packageName = Some("hamr3"),
    noEmbedArt = F,
    devicesAsThreads = F,
    excludeComponentImpl = T,

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

    experimentalOptions = ISZ("PROCESS_BTS_NODES")
  )

  override def main(args: ISZ[String]): Z = {
    val exitCode = org.sireum.cli.HAMR.codeGen(o)

    println(s"${aadlDir.name} completed with ${exitCode}")

    return exitCode
  }
}
