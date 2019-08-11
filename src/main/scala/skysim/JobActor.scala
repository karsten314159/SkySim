package skysim

import akka.actor.{ActorRef, Props}
import skysim.DBActor.{ExecSql, InitDb, SqlResult}
import skysim.JobActor.{GetJob, InitJobs, InitJobsDone, ReceiveJob}

import scala.util.Random

case class JobActorState(
                          jobs: Map[String, List[String]],
                          db: ActorRef,
                          initSender: ActorRef
                        )


class JobActor extends SimActor {

  override def receive: Receive =
    withState(JobActorState(Map.empty, null, null))


  def withState(actorState: JobActorState): Receive = {
    case InitJobs(db) =>

      db ! ExecSql("select job, states from skysim_jobs")
      context become withState(actorState.copy(db = db, initSender = sender))

    case GetJob(seed) =>
      val rnd = new Random(seed)
      assert(actorState.jobs.nonEmpty, "jobs was empty, InitJobs called?")
      val value = actorState.jobs.toSeq(rnd.nextInt(actorState.jobs.size))
      sender ! ReceiveJob(value._1, value._2)

    case SqlResult(results, _) =>
      val value: List[(String, List[String])] = results.map(row =>
        row("job").toString -> row("states").toString.split(", ").toList
      )
      actorState.initSender ! InitJobsDone
      context become withState(actorState.copy(jobs = value.toMap))

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

