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

def usage(): Unit = {
  println("GitHub Table Of Contents Generator")
  println("")
  println("Usage: <option>* file-or-directory")
  println("")
  println("Available Options")
  println("--start-at-level\t(default is 1)")
}

if(Os.cliArgs.isEmpty || Os.cliArgs.size > 4) {
  usage()
  Os.exit(0)
}

val fileOrDir = Os.path(Os.cliArgs(Os.cliArgs.size - 1))
if(!fileOrDir.isDir && !fileOrDir.isFile){
  println(s"$fileOrDir must be a file or a directory")
  println()
  usage()
  Os.exit(0)
}

val pos = ops.ISZOps(Os.cliArgs).indexOf("--start-at-level")
val startAtLevel: Z = if(pos < Os.cliArgs.size) {
  Z(Os.cliArgs(pos + 1)).get
} else {
  1
}

@datatype class Entry(level: Z, text: String, link: String)
val TC_START="<!--ts-->"
val TC_END="<!--te-->"

def processReadme(f: Os.Path): Unit = {
  assert(f.isFile, s"$f is not a file")

  var tcStart: Z = -1
  var tcEnd: Z = -1

  val contents = f.readLines
  var entries: ISZ[Entry] = ISZ()

  for(i <- 0 until contents.size) {
    val o = ops.StringOps(contents(i))
    if(o.contains(TC_START)) {
      assert(tcStart == -1, s"$TC_START already defined at $tcStart")
      tcStart = i
    }
    if(o.contains(TC_END)) {
      assert(tcStart > -1, s"$TC_START must appear before $TC_END")
      tcEnd = i
    }
    if(o.startsWith("#")) {
      entries = entries :+ toEntry(o)
    }
  }

  if(tcStart == -1 || tcEnd == -1) {
    halt(s"Didn't find $TC_START and/or $TC_END")
  }

  if(entries.size > 0) {
    //println(entries)
    var toc = st""
    for (e <- entries) {
      if (e.level >= startAtLevel) {
        val spaces = expand(e.level - startAtLevel, ' ')
        toc =
          st"""$toc
              |${spaces}* ${createHyperLink(e.text, e.link)}"""
      }
    }

    val oo = ops.ISZOps(contents)
    val start: ST = ops.ISZOps(oo.slice(0, tcStart + 1)).foldLeft((a: ST, b: String) =>
      st"$a$b\n", st"")
    val end: ST = ops.ISZOps(oo.slice(tcEnd, contents.size)).foldLeft((a: ST, b: String) =>
      st"$a$b\n", st"")

    // remove last newline from start as the following adds a newline b/w start and toc
    val ostart = ops.StringOps(start.render)
    val combined =
      st"""${ostart.substring(0, ostart.size - 1)}
          |${trim(toc)}
          |${end}"""

    f.writeOver(combined.render)
    println(s"Wrote: ${f.canon}")
  }
}


def replaceAll(s: String, from: String, to: String): String = {
  return ops.StringOps(s).replaceAllLiterally(from, to)
}

def toLowerCase(s: String): String = {
  val cms = conversions.String.toCms(s)
  return conversions.String.fromCms(cms.map((c: C) => ops.COps(c).toLower))
}

def createTag(s: String): String = {
  val lc = toLowerCase(s)
  val d = replaceAll(lc, " ", "-")
  val cis = conversions.String.toCis(d)

  // only keep numbers, lowercase letters, '-' and '_'
  val cis_ = cis.filter(c =>
    (c.value >= 48 && c.value <= 57) || (c.value >= 97 && c.value <= 122) ||
      (c == '-') || (c == '_'))
  val d_ = conversions.String.fromCis(cis_)
  return d_
}

def toEntry(o: ops.StringOps): Entry = {
  val cis = conversions.String.toCis(o.s)
  var pounds = 0
  while(cis(pounds) == '#'){
    pounds = pounds + 1
  }
  val text = ops.StringOps(o.substring(pounds, o.size)).trim
  val tag = createTag(text)
  return Entry(pounds, text, s"#$tag")
}

def trim(st: ST): ST = {
  return st"${ops.StringOps(st.render).trim}"
}

if(fileOrDir.isFile) {
  processReadme(fileOrDir)
}

def expand(count: Z, c: C) : String = {
  var ret: ST = st""
  for(i <- 0 until count*2) { ret = st"${ret}${c}" }
  return ret.render
}

def createHyperLink(title: String, target: String): ST = {
  return st"[${title}](${target})"
}