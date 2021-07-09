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
import Templates._

@enum object Matches {
  "GreaterOrEqual"
}

@datatype class Require(pluginId: String,
                        version: Option[String],
                        matches: Option[Matches.Type]
                       )

@datatype class Feature (productId: String,
                         featureId: String,
                         version: String,

                         productName: String,
                         productDescription: String,
                         providerName: String,

                         requires: ISZ[Require],

                         updateSiteName: String,
                         directory: Os.Path
                         )

val launcher = Os.home / "devel/osate/osate2_2021-03-master/eclipse/plugins/org.eclipse.equinox.launcher_1.6.100.v20201223-0822.jar"
val osate_plugin_dir = Os.home / "devel/sireum/osate-plugin/"
val dest = Os.home / "devel/sireum/osate-update-site"

val features: ISZ[Feature] = ISZ("osate", "osate.cli", "osate.hamr").map((m: String) =>
  osate_plugin_dir / s"org.sireum.aadl.${m}.feature").map((p: Os.Path) => parse(p))

val workspace = Os.home / "devel/osate/osate2_2021-03-master/ws"

val qualifier = "happy"
val tag = "v1"

val outdir = dest / tag
(outdir / "compositeContent.xml").writeOver(compositeContent.render)
(outdir / "compositeArtifacts.xml").writeOver(compositeArtifacts.render)

for(f <- features) {
  println(f)
  val buildFile =  f.directory / "build.xml"

  val destDir = outdir / f.productId
  buildFile.writeOver(build_xml(destDir, f.featureId, qualifier).render)
  assert(buildFile.exists, buildFile.canon.value)

  proc"java -cp ${launcher} org.eclipse.core.launcher.Main -application org.eclipse.ant.core.antRunner -data ${workspace.value} -buildfile ${buildFile.canon.value}".console.runCheck()

  val featuresDir = destDir / "features"
  val pluginsDir = destDir / "plugins"

  assert(featuresDir.list.size == 1, s"hmm ${featuresDir.list.size}")
  assert(pluginsDir.list.size == 1, s"hmm ${pluginsDir.list.size}")

  val featureJarFile = featuresDir.list(0)
  val pluginJarFile = pluginsDir.list(0)

  val jarName = ops.StringOps(featureJarFile.name)
  val version = jarName.substring(jarName.lastIndexOf('_') + 1, jarName.lastIndexOf('.'))
  val site_xml_contents = site_xml(f.updateSiteName, featureJarFile.name, f.featureId, version)

  (destDir / "site.xml").writeOver(site_xml_contents.render)

  (destDir / "artifacts.xml").writeOver(artifacts_xml(f.productId, f.featureId, version, pluginJarFile.length, featureJarFile.length).render)

  (destDir / "content.xml").writeOver(content_xml(f.productId, f.featureId, f.productName, f.productDescription,
    f.providerName, version, f.requires).render)

  println(s"Successfully built: ${f.featureId}")
}


object Templates {

  def getQuote(str_ : String): String = {
    val str = ops.StringOps(ops.StringOps(str_).trim)
    return str.substring(str.indexOf('"') + 1, str.lastIndexOf('"'))
  }

  def parseImport(str : ops.StringOps): Require = {
    val pluginPos = str.stringIndexOf("plugin=")
    val plugin = str.substring(pluginPos + 8, str.indexOfFrom('"', pluginPos + 9))
    var version: Option[String] = None()
    var matches: Option[Matches.Type] = None()

    val versionPos = str.stringIndexOf("version=")
    if(versionPos > 0) {
      version = Some(str.substring(versionPos + 9, str.indexOfFrom('"', versionPos + 10)))
    }
    val matchPos = str.stringIndexOf("match=")
    if(matchPos > 0) {
      val m = str.substring(matchPos + 7, str.indexOfFrom('"', matchPos + 8))
      m match {
        case "greaterOrEqual" => matches = Some(Matches.GreaterOrEqual)
        case x => halt(x)
      }
    }
    return Require(plugin, version, matches)
  }

