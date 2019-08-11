package skysim

import akka.actor.{ActorRef, Props}
import skysim.CharacterActor.InitChar
import skysim.CityActor.{ChangePos, GetNearby, GetNearbyRes, InitCity}
import skysim.DBActor.{SqlResult, StoreDb}
import skysim.Log._

import scala.collection.mutable
import scala.util.Random

object CityActor {
  def props: Props = Props(new CityActor)

  case class InitCity(numCitizens: Int, db: ActorRef, jobs: ActorRef)

  case class ChangePos(ac: ActorRef, state: CharState)

  case class GetNearbyRes(
                           other: ActorRef, otherState: CharState, dist: Double
                         )

  case class GetNearby(requester: ActorRef)

}

case class CityActorState(
                           db: ActorRef,
                           jobs: ActorRef,
                           citizens: Map[ActorRef, CharState]
                         )

class CityActor extends SimActor {

  override def receive: Receive =
    withState(CityActorState(null, null, Map.empty))


  def name(random: Random, names: mutable.Set[String]): String = {
    val vowels = "AEIOU"
    val consonants = "BCDFGHJKLMNPQRSTVWYZ"
    val name =
      1.to(random.nextInt(2) + 2) map { _ =>
        consonants.charAt(random.nextInt(consonants.length)) + "" +
          vowels.charAt(random.nextInt(vowels.length))
      } mkString ""
    val res =
      if (names.contains(name)) {
        name + "a"
      } else {
        name
      }
    names += res
    res.charAt(0) + res.substring(1).toLowerCase
  }

  def withState(actorState: CityActorState): Receive = {
    case InitCity(size, db, jobs) =>
      println(s"init city " + this + " with " + size + " citizens")
      val names: mutable.Set[String] = mutable.Set[String]()
      val citizens =
        0.until(size) map { i =>
          val rnd = new Random(i)
          val nameVal = name(rnd, names)
          val (x, y) = (rnd.nextInt(100), rnd.nextInt(100))

          val c = context.actorOf(CharacterActor.props, nameVal)
          val p = CharState(
            nameVal, this.self.path.name, "idle", x, y, Map.empty
          )
          c ! InitChar(i, this.self, p, jobs)
          c -> p
        }

      context become withState(actorState.copy(
        citizens = citizens.toMap, db = db, jobs = jobs
      ))

    case v@Verb("step" | "breakfast") =>
      actorState.citizens foreach { cha =>
        cha._1 ! v
      }

    case ChangePos(ac: ActorRef, p: CharState) =>

      actorState.db ! StoreDb(Seq(p))
      context become withState(actorState.copy(citizens = actorState.citizens + (ac -> p)))

    /*case Verb("save") =>
      this.db ! StoreDb(citizens.values.toSeq)*/

    case SqlResult(_, _) =>
      println("saved")

    case GetNearby(requester: ActorRef) =>
      val pos: CharState = actorState.citizens(requester)
      val (res: ActorRef, d: Double) =
        actorState.citizens map { case (other: ActorRef, p: CharState) =>
          if (other == requester)
            other -> Double.PositiveInfinity
          else {
            val dx = pos.x - p.x
            val dy = pos.y - p.y
            other -> (dy * dy + dx * dx).toDouble
          }
        } minBy { case (_, p: Double) => p }
      sender ! GetNearbyRes(res, actorState.citizens(res), Math.sqrt(d))

    case other => sys.error(this.self.path + " UNKNOWN <" + other + ">")
  }
}