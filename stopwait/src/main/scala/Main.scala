import akka.actor.{ ActorRef, ActorSystem, Props }

object Main extends App {
  val system: ActorSystem = ActorSystem("StopWait")

  val subscriberProps = Props[Subscriber]
  val subscriber: ActorRef = system.actorOf(subscriberProps)

  // 发布者获取一个指向订阅者的引用
  val publisherProps = Props(classOf[Publisher], subscriber)
  val publisher: ActorRef = system.actorOf(publisherProps)

  Thread.sleep(10000)
  system.terminate()
}
