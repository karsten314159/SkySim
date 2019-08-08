package skysim

import akka.actor.{ActorRef, Props}
import skysim.DBActor.{ExecSql, SqlResult}
import skysim.JobActor.{GetJob, InitJobs, InitJobsDone, ReceiveJob}

import scala.util.Random

class JobActor extends SimActor {
  var jobs: Map[String, List[String]] = _
  var db: ActorRef = _
  var initSender: ActorRef = _

  override def receive: Receive = {
    case InitJobs(db) =>
      this.db = db
      initSender = sender
      db ! ExecSql("select job, states from skysim_jobs")

    case GetJob(seed) =>
      val rnd = new Random(seed)
      val value = jobs.toSeq(rnd.nextInt(jobs.size))
      sender ! ReceiveJob(value._1, value._2)

    case SqlResult(results, _) =>
      val value: List[(String, List[String])] = results.map(row =>
        row("job").toString -> row("states").toString.split(", ").toList
      )
      this.jobs = value.toMap

      initSender ! InitJobsDone

    case other => sys.error(this.self.path + " UNKNOWN " + other)
  }
}

object JobActor {
  def props: Props = Props(new JobActor)

  case class InitJobs(db: ActorRef)

  object InitJobsDone

  case class GetJob(seed: Int)

  case class ReceiveJob(name: String, states: List[String])

}

