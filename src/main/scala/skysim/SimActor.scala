package skysim

import akka.actor.Actor

abstract class SimActor extends Actor {
  override def toString(): String = {
    this.self.path.name + " of " + this.self.path.parent.name
  }
}
