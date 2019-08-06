package skysim

import java.util.Scanner

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.collection.mutable
import scala.io.StdIn
import scala.util.Random


object CityRef {
  def props: Props = Props(new CityRef)
}

class CityRef extends SimActor {
  var allcit: mutable.Map[ActorRef, (Int, Int)] = mutable.Map.empty

  override def receive: Receive = {
    case ("init", size: Int) =>
      println(s"init city " + this + " with " + size + " citizens")

      0.until(size) foreach { i =>
        val pos = (Random.nextInt(100), Random.nextInt(100))
        val c = context.actorOf(CharacterRef.props, "cit" + i)
        this.allcit += c -> pos
      }

      this.allcit.zipWithIndex foreach { case ((cha, p), i) =>
        cha ! ("init", this.self, p)
      }

      println(this + " done")

    case "breakfast" =>
      allcit.zipWithIndex foreach { case (cha, i) =>
        cha._1 ! "breakfast"
      }

    case ("getNearby", requester, pos: (Int, Int)) =>
      val res = allcit minBy { case (other, p) =>
        if (other == requester)
          Double.PositiveInfinity
        else {
          val dx = pos._1 - p._1
          val dy = pos._2 - p._2
          dy * dy - dx * dx
        }
      }
      sender() ! "getNearbyRes" -> res._1

    case other => sys.error(this.self.path + " UNKNOWN <" + other + ">")
  }
}

object Program extends App {
  val system = ActorSystem("skysim")

  val citiesDef: List[(String, Int)] = List(
    "winterhold" -> 149,
    "winterfell" -> 491,
    "whiterun" -> 910
  )

  val citiesImp: List[ActorRef] =
    citiesDef.map { case (name, cit) =>
      val city = system.actorOf(CityRef.props, name)
      city ! ("init", cit)
      city
    }

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
        c ! line
      }
    }
  }
  finally system.terminate
}