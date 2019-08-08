package skysim

import akka.actor.{ActorRef, Props}
import skysim.DBActor.{Sql, SqlResult}
import skysim.JobActor.{GetJob, InitJobs, ReceiveJob}

import scala.util.Random

class JobActor extends SimActor {
  var jobs: Map[String, List[String]] = _
  var db: ActorRef = _

  override def receive: Receive = {
    case InitJobs(db) =>
      this.db = db
      db ! Sql("select job, states from skysim_jobs")

    case GetJob(seed) =>
      val rnd = new Random(seed)
      val value = jobs.toSeq(rnd.nextInt(jobs.size))
      sender ! ReceiveJob(value._1, value._2)

    case SqlResult(x, _) =>
      val value: List[(String, List[String])] = x.map(x =>
        x("job").toString -> x("states").toString.split(", ").toList
      )
      this.jobs = value.toMap

    case other => sys.error(this.self.path + " UNKNOWN " + other)
  }
}

object JobActor {
  def props: Props = Props(new JobActor)

  case class InitJobs(db: ActorRef)

  case class GetJob(seed: Int)

  case class ReceiveJob(name: String, states: List[String])

}

