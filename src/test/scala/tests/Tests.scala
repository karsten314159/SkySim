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
      Program.main(Array(url, username, password))
      println("Actors asked: " + new Date())
      Thread.sleep(30000)
      println("Test done: " + new Date())
      true
    }
"""
}

