package org.sireum.cli.hamr_runners.casex

import org.sireum._
import org.sireum.message.Reporter

case class Dirs (aadlDir: Os.Path,
                 slangFile: Os.Path,
                 outputDir: Os.Path
                )

object Isaac_UAV_Test_Runner {

  def main(args: Array[Predef.String]): Unit = {
    val root = Os.home / "devel/slang-embedded/UAV_Monitor_Examples"

    val projects = ISZ({
      val rootDir = root / "Event_Data_Ports"

      Dirs(
        aadlDir = rootDir,
        slangFile = rootDir / ".slang/UAV_UAV_Impl_Instance.json",
        outputDir = rootDir / "hamr")
    },
      {
        val rootDir = root / "Data_Ports"

        Dirs(
          aadlDir = rootDir,
          slangFile = rootDir / ".slang/UAV_UAV_Impl_Instance.json",
          outputDir = rootDir / "hamr")
      }
    )

    for(p <- projects) {
      val o = Util.o(
        args = ISZ(p.slangFile.value),
        devicesAsThreads = F,
        outputDir = Some(p.outputDir.value),
        excludeComponentImpl = F,

        aadlRootDir = Some(p.aadlDir.value),
      )

      def run(o: Cli.SireumHamrCodegenOption): Int = {
        val reporter = Reporter.create
        val ret = cli.HAMR.codeGen(o, reporter)
        return ret.toInt
      }

      var ret = 0

      if (ret == 0) {
        ret = run(o(
          platform = Cli.SireumHamrCodegenHamrPlatform.JVM,
        ))
      }
    }
  }
}