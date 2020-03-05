package org.sireum.cli.hamr_runners.misc

import java.io._
import java.security.Permission

import org.sireum.$internal

object Test extends App {
  val sireumVersion = $internal.Macro.version

  println(s"macro version: $sireumVersion")

  val origOut = System.out
  val origSecMan = System.getSecurityManager

  val baoout = new ByteArrayOutputStream()

  // handle Sireum's System.exit
  val noExitSecMan = new SecurityManager {
    override def checkExit(status: Int): Unit = { throw new SecurityException() }
    override def checkPermission(perm: Permission): Unit = {}
  }

  System.setSecurityManager(noExitSecMan)

  scala.Console.withOut(baoout) {
    try {
      val c = Class.forName("org.sireum.Sireum")
      val m = c.getMethod("main", classOf[Array[String]])
      m.invoke(null, Seq[String]().toArray)
    } catch {
      case x: Throwable => // ingore
    }
  }

  System.setSecurityManager(origSecMan)
  System.setOut(origOut)

  val r = baoout.toString()
  val x = r.split("\n")

  if(x.size > 3) {
    println(x(2))
  }

  println(s"I got '$r''")
}