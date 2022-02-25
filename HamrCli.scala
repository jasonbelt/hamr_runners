// #Sireum
package org.sireum.cli.hamr_runners

import org.sireum._
import Cli.SireumHamrCodegenHamrPlatform._

object HamrCli extends App {

  @datatype class Project(val aadlDir : Os.Path,
                          val json: String,
                          val packageName: Option[String],
                          val platforms: ISZ[Cli.SireumHamrCodegenHamrPlatform.Type],
                          val options: ISZ[String])

  val pingPong: Project = Project(
    aadlDir = Os.home / "devel/camkes-vm/camkes-ping-pong/ping-pong/aadl",
    json = "Ping_Pong_top_impl_Instance.json",
    packageName = Some("slang"),
    platforms = ISZ(SeL4),
    options = ISZ("--exclude-component-impl")
  )

  val defaultOptions: ISZ[String] = ISZ(
    "--verbose",
    "--bit-width", "32",
    "--max-string-size", "256",
    "--max-array-size", "1",
    "--no-proyek-ive",
  )

  val projects: ISZ[Project] = ISZ(pingPong)

  override def main(args: ISZ[String]): Z = {

    for(project <- projects) {
      for (platform <- project.platforms) {
        val aadlDir: Os.Path = project.aadlDir
        val jsonLoc = aadlDir / ".slang" / project.json

        val hamrDir: Os.Path = aadlDir.up / "hamr"
        val outputDir: Os.Path = hamrDir / "slang"
        val slangOutputCDir: Os.Path = hamrDir / "c"
        val camkesOutputDir: Os.Path = hamrDir / "camkes"

        val plat: String = platform match {
          case SeL4 => "seL4"
          case SeL4_Only => "seL4_Only"
          case SeL4_TB => "seL4_TB"
          case _ => platform.name
        }

        var projectArgs: ISZ[String] = ISZ(
          "--output-dir", outputDir.value,
          "--output-c-dir", slangOutputCDir.value,
          "--camkes-output-dir", camkesOutputDir.value,
          "--aadl-root-dir", aadlDir.value,
          "--platform", plat
        )

        if(project.packageName.nonEmpty) {
          projectArgs = projectArgs :+ "--package-name" :+ project.packageName.get
        }

        org.sireum.Sireum.run(ISZ[String]("hamr", "codegen") ++
          projectArgs ++ project.options ++ defaultOptions :+ jsonLoc.value)
      }
    }

    return 0
  }
}
