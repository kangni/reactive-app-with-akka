package com.rarebooks.library

import akka.actor.{ ActorRef, Actor, ActorLogging, Props }
import scala.util.Random

object Customer {
  import RareBooksProtocol._

  def props(rareBooks: ActorRef, odds: Int, tolerance: Int): Props =
    Props(new Customer(rareBooks, odds, tolerance))

  // Customer 将提交成功研究请求的概率.
  case class CustomerModel(
    odds: Int,
    tolerance: Int,
    found: Int,
    notFound: Int
  )

  private case class State(model: CustomerModel, timeInMillis: Long) {
    // 根据当前状态和接收到的消息, 生成新的状态.
    def update(m: Msg): State = m match {
      case BookFound(b, d)    => copy(model.copy(found = model.found + b.size), timeInMillis = d)
      case BookNotFound(_, d) => copy(model.copy(notFound = model.notFound + 1), timeInMillis = d)
      case Credit(d)          => copy(model.copy(notFound = 0), timeInMillis = d)
    }
  }
}

// 需要注意
// 将 var 而非 val 用于 state;
// Customer 会对 热questBookInfo() 进行初始调用以便初始化信息流;
// 只需要一个接收函数, 不会调用 become() 或 unbecome().
class Customer(rareBooks: ActorRef, odds: Int, tolerance: Int) extends Actor with ActorLogging {
  import Customer._
  import RareBooksProtocol._

  // 从一个中性状态开始初始化.
  private var state = State(CustomerModel(odds, tolerance, 0, 0), -1L)

  // 发送初始请求以便开始信息流.
  requestBookInfo()

  override def receive: Receive = {
    // 确保仅处理协议消息的逻辑入口.
    case m: Msg => m match {
      case f: BookFound =>
        state = state.update(f)
        log.info(f"{} Book(s) found!", f.books.size)
        requestBookInfo()
      // 在未超出容忍度时处理 NotFound 消息.
      case f: BookNotFound if state.model.notFound < state.model.tolerance =>
        state = state.update(f)
        log.info(f"{} Book(s) not found! My tolerance is {}.", state.model.notFound, state.model.tolerance)
        requestBookInfo()
      // 在超出容忍度时处理 NotFound 消息.
      case f: BookNotFound =>
        state = state.update(f)
        // 将投诉发送回 librarian.
        sender ! Complain()
        log.info(f"{} Book(s) not found! Reached my tolerance of {}. Sent complaint!",
          state.model.notFound, state.model.tolerance)
      // 恢复发送研究请求.
      case c: Credit =>
        state = state.update(c)
        log.info("Credit received, will start requesting again!")
        requestBookInfo()
      case g: GetCustomer =>
        sender ! state.model
    }
  }

  // 发送关于某个 topic 的研究请求的辅助器.
  private def requestBookInfo(): Unit =
    rareBooks ! FindBookByTopic(Set(pickTopic))

  // 选取用于随机且可能未知 topic 的辅助器.
  private def pickTopic: Topic =
    if (Random.nextInt(100) < state.model.odds) viableTopics(Random.nextInt(viableTopics.size)) else Unknown
}
