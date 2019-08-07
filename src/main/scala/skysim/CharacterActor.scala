package skysim

import akka.actor.{ActorRef, Props}
import skysim.CharacterActor.InitChar
import skysim.CityActor.{ChangePos, GetNearby, GetNearbyRes}

case class CharState(
                      city: String, name: String, state: String, x: Int, y: Int,
                      data: String
                    )

object CharacterActor {
  def props: Props = Props(new CharacterActor)

  case class InitChar(num: Int, parent: ActorRef)

}

class CharacterActor extends SimActor {
  var parent: ActorRef = _
  //var pos: (Int, Int) = _
  //var state: CharState = _

  override def receive: Receive = {
    case InitChar(num: Int, parent: ActorRef) =>
      this.parent = parent
    /*this.state = CharState(
      "cit" + num, parent.path.name, "idle", pos._1, pos._2
    )*/

    // println(s"char init " + this)

    case Verb("breakfast") =>
      parent ! GetNearby(this.self) //, pos)

    case GetNearbyRes(selfState, other, otherState, dist) =>
      println(s"char " + this + " eats breakfast with " + other.path.name + ", d:" + dist)
      parent ! ChangePos(self, selfState.copy(data = "breakfast with " + other.path.name))

    case other => sys.error(this.self.path + " UNKNOWN " + other)
  }
}
