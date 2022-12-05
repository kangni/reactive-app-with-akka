import java.util.Locale

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.FromConfig

import Tourist.Start

object TouristMain extends App {
  val system: ActorSystem = ActorSystem("TouriseSystem")

  // 使用另一个名称以便区分路由器
  val guidebook: ActorRef = system.actorOf(FromConfig.props(), "balancer")

  val tourProps: Props = Props(classOf[Tourist], guidebook)
  val tourist: ActorRef = system.actorOf(tourProps)

  tourist ! Start(Locale.getISOCountries)
}
