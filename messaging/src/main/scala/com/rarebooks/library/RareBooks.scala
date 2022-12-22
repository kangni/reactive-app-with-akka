package com.rarebooks.library

import akka.actor.{ Actor, ActorRef, ActorLogging, Props, Stash }
import scala.concurrent.duration.{ MILLISECONDS => Millis, FiniteDuration, Duration }

object RareBooks {
  case object Close
  case object Open
  case object Report

  // 为创建 Actor 提供属性工厂.
  // 好的实践是, 将返回 actor Props 的函数放在同伴对象中.
  // 这一做法会确保函数不会意外地对 actor 状态创建闭包, 
  // 以免出现 akka 使用函数创建 actor 类型的实例时难以排查 bug 的情况.
  def props: Props =
    Props(new RareBooks)
}

// 为了将存放功能添加到 actor, 需要继承 stash 特性.
// 当 actor 遇到无法在当前状态中处理消息时, 就可以调用 stash()来保存消息.
// 当 actor 再次切换状态时, 调用 unstashAll()就会让之前存放的所有消息被再次提交.

// unstashAll() 函数通常会在调用 become() 之前立即调用, 而 receive 函数通常会通过匹配通配符来调用 stash().
class RareBooks extends Actor with ActorLogging with Stash {
  import context.dispatcher

  // 从协议对象和同伴对象导入消息定义.
  import RareBooks._
  import RareBooksProtocol._

  // 定义模拟过程中各种事件持续的时长.
  private val openDuration: FiniteDuration =
    Duration(context.system.settings.config.getDuration("rare-books.open-duration", Millis), Millis)

  private val closeDuration: FiniteDuration =
    Duration(context.system.settings.config.getDuration("rare-books.close-duration", Millis), Millis)

  private val findBookDuration: FiniteDuration =
    Duration(context.system.settings.config.getDuration("rare-books.librarian.find-book-duration", Millis), Millis)

  // 目前只有一个 librarian.
  private val librarian = createLibrarian()

  // 初始化参数.
  var requestsToday: Int = 0
  var totalRequests: Int = 0

  // 调度首个关闭事件.
  context.system.scheduler.scheduleOnce(openDuration, self, Close)

  // 以开放状态开始.
  override def receive: Receive = open

  private def open: Receive = {
    case m: Msg =>
      // 将协议消息发送到 librarian.
      librarian forward m
      requestsToday += 1
    case Close =>
      // 在关闭时, 安排何时重新开放.
      context.system.scheduler.scheduleOnce(closeDuration, self, Open)
      log.info("Closing down for the day")
      // 切换到 closed 函数. 关闭书店.
      context.become(closed)

      // 告知自己运行报告.
      self ! Report
  }

  private def closed: Receive = {
    case Report =>
      // 更新总运行次数;
      // 打印报告;
      // 重置每日总运行次数.
      totalRequests += requestsToday
      log.info(s"$requestsToday requests processed today. Total requests processed = $totalRequests")
      requestsToday = 0
    case Open =>
      // 在开放前, 安排何时关闭.
      context.system.scheduler.scheduleOnce(openDuration, self, Close)
      // 取出书店关闭期间到达的消息.
      unstashAll()
      log.info("Time to open up!")
      // 开放书店.
      context.become(open)
    case _ =>
      // 存放书店关闭期间到达的其他消息.
      stash()
  }

  protected def createLibrarian(): ActorRef = {
    context.actorOf(Librarian.props(findBookDuration), "librarian")
  }
}
