// #Sireum
package org.sireum.cli.hamr_runners

import org.sireum._

object QuickRunner extends App{

  val clearFiles: B = F

  val mult_thread_vm: Os.Path = Os.home / "CASE/Sireum/hamr/codegen/jvm/src/test/scala/models/CodeGenTest_Base/vm-with-multiple-threads/aadl"
  val mult_thread_vmJson: Os.Path = mult_thread_vm / ".slang/model_m_impl_Instance.json"

  val hardened: Os.Path = Os.home / "devel/case/case-loonwerks/CASE_Simple_Example_V4/Hardened"
  val hardenedJson: Os.Path = hardened / ".slang" / "MC_MissionComputer_Impl_Instance.json"


  val isoletteDir: Os.Path = Os.home / "devel"/ "gumbo" / "isolette" / "aadl"
  val isoletteJson: Os.Path = isoletteDir / ".slang" / "Isolette_isolette_single_sensor_Instance.json"

  val buildingDir: Os.Path = Os.home / "temp/x/building-control-art-scheduling/aadl"
  val buildingJson: Os.Path = buildingDir / ".slang/BuildingControl_BuildingControlDemo_i_Instance.json"

  val voterDir: Os.Path = Os.home / "devel/gumbo/gumbo-models/voter/RedundantSensors_Bless"
  val voterJson: Os.Path = voterDir / ".slang/SensorSystem_redundant_sensors_impl_Instance.json"

  val aadlDir: Os.Path = mult_thread_vm
  val json: Os.Path = mult_thread_vmJson

  val rootDir: Os.Path = aadlDir.up / "hamr"
  val outputDir: Os.Path = rootDir / "slang"
  val slangOutputCDir: Os.Path = rootDir / "c"
  val camkesOutputDir: Os.Path = rootDir / "camkes"

  val platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type] =
    ISZ(Cli.SireumHamrCodegenHamrPlatform.SeL4)

  def o(platform: Cli.SireumHamrCodegenHamrPlatform.Type): Cli.SireumHamrCodegenOption = {
    return Cli.SireumHamrCodegenOption(
      help = "",
      args = ISZ(json.value),
      msgpack = F,
      verbose = T,
      platform = platform,

      packageName = Some("base"),
      noProyekIve = T,
      noEmbedArt = F,
      devicesAsThreads = F,
      excludeComponentImpl = F,

      bitWidth = 32,
      maxStringSize = 256,
      maxArraySize = 1,
      runTranspiler = F,

      slangAuxCodeDirs = ISZ(),
      slangOutputCDir = Some(slangOutputCDir.value),
      outputDir = Some(outputDir.value),

      camkesOutputDir = Some(camkesOutputDir.value),
      camkesAuxCodeDirs = ISZ(),
      aadlRootDir = Some(aadlDir.value),

      experimentalOptions = ISZ("PROCESS_BTS_NODES")
    )
  }

  override def main(args: ISZ[String]): Z = {
    if(clearFiles) {
      ISZ(outputDir / "bin", outputDir / "src", outputDir / "build.sbt", outputDir / "build.sc", outputDir / "versions.properties").foreach(f => f.removeAll())

      slangOutputCDir.removeAll()

      camkesOutputDir.removeAll()
    }

    for(p <- platforms) {
      val exitCode = org.sireum.cli.HAMR.codeGen(o(p))
      if(exitCode != 0) {
        eprintln(s"Error while generating ${p} - ${exitCode}")
      } else {
        println(s"${aadlDir.name} ${p} completed with ${exitCode}")
      }
    }

    proc"git checkout ${(outputDir / "src/main/bridge").string}".at(outputDir).console.runCheck()
    return 0
  }
}
