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

case class CharState(city: String, name: String, state: String, x: Int, y: Int)

object CityActor {
  def props: Props = Props(new CityActor)

  case class InitCity(numCitizens: Int, db: ActorRef)

  case class ChangePos(ac: ActorRef, state: CharState)

  case class GetNearbyRes(other: ActorRef, dist: Double)

  case class GetNearby(requester: ActorRef)

}

class CityActor extends SimActor {
  var citizens: mutable.Map[ActorRef, CharState] = mutable.Map.empty
  var db: ActorRef = _

  override def receive: Receive = {
    case InitCity(size: Int, db: ActorRef) =>
      this.db = db

      println(s"init city " + this + " with " + size + " citizens")

      0.until(size) foreach { i =>
        val pos = (Random.nextInt(100), Random.nextInt(100))
        val name = "Cit" + i
        val c = context.actorOf(CharacterActor.props, name)
        this.citizens += c -> CharState(
          name, this.self.path.name, "idle", pos._1, pos._2
        )
      }

      this.citizens.zipWithIndex foreach { case ((cha, p), i) =>
        cha ! InitChar(i, this.self)
      }

      println(this + " done")

    case v@Verb("breakfast") =>
      citizens foreach { cha =>
        cha._1 ! v
      }

    case ChangePos(ac: ActorRef, p: CharState) =>
      citizens.update(ac, p)

    case Verb("save") =>
      this.db ! StoreDb(citizens.values.toSeq)

    case GetNearby(requester: ActorRef) =>
      val pos: CharState = citizens(requester)
      val (res: ActorRef, d: Double) =
        citizens map { case (other: ActorRef, p: CharState) =>
          if (other == requester)
            other -> Double.PositiveInfinity
          else {
            val dx = pos.x - p.x
            val dy = pos.y - p.y
            other -> (dy * dy + dx * dx).toDouble
          }
        } minBy { case (_, p: Double) => p }
      sender ! GetNearbyRes(res, Math.sqrt(d))

    case other => sys.error(this.self.path + " UNKNOWN <" + other + ">")
  }
}

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
    "Winterhold" -> 149,
    "Winterfell" -> 491,
    "Whiterun" -> 910
  )

  val citiesImp: List[ActorRef] =
    citiesDef.map { case (name, citizens) =>
      val city = system.actorOf(CityActor.props, name)
      city ! InitCity(citizens, db)
      city
    }

  citiesImp.head ! Verb("save")

  println(
    s"""
>>> Press ENTER to exit <<<
Or enter a command to be executed across: ${citiesDef.map(_._1).mkString(", ")}
Commands: breakfast
    """)
  var line = ""
  try {
    while ( {
      line = StdIn.readLine(">")
      line != ""
    }) {
      // println("CMD: <" + line + ">, len " + line.length)
      citiesImp foreach { c =>
        c ! Verb(line)
      }
    }
  }
  finally system.terminate
}

