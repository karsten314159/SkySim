package skysim

import akka.event.Logging
import akka.event.Logging.LogLevel
import akka.event.jul.Logger
import sun.misc.{JavaLangAccess, SharedSecrets}

object Log {
  final val dev: Boolean = false

  def println(msg: Any): Unit = {
    if (dev) {
      Predef.println("[INFO] " + msg)
    } else {
      val t: Throwable = new Throwable
      val (className, meth, line) = getCaller(t)
      val end = className.indexOf("$")
      val c =
        if (end == -1)
          className.substring(className.indexOf(".") + 1)
        else
          className.substring(className.indexOf(".") + 1, end)
      Logger.root.logp(java.util.logging.Level.INFO, c, ":" + line,
        String.valueOf(msg))
    }
  }

  def getCaller(throwable: Throwable): (String, String, Int) = {
    val access: JavaLangAccess = SharedSecrets.getJavaLangAccess

    val depth: Int = access.getStackTraceDepth(throwable)

    for (ix <- 1.until(depth)) {
      // Calling getStackTraceElement directly prevents the VM
      // from paying the cost of building the entire stack frame.
      val frame = access.getStackTraceElement(throwable, ix)
      val cname = frame.getClassName
      return (cname, frame.getMethodName, frame.getLineNumber)
    }
    ("", "", 0)
  }
}