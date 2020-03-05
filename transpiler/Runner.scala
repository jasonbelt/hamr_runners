package org.sireum.cli.hamr_runners.transpiler

import org.sireum._

object Runner {
  
  def main(args: Array[Predef.String]): Unit = {
    val args: String = st"""slang
                   |transpilers
                   |c
                   |--sourcepath
                   |/home/sireum/devel/sel4/home/CASETeam-KU/examples/ksu-proprietary/producer-splatfilter-consumer_ARTseL4/pfc-project/src/main/art:/home/sireum/devel/sel4/home/CASETeam-KU/examples/ksu-proprietary/producer-splatfilter-consumer_ARTseL4/pfc-project/src/main/bridge:/home/sireum/devel/sel4/home/CASETeam-KU/examples/ksu-proprietary/producer-splatfilter-consumer_ARTseL4/pfc-project/src/main/component:/home/sireum/devel/sel4/home/CASETeam-KU/examples/ksu-proprietary/producer-splatfilter-consumer_ARTseL4/pfc-project/src/main/data:/home/sireum/devel/sel4/home/CASETeam-KU/examples/ksu-proprietary/producer-splatfilter-consumer_ARTseL4/pfc-project/src/main/architecture-isolated/pfc_project/PFC_Sys_Impl_Instance_proc_sw_producer:/home/sireum/devel/sel4/home/CASETeam-KU/examples/ksu-proprietary/producer-splatfilter-consumer_ARTseL4/pfc-project/src/main/sel4_nix_isolated/pfc_project/PFC_Sys_Impl_Instance_proc_sw_producer
                   |--output-dir
                   |/home/sireum/devel/sel4/home/CASETeam-KU/examples/ksu-proprietary/producer-splatfilter-consumer_ARTseL4/pfc-project/src/c/CAmkES_seL4/hamr
                   |--name
                   |main
                   |--apps
                   |pfc_project.PFC_Sys_Impl_Instance_proc_sw_producer.PFC_Sys_Impl_Instance_proc_sw_producer
                   |--fingerprint
                   |3
                   |--bits
                   |32
                   |--string-size
                   |256
                   |--sequence-size
                   |12
                   |--sequence
                   |MS[org.sireum.Z,art.Bridge]=3;MS[org.sireum.Z,org.sireum.MOption[art.Bridge]]=3;IS[org.sireum.Z,art.UPort]=2;IS[org.sireum.Z,art.UConnection]=2
                   |--constants
                   |art.Art.maxComponents=3;art.Art.maxPorts=8
                   |--forward
                   |art.ArtNative=pfc_project.PFC_Sys_Impl_Instance_proc_sw_producer.PFC_Sys_Impl_Instance_proc_sw_producer
                   |--stack-size
                   |53248
                   |--stable-type-id
                   |--exts
                   |/home/sireum/devel/sel4/home/CASETeam-KU/examples/ksu-proprietary/producer-splatfilter-consumer_ARTseL4/pfc-project/src/c/ext-c/ext.c:/home/sireum/devel/sel4/home/CASETeam-KU/examples/ksu-proprietary/producer-splatfilter-consumer_ARTseL4/pfc-project/src/c/ext-c/ext.h
                   |--lib-only
                   |--verbose
                   |""".render
    
    val _args = ops.StringOps(args).split(c => c.value == '\n').map(m => m.native)
    
    org.sireum.Sireum.main(_args.elements.toArray)
  }
}
