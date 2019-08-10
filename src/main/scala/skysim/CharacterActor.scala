package skysim

import akka.actor.{ActorRef, Props}
import skysim.CharacterActor.InitChar
import skysim.CityActor.{ChangePos, GetNearby, GetNearbyRes}
import skysim.JobActor.{GetJob, ReceiveJob}
import skysim.Log._

case class CharState(
                      city: String, name: String, status: String, x: Int, y: Int,
                      data: Map[String, Int]
                    )

object CharacterActor {
  def props: Props = Props(new CharacterActor)

  case class InitChar(num: Int, parent: ActorRef, state: CharState, job: ActorRef)

}

case class CharacterActorState(
                                parent: ActorRef,
                                state: CharState,
                                job: ReceiveJob
                              )

class CharacterActor extends SimActor {
  override def receive: Receive =
    withState(CharacterActorState(null, CharState("", "", "", 0, 0, Map.empty), ReceiveJob("", List(""))))

  def withState(actorState: CharacterActorState): Receive = {
    case InitChar(num, parent, newState, job) =>
      job ! GetJob(num)

      context become withState(actorState.copy(
        parent = parent,
        state = newState
      ))

    case Verb("step") =>
      val ind = actorState.job.states.indexOf(actorState.state.status)
      val newState = actorState.state.copy(status = actorState.job.states(ind + 1 % actorState.job.states.length))

      actorState.parent ! ChangePos(self, newState)
      context become withState(actorState.copy(
        state = newState
      ))

    case Verb("breakfast") =>
      actorState.parent ! GetNearby(self)

    case r@ReceiveJob(jobName, jobsStates) =>

      val newState = actorState.state.copy(
        status = jobsStates.head, data = actorState.state.data + (jobName -> 0)
      )
      actorState.parent ! ChangePos(self, newState)

      context become withState(actorState.copy(
        state = newState,
        job = r
      ))

    case GetNearbyRes(other, otherState, dist) =>

      val newState = actorState.state.copy(data = actorState.state.data + ("breakfast with " + other.path.name -> 1))
      println(s"char " + this + " eats breakfast with " + other.path.name + ", d:" + dist)

      actorState.parent ! ChangePos(self, newState)
      context become withState(actorState.copy(
        state = newState
      ))

    case other => sys.error(self.path + " UNKNOWN " + other)
  }
}