  def parse(featureDir: Os.Path): Feature = {
    val featureXml = featureDir / "feature.xml"
    assert(featureXml.exists, featureXml.canon.value)

    val lines = featureXml.readLines

    var productId: String = ""
    var featureId: String = ""
    var productName: String = ""
    var productDescription: String = ""
    var version: String = ""
    var providerName: String = ""
    var requires: ISZ[Require]= ISZ()
    var i = 0
    while (i < lines.size) {
      var line = ops.StringOps(lines(i))
      if(line.startsWith("<feature")) {
        featureId = getQuote(lines(i + 1))
        productName = getQuote(lines(i + 2))
        version = getQuote(lines(i + 3))
        providerName = getQuote(lines(i + 4))
      }

      if(line.contains("<description")) {
        i = i + 1
        while (!ops.StringOps(lines(i)).contains("</description>")) {
          productDescription = st"""${productDescription}
                                   |${lines(i)}""".render
          i = i + 1
        }
      }

      if(line.contains("<requires>")) {
        i = i  + 1
        while(ops.StringOps(lines(i)).contains("<import")) {
          requires = requires :+ parseImport(ops.StringOps(lines(i)))
          i = i  + 1
        }
      }

      if(line.contains("<plugin")) {
        productId = getQuote(lines(i + 1))
      }

      i = i + 1
    }

    return Feature(productId, featureId, version, productName, ops.StringOps(productDescription).trim, providerName, requires, productId, featureDir)
  }

  def site_xml(updateSiteName: String,
               jarFile: String,
               featureId: String,
               featureVersion: String): ST = {
    val ret = st"""<?xml version="1.0" encoding="UTF-8"?>
                  |<site>
                  |   <description name="${updateSiteName}">
                  |      ${updateSiteName}
                  |   </description>
                  |   <feature url="features/${jarFile}" id="${featureId}" version="${featureVersion}">
                  |      <category name="org.sireum.aadl.osate.category"/>
                  |   </feature>
                  |   <category-def name="org.sireum.aadl.osate.category" label="Sireum">
                  |      <description>
                  |         Sireum features
                  |      </description>
                  |   </category-def>
                  |</site>
                  |"""
    return ret
  }

