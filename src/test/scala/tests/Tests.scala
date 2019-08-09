package tests

import java.util.Date

import org.specs2._
import skysim.Program

class Tests extends Specification {
  def is =
    s2"""
 Run Program
   ${
      println("Test started: " + new Date())
      val url = System.getenv("url")
      val username = System.getenv("username")
      val password = System.getenv("password")

      assert(url != null, "systemenv url not set")
      assert(username != null, "systemenv username not set")
      assert(password != null, "systemenv password not set")

      Program.main(Array(url, username, password))

      println("Test done: " + new Date())
      true
    }
"""
}

