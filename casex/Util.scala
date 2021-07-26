package org.sireum.cli.hamr_runners.casex

import org.sireum._

object Util {
  val o = Cli.SireumHamrCodegenOption(
    help = "",
    args = ISZ(),
    msgpack = F,
    verbose = T,
    platform = Cli.SireumHamrCodegenHamrPlatform.Linux,
    outputDir = None(),
    packageName = None(),
    noProyekIve = F,
    noEmbedArt = F,
    devicesAsThreads = F,
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
