::#! 2> /dev/null                                             #
@ 2>/dev/null # 2>nul & echo off & goto BOF                   #
if [ -f "$0.com" ] && [ "$0.com" -nt "$0" ]; then             #
  exec "$0.com" "$@"                                          #
fi                                                            #
rm -f "$0.com"                                                #
if [ -z ${SIREUM_HOME} ]; then                                #
  echo "Please set SIREUM_HOME env var"                       #
  exit -1                                                     #
fi                                                            #
exec ${SIREUM_HOME}/bin/sireum slang run -n "$0" "$@"      #
:BOF
if not defined SIREUM_HOME (
  echo Please set SIREUM_HOME env var
  exit /B -1
)
%SIREUM_HOME%\bin\sireum.bat slang run -n "%0" %*
exit /B %errorlevel%
::!#
// #Sireum

import org.sireum._

val localUpdateSites: B = ops.ISZOps(Os.cliArgs).contains("local")

val sireumHome = Os.path(Os.env("SIREUM_HOME").get)

val osateLoc = Os.home / ".sireum" / "phantom" / "osate-2.9.1-vfinal" / "osate"
val fmideLoc = sireumHome / "bin" / "linux" / "fmide" / "fmide"

val osateExe: Os.Path = if(ops.ISZOps(Os.cliArgs).contains("fmide")) fmideLoc else osateLoc

//val osateVer= "2.9.2-vfinal"
//val OSATE_PRODUCTS_URL=s"https://osate-build.sei.cmu.edu/download/osate/stable/${osateVer}/products/"

val awas = Feature(
  name = "Awas",
  id ="org.sireum.aadl.osate.awas.feature.feature.group",
  updateSite = "https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/org.sireum.aadl.osate.awas.update.site",
  localUpdateSite = Some("file:///home/vagrant/devel/sireum/osate-plugin-update-site/org.sireum.aadl.osate.awas.update.site"))

val base = Feature(
  name="Base",
  updateSite = "https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/org.sireum.aadl.osate.update.site",
  localUpdateSite = Some("file:///home/vagrant/devel/sireum/osate-plugin-update-site/org.sireum.aadl.osate.update.site"),
  id = "org.sireum.aadl.osate.feature.feature.group")

val hamr = Feature(
  name="HAMR",
  updateSite ="https://raw.githubusercontent.com/sireum/hamr-plugin-update-site/master/org.sireum.aadl.osate.hamr.update.site",
  localUpdateSite = Some("file:///home/vagrant/devel/sireum/hamr-plugin-update-site/org.sireum.aadl.osate.hamr.update.site"),
  id = "org.sireum.aadl.osate.hamr.feature.feature.group")

val cli = Feature(
  name = "cli",
  updateSite = "https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/org.sireum.aadl.osate.update.site",
  localUpdateSite = Some("file:///home/vagrant/devel/sireum/osate-plugin-update-site/org.sireum.aadl.osate.update.site"),
  id = "org.sireum.aadl.osate.feature.feature.group")

//val order = ISZ(base, cli, hamr, awas)
val order = ISZ(base)

for(o <- order) {
  if (isInstalled(o.id, osateExe)) {
    uninstallPlugin(o, osateExe)
  }
  installPlugin(o, osateExe)
}

println(s"\n\nExecute the following to to launch OSATE :\n")
println(s"${osateExe}&")





@datatype class Feature(name: String,
                        id: String,
                        updateSite: String,
                        localUpdateSite: Option[String])


def isInstalled(featureId: String, osateExe: Os.Path): B = {
  val installedPlugins = Os.proc(ISZ(osateExe.string, "-nosplash", "-console", "-consoleLog", "-application", "org.eclipse.equinox.p2.director",
    "-listInstalledRoots")).at(osateExe.up).runCheck()
  return ops.StringOps(installedPlugins.out).contains(featureId)
}

def uninstallPlugin(feature: Feature, osateExe : Os.Path): Unit = {
  addInfo(s"Uninstalling ${feature.name} OSATE plugin")
  Os.proc(ISZ(osateExe.string, "-nosplash", "-console", "-consoleLog", "-application", "org.eclipse.equinox.p2.director",
    "-uninstallIU", feature.id
  )).at(osateExe.up).runCheck()
}

def installPlugin(feature: Feature, osateExe : Os.Path): Unit = {
  val updateSite = if(localUpdateSites) feature.localUpdateSite.get else feature.updateSite
  addInfo(s"Installing ${feature.name} OSATE plugin from ${updateSite}")

  Os.proc(ISZ(osateExe.string, "-nosplash", "-console", "-consoleLog", "-application", "org.eclipse.equinox.p2.director",
    "-repository", updateSite, "-installIU", feature.id
  )).at(osateExe.up).runCheck()
}

def addInfo(s: String): Unit = { cprintln(F, s ) }
def addError(s: String): Unit = { cprintln(T, s"Error: $s.") }
def addWarning(s: String): Unit = { cprintln(F, s"Warning: $s") }


