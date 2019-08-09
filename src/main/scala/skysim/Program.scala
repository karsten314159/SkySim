package skysim

import java.io.{FileInputStream, InputStreamReader}
import java.util.Properties

import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem, Props}
import skysim.CityActor.InitCity
import skysim.DBActor.{ExecSql, InitDb, SqlResult}
import skysim.JobActor.InitJobs
import skysim.Log._

import scala.io.StdIn
import scala.util.{Failure, Success, Try}

object ThenDo {

  class ActorX(f: => Unit) extends Actor {
    override def receive: Receive = {
      case _ =>
        val _: Unit = f
    }
  }

  class ActorY(f: Any => Unit) extends Actor {
    override def receive: Receive = {
      case x: Any =>
        f(x)
    }
  }

  def system(f: => Unit)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(new ActorX(f)))
  }

  def systemCallback(f: Any => Unit)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(new ActorY(f)))
  }

  def context(f: => Unit)(implicit system: ActorContext): ActorRef = {
    system.actorOf(Props(new ActorX(f)))
  }
}

object Program {
  def run(args: Array[String]): Unit = {
    println("Booting up actor system...")
    implicit val system = ActorSystem("skysim")

    val (url, username, password) = Try {
      println("Load props...")
      val prop = new Properties
      prop.load(new InputStreamReader(new FileInputStream("secret.properties")))

      (prop.getProperty("url"),
        prop.getProperty("username"),
        prop.getProperty("password"))
    } match {
      case Failure(x) =>
        println(x)

        (args(0), args(1), args(2))
      case Success(x) =>
        x
    }

    val db = system.actorOf(DBActor.props, "db")

    println("Booting up db...")

    db.tell(InitDb(username = username, password = password, url = url), ThenDo.system {

      println("Booting up job definitions...")

      val job = system.actorOf(JobActor.props, "jobs")
      job.tell(InitJobs(db), ThenDo.system {

        // job ! GetJob(42)

        /*Thread.sleep(10000)
      System.exit(1)*/

        println("Booting up city definitions...")

        db.tell(ExecSql("select * from skysim_cities"), ThenDo.systemCallback { case SqlResult(res, _) =>

          val citiesDef: List[(String, Int)] = res.toList.map(row =>
            row("name").toString -> row("population").toString.toInt
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

          Thread.sleep(1000)

          if (args.nonEmpty) {
            citiesImp foreach { c =>
              c.tell(Verb("step"), ThenDo.system {
                c ! Verb("save")
              })
            }

            db.tell(ExecSql("update skyrim_system set lastRun = " + System.currentTimeMillis), ThenDo.systemCallback { case SqlResult(_, a) =>
              println("last run set: " + a)
            })

            db.tell(ExecSql("select wait from skyrim_system"), ThenDo.systemCallback { case SqlResult(r, _) =>
              val wait = r.head("wait").toString.toInt
              Thread.sleep(wait)
            })

          } else {
            s"""
>>> Press ENTER to exit <<<
Or enter a command to be executed across: ${citiesDef.map(_._1).mkString(", ")}
Commands:
> sql:truncate table skysim
> breakfast
> save
> step
    """.split("\n").foreach(x => println(x))

            var line = ""
            try {
              while ( {
                line = StdIn.readLine()
                line != ""
              }) {
                // println("CMD: <" + line + ">, len " + line.length)
                if (line.startsWith("sql:")) {
                  db.tell(ExecSql(line.substring("sql:".length)), ThenDo.systemCallback {
                    case SqlResult(x, a) => println("sql:<command> done")
                  })
                } else {
                  citiesImp foreach { c =>
                    c ! Verb(line)
                  }
                }
              }
            }
            finally system.terminate
          }
        })
      })
    })
  }

  def main(args: Array[String]): Unit = {
    run(args)
  }
}
