package skysim

import akka.actor.{ActorRef, Props}

object CharacterRef {
  def props: Props = Props(new CharacterRef)
}

class CharacterRef extends SimActor {
  var parent: ActorRef = _
  var pos: (Int, Int) = _

  override def receive: Receive = {
    case ("init", parent: ActorRef, pos: (Int, Int)) =>
      this.parent = parent
      this.pos = pos

    // println(s"char init " + this)

    case "breakfast" =>
      parent ! ("getNearby", this.self, pos)


    case ("getNearbyRes", actorRef: ActorRef) =>
      println(s"char " + this + " eats breakfast with " + actorRef.path.name)

    case other => sys.error(this.self.path + " UNKNOWN " + other)
  }
}