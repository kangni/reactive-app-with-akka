import akka.actor.Actor

class Echo extends Actor {
  def receive = {
    case msg =>
      // akka 通过 Actor sender() 将发送者变为可用的 ActorRef.
      sender() ! msg
  }
}
