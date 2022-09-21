// #Sireum
package org.sireum.cli.hamr_runners

import org.sireum._
import Cli.SireumHamrCodegenHamrPlatform._
import org.sireum.message.Reporter

@datatype class Project (val aadlDir : Os.Path,
                         val outputDir: Option[Os.Path],
                         val json: String,
                         val packageName: Option[String],
                         val platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type]) {
  def jsonLoc : Os.Path = { return aadlDir / ".slang" / json }
}

object QuickRunner extends App {

  val clearFiles: B = F

  val mult_thread_vm: Project = Project(
    aadlDir = Os.home / "CASE/Sireum/hamr/codegen/jvm/src/test/scala/models/CodeGenTest_Base/vm-with-multiple-threads/aadl",
    outputDir = None(),
    json = "model_m_impl_Instance.json",
    packageName = None(),
    platforms = ISZ(SeL4))

  val hardened: Project = Project (
    aadlDir = Os.home / "devel/case/case-loonwerks/CASE_Simple_Example_V4/Hardened",
    outputDir = None(),
    json = "MC_MissionComputer_Impl_Instance.json",
    packageName = None(),
    platforms = ISZ(JVM))

  val isolette: Project = Project (
    aadlDir = Os.home / "devel"/ "gumbo" / "isolette" / "aadl",
    outputDir = None(),
    json = "Isolette_isolette_single_sensor_Instance.json",
    packageName = Some("isolette"),
    platforms = ISZ(SeL4))

  val pingpong: Project = Project (
    aadlDir = Os.home / "devel/camkes-vm/camkes-ping-pong/ping-pong/aadl",
    outputDir = None(),
    json = "Ping_Pong_top_impl_Instance.json",
    packageName = Some("slang"),
    platforms = ISZ(SeL4))


  val building: Project = Project(
    aadlDir = Os.home / "temp/x/building-control-art-scheduling/aadl",
    outputDir = None(),
    json = "BuildingControl_BuildingControlDemo_i_Instance.json",
    packageName = None(),
    platforms = ISZ(JVM))

  val voter: Project = Project(
    aadlDir = Os.home / "devel/gumbo/gumbo-models/voter/RedundantSensors_Bless",
    outputDir = None(),
    json = "SensorSystem_redundant_sensors_impl_Instance.json",
    packageName = None(),
    platforms = ISZ(JVM))

  val aeic2020_tc: Project = Project (
    aadlDir = Os.home / "devel/aeic2002_tc_module/aadl",
    outputDir = None(),
    json = "TemperatureControl_TempControlSystem_i_Instance.json",
    packageName = None(),
    platforms = ISZ(JVM))

  val redundantSensors: Project = Project (
    aadlDir = Os.home / "devel/sirfur/sirfur_models/redundant_sensors/aadl/",
    outputDir = None(),
    json = "TestHarnessSystem_TestHarness_triplex_Instance.json",
    packageName = Some("t"),
    platforms = ISZ(JVM)
  )

  val initialize_entrypoint: Project = Project (

    aadlDir = Os.home / "devel/sireum/osate-plugin/org.sireum.aadl.osate.tests/projects/org/sireum/aadl/osate/tests/gumbo/initialize-entrypoint",
    outputDir = Some(Os.home / "devel/sireum/osate-plugin/org.sireum.aadl.osate.tests/projects/org/sireum/aadl/osate/tests/gumbo/initialize-entrypoint/hamr"),
    json = "Initialize_Entrypoint_s_impl_Instance.json",
    packageName = Some("t"),
    platforms = ISZ(JVM)
  )

  val rts: Project = Project(
    aadlDir = Os.home / "temp/building_control_gen_mixed",
    outputDir = Some(Os.home / "temp/building_control_gen_mixed/slang"),
    json = "BuildingControl_BuildingControlDemo_i_Instance.json",
    packageName = Some("bless"),
    platforms = ISZ(JVM)
  )
  val project: Project = rts

  val aadlDir: Os.Path = project.aadlDir
  val rootDir: Os.Path = if(project.outputDir.nonEmpty) project.outputDir.get else aadlDir.up / "hamr"
  val outputDir: Os.Path = rootDir / "slang"
  val slangOutputCDir: Os.Path = rootDir / "c"
  val camkesOutputDir: Os.Path = rootDir / "camkes"

  def o(platform: Cli.SireumHamrCodegenHamrPlatform.Type): Cli.SireumHamrCodegenOption = {
    return Cli.SireumHamrCodegenOption(
      help = "",
      args = ISZ(project.jsonLoc.value),
      msgpack = F,
      verbose = T,
      platform = platform,

      packageName = project.packageName,
      noProyekIve = T,
      noEmbedArt = F,
      devicesAsThreads = F,
      excludeComponentImpl = T,
      genSbtMill = F,

      bitWidth = 32,
      maxStringSize = 256,
      maxArraySize = 1,
      runTranspiler = F,

      slangAuxCodeDirs = ISZ(),
      slangOutputCDir = Some(slangOutputCDir.value),
      outputDir = Some(outputDir.value),

      camkesOutputDir = Some(camkesOutputDir.value),
      camkesAuxCodeDirs = ISZ(),
      aadlRootDir = Some(project.aadlDir.value),

      experimentalOptions = ISZ("PROCESS_BTS_NODES", "GENERATE_REFINEMENT_PROOF")
    )
  }

  override def main(args: ISZ[String]): Z = {
    assert(project.aadlDir.exists)
    assert(project.jsonLoc.exists)

    if(clearFiles) {
      ISZ(outputDir / "bin", outputDir / "src", outputDir / "build.sbt", outputDir / "build.sc", outputDir / "versions.properties").foreach(f => f.removeAll())

      slangOutputCDir.removeAll()

      camkesOutputDir.removeAll()
    }

    for(p <- project.platforms) {
      val reporter = Reporter.create
      val exitCode = org.sireum.cli.HAMR.codeGen(o(p), reporter)
      if(exitCode != 0) {
        eprintln(s"Error while generating ${p} - ${exitCode}")
      } else {
        println(s"${aadlDir.name} ${p} completed with ${exitCode}")
      }
    }

    //proc"git checkout ${(outputDir / "src/main/bridge").string}".at(outputDir).console.runCheck()
    return 0
  }
}
