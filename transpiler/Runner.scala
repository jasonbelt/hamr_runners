package org.sireum.cli.hamr_runners.transpiler

import org.sireum._

object Runner {
  
  def main(args: Array[Predef.String]): Unit = {
    var args: String = st"""slang
                   |transpilers
                   |c
                   |--sourcepath
                   |/home/vagrant/devel/gumbo/isolette_mod/hamr/slang/src/common/data/main:/home/vagrant/devel/gumbo/isolette_mod/hamr/slang/src/common/library/main:/home/vagrant/devel/n
                   |--output-dir
                   |/home/vagrant/devel/gumbo/isolette_mod/hamr/slang/bin/../../c/nix
                   |--name
                   |main
                   |--apps
                   |isolette.Manage_Regulator_Interface_impl_thermostat_regulate_temperature_manage_regulator_interface_App,isolette.Manage_Heat_Source_impl_thermostat_regulate_temperato
                   |--fingerprint
                   |3
                   |--bits
                   |32
                   |--string-size
                   |256
                   |--sequence-size
                   |57
                   |--sequence
                   |IS[Z,art.Bridge]=9;MS[Z,Option[art.Bridge]]=9;IS[Z,art.UPort]=9;IS[Z,art.UConnection]=25
                   |--constants
                   |art.Art.maxComponents=9;art.Art.maxPorts=57
                   |--forward
                   |art.ArtNative=isolette.ArtNix,isolette.Platform=isolette.PlatformNix
                   |--stack-size
                   |217088
                   |--stable-type-id
                   |--exts
                   |/home/vagrant/devel/gumbo/isolette_mod/hamr/slang/bin/../../c/ext-c:/home/vagrant/devel/gumbo/isolette_mod/hamr/slang/bin/../../c/etc
                   |--verbose
                   |""".render

    args = (Os.home / "devel/gumbo/isolette_mod/hamr/slang/transpiler_script").read

    val _args = ops.StringOps(args).split(c => c.value == '\n').map(m => m.native)
    
    org.sireum.Sireum.main(_args.elements.toArray)
  }
}
