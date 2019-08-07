package skysim

import java.io.{FileInputStream, InputStreamReader}
import java.util.Properties

import akka.actor.{ActorRef, ActorSystem, Props}
import skysim.CharacterActor.InitChar
import skysim.CityActor.{ChangePos, GetNearby, GetNearbyRes, InitCity}
import skysim.DBActor.{InitDb, StoreDb}

import scala.collection.mutable
import scala.io.StdIn
import scala.util.Random


object Program extends App {
  println("Booting up actor system...")
  val system = ActorSystem("skysim")

  val prop = new Properties
  prop.load(new InputStreamReader(new FileInputStream("secret.properties")))

  val db = system.actorOf(DBActor.props, "db")

  println("Booting up db...")

  db ! InitDb(
    username = prop.getProperty("username"),
    password = prop.getProperty("password"),
    url = prop.getProperty("url")
  )

  /*Thread.sleep(10000)
  System.exit(1)*/

  println("Booting up cities...")

  val citiesDef: List[(String, Int)] = List(
    "Riften" -> 450,
    "Windhelm" -> 450,
    "Whiterun" -> 450,
    "Markarth" -> 450,
    "Solitude" -> 450,
    "Morthal" -> 450,
    "Dawnstar" -> 450,
    "Winterhold" -> 450
  )

  val citiesImp: List[ActorRef] =
    citiesDef.map { case (name, citizens) =>
      val city = system.actorOf(CityActor.props, name)
      city ! InitCity(citizens, db)
      city
    }


  /*if (firstRun) {
    citiesImp foreach { x => x ! Verb("save") }
  }*/

  println(
    s"""
>>> Press ENTER to exit <<<
Or enter a command to be executed across: ${citiesDef.map(_._1).mkString(", ")}
Commands:
> sql:truncate table skysim
> breakfast
> save
    """)
  var line = ""
  try {
    while ( {
      line = StdIn.readLine("> ")
      line != ""
    }) {
      // println("CMD: <" + line + ">, len " + line.length)
      if (line.startsWith("sql:")) {
        db ! Verb(line.substring("sql:".length))
      } else {
        citiesImp foreach { c =>
          c ! Verb(line)
        }
      }
    }
  }
  finally system.terminate
}
