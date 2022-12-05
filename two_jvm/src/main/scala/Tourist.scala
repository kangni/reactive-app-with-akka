object Tourist {
  case class Guidence(code: String, description: String)
  case class Start(codes: Seq[String])
}

import akka.actor.{Actor, ActorRef}

import Guidebook.Inquiry
import Tourist.{Guidence, Start}

class Tourist(guidebook: ActorRef) extends Actor {
  override def receive = {
    case Start(codes) =>
      codes.foreach(guidebook ! Inquiry(_))
    case Guidence(code, description) =>
      println(s"$code: $description")
  }
}
