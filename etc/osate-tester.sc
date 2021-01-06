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

val AWAS_UPDATE_SITE="https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/"
val AWAS_FEATURE_ID="org.sireum.aadl.osate.awas.feature.feature.group"

val HAMR_UPDATE_SITE="https://raw.githubusercontent.com/sireum/hamr-plugin-update-site/master/"
val HAMR_FEATURE_ID="org.sireum.aadl.osate.hamr.feature.feature.group"

val osateVer= "2.9.0-vfinal"
val OSATE_PRODUCTS_URL=s"https://osate-build.sei.cmu.edu/download/osate/stable/${osateVer}/products/"

val (tgz, exeLoc) : (String, String) = Os.kind match {
  case Os.Kind.Linux =>
    (s"osate2-${osateVer}-linux.gtk.x86_64.tar.gz", "osate")
  case Os.Kind.Mac =>
    (s"osate2-${osateVer}-macosx.cocoa.x86_64.tar.gz", "osate2.app/Contents/MacOS/osate")
  case x => halt(s"not supporting ${x}")
}

val tgzPath = Os.cwd / tgz

if(!tgzPath.exists) {
  println(s"Fetching OSATE ${osateVer}")

  proc"wget ${OSATE_PRODUCTS_URL}${tgz}".console.runCheck()
}
assert(tgzPath.exists, s"${tgz} doesn't exist")


val installDir = Os.cwd / osateVer

if(installDir.exists) {
  installDir.removeAll()
}
installDir.mkdirAll()
assert(installDir.exists, s"${installDir} doesn't exist")

proc"tar xvfz ${tgzPath} -C ${installDir}".console.runCheck()

println("Installing HAMR plugin")
proc"${installDir}/${exeLoc} -nosplash -console -consoleLog -application org.eclipse.equinox.p2.director -repository ${HAMR_UPDATE_SITE} -installIU ${HAMR_FEATURE_ID}".console.runCheck()

println("Installing AWAS plugin")
proc"${installDir}/${exeLoc} -nosplash -console -consoleLog -application org.eclipse.equinox.p2.director -repository ${AWAS_UPDATE_SITE} -installIU ${AWAS_FEATURE_ID}".console.runCheck()

println(s"\n\nExecute the following to to launch OSATE ${osateVer}:\n")
println(s"${installDir}/${exeLoc}&")