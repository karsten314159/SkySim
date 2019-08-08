package skysim

import java.io.{FileInputStream, InputStreamReader}
import java.util.Properties

import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem, Props}
import skysim.CityActor.InitCity
import skysim.DBActor.{ExecSql, InitDb}
import skysim.JobActor.InitJobs
import skysim.Log._

import scala.io.StdIn

object ThenDo {

  class ActorX(f: => Unit) extends Actor {
    override def receive: Receive = {
      case _ =>
        val _: Unit = f
    }
  }

  def system(f: => Unit)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(new ActorX(f)))
  }

  def context(f: => Unit)(implicit system: ActorContext): ActorRef = {
    system.actorOf(Props(new ActorX(f)))
  }
}

object Program extends App {
  println("Booting up actor system...")
  implicit val system = ActorSystem("skysim")

  val prop = new Properties
  prop.load(new InputStreamReader(new FileInputStream("secret.properties")))

  val db = system.actorOf(DBActor.props, "db")

  println("Booting up db...")

  db.tell(InitDb(
    username = prop.getProperty("username"),
    password = prop.getProperty("password"),
    url = prop.getProperty("url")
  ), ThenDo.system {
    val job = system.actorOf(JobActor.props, "jobs")
    job.tell(InitJobs(db), ThenDo.system {

      // job ! GetJob(42)

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
          city ! InitCity(citizens, db, job)
          city
        }

      /*if (firstRun) {
      citiesImp foreach { x => x ! Verb("save") }
    }*/

      s"""
>>> Press ENTER to exit <<<
Or enter a command to be executed across: ${citiesDef.map(_._1).mkString(", ")}
Commands:
> sql:truncate table skysim
> breakfast
> save
    """.split("\n").foreach(x => println(x))

      Thread.sleep(1000)

      var line = ""
      try {
        while ( {
          line = StdIn.readLine("> ")
          line != ""
        }) {
          // println("CMD: <" + line + ">, len " + line.length)
          if (line.startsWith("sql:")) {
            db ! ExecSql(line.substring("sql:".length))
          } else {
            citiesImp foreach { c =>
              c ! Verb(line)
            }
          }
        }
      }
      finally system.terminate
    })
  })
}
