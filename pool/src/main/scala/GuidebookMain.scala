import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.FromConfig

object GuidebookMain extends App {
  val system: ActorSystem = ActorSystem("BookSystem")

  val guideProps: Props = Props[Guidebook]
  
  // 封装用于 guidebook actor 的初始 Props 的 pool 配置.
  val routerProps: Props = FromConfig.props(guideProps)

  // 注意 Actor 的名称必须匹配配置文件中的名称.
  val guidebook: ActorRef = system.actorOf(routerProps, "guidebook")
}
