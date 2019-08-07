package skysim

import akka.actor.{ActorRef, Props}
import skysim.CharacterActor.InitChar
import skysim.CityActor.{GetNearby, GetNearbyRes}

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

    case GetNearbyRes(actorRef: ActorRef, d: Double) =>
      println(s"char " + this + " eats breakfast with " + actorRef.path.name + ", d:" + d)

    case other => sys.error(this.self.path + " UNKNOWN " + other)
  }
}
