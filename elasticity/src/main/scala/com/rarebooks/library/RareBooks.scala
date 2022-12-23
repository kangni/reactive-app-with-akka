package com.rarebooks.library

// 较 messaging, 移除了 ActorRef 导入.
import akka.actor.{ Actor, ActorLogging, Props, Stash }
import akka.routing.{ ActorRefRoutee, Router, RoundRobinRoutingLogic }
import scala.concurrent.duration.{ MILLISECONDS => Millis, FiniteDuration, Duration }

object RareBooks {

  case object Close
  case object Open
  case object Report

  def props: Props =
    Props(new RareBooks)
}

class RareBooks extends Actor with ActorLogging with Stash {

  import context.dispatcher
  import RareBooks._
  import RareBooksProtocol._

  private val openDuration: FiniteDuration =
    Duration(context.system.settings.config.getDuration("rare-books.open-duration", Millis), Millis)

  private val closeDuration: FiniteDuration =
    Duration(context.system.settings.config.getDuration("rare-books.close-duration", Millis), Millis)

  // 从配置中获取 Librarian 被路由对象的数量以便进行创建.
  private val nbrOfLibrarians: Int = context.system.settings.config getInt "rare-books.nbr-of-librarians"

  private val findBookDuration: FiniteDuration =
    Duration(context.system.settings.config.getDuration("rare-books.librarian.find-book-duration", Millis), Millis)

  var requestsToday: Int = 0
  var totalRequests: Int = 0

  var router: Router = createLibrarian()

  context.system.scheduler.scheduleOnce(openDuration, self, Close)

  /**
   * Set the initial behavior.
   *
   * @return partial function open
   */
  override def receive: Receive = open

  /**
   * Behavior that simulates RareBooks is open.
   *
   * @return partial function for completing the request.
   */
  private def open: Receive = {
    case m: Msg =>
      // 路由消息而非转发消息.
      router.route(m, sender())
      requestsToday += 1
    case Close =>
      context.system.scheduler.scheduleOnce(closeDuration, self, Open)
      log.info("Closing down for the day")
      context.become(closed)
      self ! Report
  }

  /**
   * Behavior that simulates the RareBooks is closed.
   *
   * @return partial function for completing the request.
   */
  private def closed: Receive = {
    case Report =>
      totalRequests += requestsToday
      log.info(s"$requestsToday requests processed today. Total requests processed = $totalRequests")
      requestsToday = 0
    case Open =>
      context.system.scheduler.scheduleOnce(openDuration, self, Close)
      unstashAll()
      log.info("Time to open up!")
      context.become(open)
    case _ =>
      stash()
  }

  /**
   * Create librarian as router.
   *
   * @return librarian router reference
   */
  // 返回 Router 而非 ActorRef.
  protected def createLibrarian(): Router = {
    var cnt: Int = 0
    val routees: Vector[ActorRefRoutee] = Vector.fill(nbrOfLibrarians) {
      // 创建 librarian
      val r = context.actorOf(Librarian.props(findBookDuration), s"librarian-$cnt")
      cnt += 1
      // 将 Librarian 封装在 ActorRefRoutee 中.
      ActorRefRoutee(r)
    }
    // 使用 RoundRobin, 和被路由对象的列表创建 Router.
    Router(RoundRobinRoutingLogic(), routees)
  }
}
