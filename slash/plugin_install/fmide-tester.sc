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

val SIREUM_HOME = Os.path(Os.env("SIREUM_HOME").get)
val sireum = SIREUM_HOME / "bin" / "sireum"
assert(sireum.exists, "Where's sireum?")

val fmideInstall = SIREUM_HOME / "bin" / "install" / "fmide.cmd"
assert(fmideInstall.exists, "Missing fmide installer")

val AWAS_UPDATE_SITE="https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/"
//val AWAS_UPDATE_SITE="file:///home/vagrant/devel/sireum/osate-plugin-update-site/org.sireum.aadl.osate.awas.update.site"
val AWAS_FEATURE_ID="org.sireum.aadl.osate.awas.feature.feature.group"

def normal(): Unit = {
  println("Running FMIDE install script")
  proc"$sireum slang run ${fmideInstall}".console.runCheck()

  val exeLoc = Os.kind match {
    case Os.Kind.Linux => SIREUM_HOME / "bin" / "linux" / "fmide" / "fmide"
    case Os.Kind.Mac => SIREUM_HOME / "bin" / "mac" / "fmide.app" / "Contents" / "MacOS" / "fmide"
    case x => halt(s"not supporting ${x}")
  }
  assert(exeLoc.exists, s"${exeLoc} doesn't exist")

  println("Installing AWAS plugin")
  proc"${exeLoc} -nosplash -console -consoleLog -application org.eclipse.equinox.p2.director -repository ${AWAS_UPDATE_SITE} -installIU ${AWAS_FEATURE_ID}".console.runCheck()

  println(s"\n\nExecute the following to to launch FMIDE:\n")
  println(s"${exeLoc}&")
}

def abnormal(): Unit = {
  val timestamp = "202101050141"
  val (ver, exeLoc): (String, String) = Os.kind match {
    case Os.Kind.Linux => (s"${timestamp}-linux.gtk.x86_64", "fmide")
    case Os.Kind.Mac => (s"${timestamp}-macosx.cocoa.x86_64", "com.collins.trustedsystems.fmw.ide.app/Contents/MacOS/fmide")
    case x => halt(s"Not handling ${x}")
  }

  val TGZ = Os.cwd / s"com.collins.trustedsystems.fmw.ide-2.4.0-${ver}.tar.gz"
  val FMIDE_URL = s"https://github.com/loonwerks/formal-methods-workbench/releases/download/untagged-b6b5ab038c0bb0bf9bf1/${TGZ.name}"

  if(!TGZ.exists) {
    println(s"Fetching FMIDE ${ver}")

    proc"wget ${FMIDE_URL}".console.runCheck()
  }
  assert(TGZ.exists, s"${TGZ} doesn't exist")

  val installDir = Os.cwd / "fmide_abnormal"

  if(installDir.exists) {
    installDir.removeAll()
  }
  installDir.mkdirAll()
  assert(installDir.exists, s"${installDir} doesn't exist")

  proc"tar xvfz ${TGZ} -C ${installDir}".console.runCheck()

  val HAMR_UPDATE_SITE="https://raw.githubusercontent.com/sireum/hamr-plugin-update-site/master/"
  val HAMR_FEATURE_ID="org.sireum.aadl.osate.hamr.feature.feature.group"

  println("Updating HAMR plugin")
  proc"${installDir}/${exeLoc} -nosplash -console -consoleLog -application org.eclipse.equinox.p2.director -repository ${HAMR_UPDATE_SITE} -installIU ${HAMR_FEATURE_ID} -uninstallIU ${HAMR_FEATURE_ID}".console.runCheck()

  println("Installing AWAS plugin")
  proc"${installDir}/${exeLoc} -nosplash -console -consoleLog -application org.eclipse.equinox.p2.director -repository ${AWAS_UPDATE_SITE} -installIU ${AWAS_FEATURE_ID}".console.runCheck()

  println(s"\n\nExecute the following to to launch FMIDE ${ver}:\n")
  println(s"${installDir}/${exeLoc}&")
}

//normal()
abnormal()
