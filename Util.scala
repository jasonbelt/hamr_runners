package org.sireum.cli.hamr_runners

import org.sireum._
import org.sireum.cli._

object Util {
  val o = Cli.SireumHamrCodegenOption(
    help = "",
    args = ISZ(),
    msgpack = F,
    verbose = T,
    runtimeMonitoring = F,
    platform = Cli.SireumHamrCodegenHamrPlatform.Linux,
    outputDir = None(),
    packageName = None(),
    noProyekIve = F,
    noEmbedArt = F,
    devicesAsThreads = F,
    genSbtMill = T,

    slangAuxCodeDirs = ISZ(),
    slangOutputCDir = None(),
    excludeComponentImpl = T,
    bitWidth = 32,
    maxStringSize = 256,
    maxArraySize = 16,
    runTranspiler = T,

    camkesOutputDir = None(),
    camkesAuxCodeDirs = ISZ(),
    aadlRootDir = None(),
    experimentalOptions = ISZ()
  )
}
