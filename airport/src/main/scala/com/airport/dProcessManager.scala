package com.airport

import akka.actor.{ReceiveTimeout, Actor, ActorRef}

import scala.concurrent.duration._

// 银行转账协议
object BankTransferProcessProtocol {
  sealed trait BankTransferProcessMessage

  final case class TransferFunds(
    transactionId: String,
    fromAccount: ActorRef,
    toAccount: ActorRef,
    amount: Double) extends BankTransferProcessMessage
}

// 同伴对象, 其中具有正面和负面的确认消息.
object BankTransferProcess {
  final case class FundsTransfered(transactionId: String)
  final case class TransferFailed(transactionId: String)
}

object AccountProtocol {
  sealed trait AccountProtocolMessage

  case class Withdraw(amount: Double) extends AccountProtocolMessage
  case class Deposit(amount: Double) extends AccountProtocolMessage
  final case object Acknowledgment
}

class BankTransferProcess extends Actor {
  import BankTransferProcess._
  import BankTransferProcessProtocol._
  import AccountProtocol._

  // 接受超时允许处理过程中的任意步骤花费 30 分钟.
  context.setReceiveTimeout(30.minutes)

  // 转账的初始请求包含了执行任务所需的所有信息.
  // 其中包括发送方 Actor 引用, 这里将之复制到客户端以便可以跨接收边界回复转账的初始请求方.
  override def receive = {
    case TransferFunds(transactionId, fromAccount, toAccount, amount) =>
      fromAccount ! Withdraw(amount)
      val client = sender()
      context become awaitWithdrawal(transactionId, amount, toAccount, client)
  }

  // 等待提款或者接收超时消息(包含在 Akka 框架中)中的转账失败提示.
  def awaitWithdrawal(transactionId: String, amount: Double, toAccount: ActorRef, client: ActorRef): Recceive = {
    case Acknowledgment =>
      toAccount ! Deposit(amount)
      context become awaitDeposit(transactionId, client)

    case ReceiveTimeout =>
      client ! TransferFailed(transactionId)

      // 进程自我销毁.
      context.stop(self)
  }

  // 等待收款或者接收超时消息中的转账失败提示.
  def awaitDeposit(transactionId: String, client: ActorRef): Receive = {
    case Acknowledgment =>
      client ! FundsTransfered(transactionId)
      context.stop(self)

    case ReceiveTimeout =>
      client ! TransferFailed(transactionId)
      context.stop(self)
  }
}
