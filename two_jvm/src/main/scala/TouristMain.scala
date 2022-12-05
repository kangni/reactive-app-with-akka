import java.util.Locale

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout

import Tourist.Start
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.SECONDS
import scala.util.{Failure, Success}

object TouristMain extends App {
  val system: ActorSystem = ActorSystem("TouristSystem")

  val path =
    "akka.tcp://BookSystem@127.0.0.1:2553/user/guidebook"

  // 解析选择会引发本地 Actor 系统尝试与远程 Actor 进行通信并验证其是否存在.
  // 由于这一过程需要花一段时间, 因此将 actor 选择解析成引用就需要一个 timeout,
  // 并且返回一个 Future(ActorRef).

  implicit val timeout: Timeout = Timeout(5, SECONDS)
  system.actorSelection(path).resolveOne().onComplete {
    case Success(guidebook) =>
      val tourProps: Props =
        Props(classOf[Tourist], guidebook)

      val tourist: ActorRef = system.actorOf(tourProps)

      tourist ! Start(Locale.getISOCountries)

    case Failure(e) => println(e)
  }
}