  def content_xml(productId: String,
                  featureId: String,
                  productName: String,
                  productDescription: String,
                  provider: String,
                  version: String,
                  requires_ : ISZ[Require]): ST = {

    val requires: ISZ[(ST, ST)] = requires_.map(r => {
      val range: String =
        if (r.matches.nonEmpty) r.version.get
        else "0.0.0"
      (st"""<required namespace='osgi.bundle' name='${r.pluginId}' range='${range}'/>""",
        st"""<required namespace='org.eclipse.equinox.p2.i' name='${r.pluginId}' range='${range}'/>""")
    })

    val ret = st"""<?xml version='1.0' encoding='UTF-8'?>
                  |<?metadataRepository version='1.2.0'?>
                  |<repository name='Exported Repository' type='org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository' version='1'>
                  |  <properties size='2'>
                  |    <property name='p2.timestamp' value='1625783166725'/>
                  |    <property name='p2.compressed' value='true'/>
                  |  </properties>
                  |  <units size='3'>
                  |    <unit id='${productId}' version='${version}'>
                  |      <update id='${productId}' range='[0.0.0,${version})' severity='0'/>
                  |      <properties size='1'>
                  |        <property name='org.eclipse.equinox.p2.name' value='${productDescription}'/>
                  |      </properties>
                  |      <provides size='4'>
                  |        <provided namespace='org.eclipse.equinox.p2.iu' name='${productId}' version='${version}'/>
                  |        <provided namespace='osgi.bundle' name='${productId}' version='${version}'/>
                  |        <provided namespace='osgi.identity' name='${productId}' version='${version}'>
                  |          <properties size='1'>
                  |            <property name='type' value='osgi.bundle'/>
                  |          </properties>
                  |        </provided>
                  |        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' version='1.0.0'/>
                  |      </provides>
                  |      <requires size='${requires.size}'>
                  |        ${(requires.map(m => m._1), "\n")}
                  |      </requires>
                  |      <artifacts size='1'>
                  |        <artifact classifier='osgi.bundle' id='${productId}' version='${version}'/>
                  |      </artifacts>
                  |      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
                  |      <touchpointData size='1'>
                  |        <instructions size='1'>
                  |          <instruction key='manifest'>
                  |            Bundle-SymbolicName: ${productId};singleton:=true&#xA;Bundle-Version: ${version}
                  |          </instruction>
                  |        </instructions>
                  |      </touchpointData>
                  |    </unit>
                  |    <unit id='${featureId}.feature.jar' version='${version}'>
                  |      <properties size='3'>
                  |        <property name='org.eclipse.equinox.p2.name' value='${productName}'/>
                  |        <property name='org.eclipse.equinox.p2.description' value='${productDescription}'/>
                  |        <property name='org.eclipse.equinox.p2.provider' value='${provider}'/>
                  |      </properties>
                  |      <provides size='3'>
                  |        <provided namespace='org.eclipse.equinox.p2.iu' name='${featureId}.feature.jar' version='${version}'/>
                  |        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='feature' version='1.0.0'/>
                  |        <provided namespace='org.eclipse.update.feature' name='${featureId}' version='${version}'/>
                  |      </provides>
                  |      <filter>
                  |        (org.eclipse.update.install.features=true)
                  |      </filter>
                  |      <artifacts size='1'>
                  |        <artifact classifier='org.eclipse.update.feature' id='${featureId}' version='${version}'/>
                  |      </artifacts>
                  |      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
                  |      <touchpointData size='1'>
                  |        <instructions size='1'>
                  |          <instruction key='zipped'>
                  |            true
                  |          </instruction>
                  |        </instructions>
                  |      </touchpointData>
                  |      <licenses size='1'>
                  |        <license uri='http://www.example.com/license' url='http://www.example.com/license'>
                  |          Copyright (c) 2017-2021, Kansas State University&#xA;All rights reserved.&#xA;&#xA;Redistribution and use in source and binary forms, with or without&#xA;modification, are permitted provided that the following conditions are met:&#xA;&#xA;1. Redistributions of source code must retain the above copyright notice, this&#xA;   list of conditions and the following disclaimer.&#xA;2. Redistributions in binary form must reproduce the above copyright notice,&#xA;   this list of conditions and the following disclaimer in the documentation&#xA;   and/or other materials provided with the distribution.&#xA;&#xA;THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS &quot;AS IS&quot; AND&#xA;ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED&#xA;WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE&#xA;DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR&#xA;ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES&#xA;(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;&#xA;LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND&#xA;ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT&#xA;(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS&#xA;SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
                  |        </license>
                  |      </licenses>
                  |    </unit>
                  |    <unit id='${featureId}.feature.group' version='${version}' singleton='false'>
                  |      <update id='${featureId}.feature.group' range='[0.0.0,${version})' severity='0'/>
                  |      <properties size='4'>
                  |        <property name='org.eclipse.equinox.p2.name' value='${productName}'/>
                  |        <property name='org.eclipse.equinox.p2.description' value='${productDescription}'/>
                  |        <property name='org.eclipse.equinox.p2.provider' value='${provider}'/>
                  |        <property name='org.eclipse.equinox.p2.type.group' value='true'/>
                  |      </properties>
                  |      <provides size='1'>
                  |        <provided namespace='org.eclipse.equinox.p2.iu' name='${featureId}.feature.group' version='${version}'/>
                  |      </provides>
                  |      <requires size='${requires.size}'>
                  |        ${(requires.map(m => m._2), "\n")}
                  |          <filter>
                  |            (org.eclipse.update.install.features=true)
                  |          </filter>
                  |      </requires>
                  |      <touchpoint id='null' version='0.0.0'/>
                  |      <licenses size='1'>
                  |        <license uri='http://www.example.com/license' url='http://www.example.com/license'>
                  |          Copyright (c) 2017-2021, Kansas State University&#xA;All rights reserved.&#xA;&#xA;Redistribution and use in source and binary forms, with or without&#xA;modification, are permitted provided that the following conditions are met:&#xA;&#xA;1. Redistributions of source code must retain the above copyright notice, this&#xA;   list of conditions and the following disclaimer.&#xA;2. Redistributions in binary form must reproduce the above copyright notice,&#xA;   this list of conditions and the following disclaimer in the documentation&#xA;   and/or other materials provided with the distribution.&#xA;&#xA;THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS &quot;AS IS&quot; AND&#xA;ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED&#xA;WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE&#xA;DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR&#xA;ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES&#xA;(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;&#xA;LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND&#xA;ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT&#xA;(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS&#xA;SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
                  |        </license>
                  |      </licenses>
                  |    </unit>
                  |  </units>
                  |</repository>"""
    return ret
  }

