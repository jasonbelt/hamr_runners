package org.sireum.cli.hamr_runners

import org.sireum._
import org.sireum.cli._

object Util {
  val o = Cli.HamrCodeGenOption(
    help = "",
    args = ISZ(),
    msgpack = F,
    verbose = T,
    platform = Cli.HamrPlatform.Linux,
    outputDir = None(),
    packageName = None(),
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
