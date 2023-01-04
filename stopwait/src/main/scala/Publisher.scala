import akka.actor.{ Actor, ActorRef }
import Subscriber.{ Register, Work }

object Publisher {
  // 告知发布者可以发送工作的 OK 消息
  case object Ok
}

class Publisher(subscriber: ActorRef) extends Actor {
  // 发送一条初始消息, 以便启动发送过程
  override def preStart = 
    subscriber ! Register
  // 发布者在接收到 OK 消息时发送工作
  override def receive = {
    case Publisher.Ok =>
      subscriber ! Work("Do something!")
  }
}
