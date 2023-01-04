import akka.actor.Actor

object Subscriber {
  case object Register
  case class Work(m: String)
}

import Subscriber.{ Register, Work }


class Subscriber extends Actor {
  override def receive = {
    // 发出 OK 请求以便发送初始工作
    case Register =>
      sender() ! Publisher.Ok
    
    case Work(m) =>
      // 执行已请求的工作
      System.out.println(s"Working on $m")
      // 告知发布者可以发送更多的工作
      sender() ! Publisher.Ok
  }
}
