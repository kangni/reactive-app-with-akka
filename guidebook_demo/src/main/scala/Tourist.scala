object Tourist {
  case class Guidance(code: String, description: String)

  case class Start(codes: Seq[String])
}

import akka.actor.{Actor, ActorRef}

import Guidebook.Inquiry
import Tourist.{Guidance, Start}

class Tourist(guidebook: ActorRef) extends Actor {

  override def receive = {
    // 接收 start 消息, 提取出国家编码并且为找到的每一个编码向 Guidebook actor 发送一条 Inquiry 消息.
    case Start(codes) =>
      // ! 运算符
      // 使用 ! 运算符从一个 actor 将消息发送到另一个 actor.
      // ref ! Message(x) 等价于 ref.!(Message(x))(self)
      // 这两个方法都使用了 self 值, self 值是由 Actor 特性提供的 ActorRef, 用作对自身的引用.
      // ! 运算符利用了 Scala 中缀表示法的优点, 但实际上, self 会被声明为隐式值.
      codes.foreach(guidebook ! Inquiry(_))

    // 接收 Guidence 消息, 并且将消息中的国家编码和描述打印到控制台.
    case Guidance(code, description) =>
      println(s"$code: $description")
  }
}
