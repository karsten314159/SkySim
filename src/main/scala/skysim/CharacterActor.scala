package skysim

import Log._
import akka.actor.{ActorRef, Props}
import skysim.CharacterActor.InitChar
import skysim.CityActor.{ChangePos, GetNearby, GetNearbyRes}
import skysim.JobActor.{GetJob, ReceiveJob}

case class CharState(
                      city: String, name: String, state: String, x: Int, y: Int,
                      data: Map[String, Int]
                    )

object CharacterActor {
  def props: Props = Props(new CharacterActor)

  case class InitChar(num: Int, parent: ActorRef, state: CharState, job: ActorRef)

}

class CharacterActor extends SimActor {
  var parent: ActorRef = _
  //var pos: (Int, Int) = _
  var state: CharState = _
  var jobName: String = _
  var states: List[String] = _

  override def receive: Receive = {
    case InitChar(num, parent, state, job) =>
      this.parent = parent
      job ! GetJob(num)

      this.state = state

    // println(s"char init " + this)

    case Verb("step") =>
      val ind = states.indexOf(state.state)
      state = state.copy(state = states(ind + 1 % states.length))
      parent ! ChangePos(self, state)

    case Verb("breakfast") =>
      parent ! GetNearby(self)

    case ReceiveJob(jobName, states) =>
      this.jobName = jobName
      this.states = states
      state = state.copy(
        state = states.head, data = state.data + (jobName -> 0)
      )
      parent ! ChangePos(self, state)

    case GetNearbyRes(other, otherState, dist) =>
      println(s"char " + this + " eats breakfast with " + other.path.name + ", d:" + dist)
      state = state.copy(data = state.data + ("breakfast with " + other.path.name -> 1))

      parent ! ChangePos(self, state)

    case other => sys.error(self.path + " UNKNOWN " + other)
  }
}
