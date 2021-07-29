// #Sireum
package org.sireum.cli.hamr_runners

import org.sireum._

object QuickRunner extends App{


  val buildingDir: Os.Path = Os.home / "temp/x/building_control_gen_mixed--Linux/aadl"
  val buildingJson: Os.Path = buildingDir / ".slang/BuildingControl_BuildingControlDemo_i_Instance.json"

//Wrote: /home/vagrant/devel/gumbo/gumbo-models/building-control/building-control-bless-mixed/aadl/.slang/BuildingControl_Bless_BuildingControlDemo_i_Instance.json
  val voterDir: Os.Path = Os.home / "devel/gumbo/gumbo-models/voter/RedundantSensors_Bless"
  val voterJson: Os.Path = voterDir / ".slang/SensorSystem_redundant_sensors_impl_Instance.json"

  val aadlDir: Os.Path = buildingDir
  val json: Os.Path = buildingJson
  val outputDir: Os.Path = aadlDir.up / "hamr"
  val platform: Cli.SireumHamrCodegenHamrPlatform.Type = Cli.SireumHamrCodegenHamrPlatform.Linux

  val o: Cli.SireumHamrCodegenOption = Cli.SireumHamrCodegenOption(
    help = "",
    args = ISZ(json.value),
    msgpack = F,
    verbose = T,
    platform = platform,

    packageName = Some("packageName_not_set"),
    noProyekIve = F,
    noEmbedArt = F,
    devicesAsThreads = F,
    excludeComponentImpl = F,

    bitWidth = 32,
    maxStringSize = 256,
    maxArraySize = 1,
    runTranspiler = F,

    slangAuxCodeDirs = ISZ(),
    slangOutputCDir = Some((outputDir / "c").value),
    outputDir = Some((outputDir / "slang").value),

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