  def artifacts_xml(productId: String,
                    featureId: String,
                    version: String,
                    pluginSize: Z,
                    featureSize: Z): ST = {
    val ret = st"""<?xml version='1.0' encoding='UTF-8'?>
                  |<?artifactRepository version='1.1.0'?>
                  |<repository name='Exported Repository' type='org.eclipse.equinox.p2.artifact.repository.simpleRepository' version='1'>
                  |  <properties size='2'>
                  |    <property name='p2.timestamp' value='1625783166724'/>
                  |    <property name='p2.compressed' value='true'/>
                  |  </properties>
                  |  <mappings size='3'>
                  |    <rule filter='(&amp; (classifier=osgi.bundle))' output='$${repoUrl}/plugins/$${id}_$${version}.jar'/>
                  |    <rule filter='(&amp; (classifier=binary))' output='$${repoUrl}/binary/$${id}_$${version}'/>
                  |    <rule filter='(&amp; (classifier=org.eclipse.update.feature))' output='$${repoUrl}/features/$${id}_$${version}.jar'/>
                  |  </mappings>
                  |  <artifacts size='2'>
                  |    <artifact classifier='osgi.bundle' id='${productId}' version='${version}'>
                  |      <properties size='1'>
                  |        <property name='download.size' value='${pluginSize}'/>
                  |      </properties>
                  |    </artifact>
                  |    <artifact classifier='org.eclipse.update.feature' id='${featureId}' version='${version}'>
                  |      <properties size='2'>
                  |        <property name='download.contentType' value='application/zip'/>
                  |        <property name='download.size' value='${featureSize}'/>
                  |      </properties>
                  |    </artifact>
                  |  </artifacts>
                  |</repository>"""
    return ret
  }

  def build_xml(destDir: Os.Path,
        featureId: String,
        qualifier: String): ST = {

    val ret: ST =
      st"""<?xml version="1.0" encoding="UTF-8"?>
          |<project default="feature_export" name="build">
          |	<target name="feature_export">
          |		<pde.exportFeatures destination="${destDir.canon.value}"
          |                       features="${featureId}"
          |                       qualifier="${qualifier}"
          |                       exportSource="false" exportType="directory" useJARFormat="true"/>
          |	</target>
          |</project>
          |"""
    return ret
  }

  val compositeArtifacts: ST = st"""<?xml version='1.0' encoding='UTF-8'?>
                                   |<?compositeArtifactRepository version='1.0.0'?>
                                   |<repository name='Sireum OSATE Update Site'
                                   |    type='org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository' version='1.0.0'>
                                   |  <properties size='1'>
                                   |    <property name='p2.timestamp' value='1243822502440'/>
                                   |  </properties>
                                   |  <children size='3'>
                                   |    <child location='base'/>
                                   |    <child location='cli'/>
                                   |    <child location='hamr'/>
                                   |  </children>
                                   |</repository>
                                   |"""

  val compositeContent: ST = st"""<?xml version='1.0' encoding='UTF-8'?>
                                 |<?compositeMetadataRepository version='1.0.0'?>
                                 |<repository name='Sireum OSATE Update Site'
                                 |    type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository' version='1.0.0'>
                                 |  <properties size='1'>
                                 |    <property name='p2.timestamp' value='1243822502499'/>
                                 |  </properties>
                                 |  <children size='3'>
                                 |    <child location='base'/>
                                 |    <child location='cli'/>
                                 |    <child location='hamr'/>
                                 |  </children>
                                 |</repository>
                                 |"""
}