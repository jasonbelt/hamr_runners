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

val localUpdateSites: B = T

val phantomInstallDir: Os.Path = Os.home / "temp" / "phantom_plugin_test"

val AWAS_UPDATE_SITE="https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/org.sireum.aadl.osate.awas.update.site"
val AWAS_LOCAL_UPDATE_SITE="file:///home/vagrant/devel/sireum/osate-plugin-update-site/org.sireum.aadl.osate.awas.update.site"
val AWAS_FEATURE_ID="org.sireum.aadl.osate.awas.feature.feature.group"

val BASE_UPDATE_SITE="https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/org.sireum.aadl.osate.update.site"
val BASE_LOCAL_UPDATE_SITE="file:///home/vagrant/devel/sireum/osate-plugin-update-site/org.sireum.aadl.osate.update.site"
val BASE_FEATURE_ID="org.sireum.aadl.osate.feature.feature.group"

val CLI_UPDATE_SITE="https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/org.sireum.aadl.osate.cli.update.site"
val CLI_LOCAL_UPDATE_SITE="file:///home/vagrant/devel/sireum/osate-plugin-update-site/org.sireum.aadl.osate.cli.update.site"
val CLI_FEATURE_ID="org.sireum.aadl.osate.cli.feature.feature.group"

val HAMR_UPDATE_SITE="https://raw.githubusercontent.com/sireum/hamr-plugin-update-site/master/org.sireum.aadl.osate.hamr.update.site"
val HAMR_LOCAL_UPDATE_SITE="file:///home/vagrant/devel/sireum/hamr-plugin-update-site/org.sireum.aadl.osate.hamr.update.site"
val HAMR_FEATURE_ID="org.sireum.aadl.osate.hamr.feature.feature.group"

val osateVer= "2.9.1-vfinal"
val OSATE_PRODUCTS_URL=s"https://osate-build.sei.cmu.edu/download/osate/stable/${osateVer}/products/"

val (aus, bus, cus, hus): (String, String, String, String) =
  if(localUpdateSites) (AWAS_LOCAL_UPDATE_SITE, BASE_LOCAL_UPDATE_SITE, CLI_LOCAL_UPDATE_SITE, HAMR_LOCAL_UPDATE_SITE)
  else (AWAS_UPDATE_SITE, BASE_UPDATE_SITE, CLI_UPDATE_SITE, HAMR_UPDATE_SITE)

val (tgz, exeLoc) : (String, String) = Os.kind match {
  case Os.Kind.Linux =>
    (s"osate2-${osateVer}-linux.gtk.x86_64.tar.gz", "osate")
  case Os.Kind.Mac =>
    (s"osate2-${osateVer}-macosx.cocoa.x86_64.tar.gz", "osate2.app/Contents/MacOS/osate")
  case x => halt(s"not supporting ${x}")
}

val tgzPath = phantomInstallDir / tgz

if(!tgzPath.exists) {
  println(s"Fetching OSATE ${osateVer}")

  proc"wget -P ${phantomInstallDir} ${OSATE_PRODUCTS_URL}${tgz}".console.runCheck()
}
assert(tgzPath.exists, s"${tgz} doesn't exist")


val installDir = phantomInstallDir / s"osate_${osateVer}"

if(installDir.exists) {
  installDir.removeAll()
}
installDir.mkdirAll()
assert(installDir.exists, s"${installDir} doesn't exist")

proc"tar xvfz ${tgzPath} -C ${installDir}".console.runCheck()

println(s"Installing BASE plugin from ${bus}")
proc"${installDir}/${exeLoc} -nosplash -console -consoleLog -application org.eclipse.equinox.p2.director -repository ${bus} -installIU ${BASE_FEATURE_ID}".console.runCheck()

println(s"Installing CLI plugin from ${cus}")
proc"${installDir}/${exeLoc} -nosplash -console -consoleLog -application org.eclipse.equinox.p2.director -repository ${cus} -installIU ${CLI_FEATURE_ID}".console.runCheck()

//println(s"Installing HAMR plugin from ${hus}")
//proc"${installDir}/${exeLoc} -nosplash -console -consoleLog -application org.eclipse.equinox.p2.director -repository ${hus} -installIU ${HAMR_FEATURE_ID}".console.runCheck()

//println(s"Installing AWAS plugin from ${aus}")
//proc"${installDir}/${exeLoc} -nosplash -console -consoleLog -application org.eclipse.equinox.p2.director -repository ${aus} -installIU ${AWAS_FEATURE_ID}".console.runCheck()

println(s"\n\nalias phantom='${installDir}/osate -nosplash -console -consoleLog -application org.sireum.aadl.osate.cli'\n")