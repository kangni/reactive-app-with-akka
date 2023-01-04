package com.example

import java.util.UUID

import akka.persistence.PersistentActor
import org.scalactic._
import Accumulation._

import OrderType._

// 用于整洁封装的订单 Actor 的同伴对象
object OrderActor {

  object OrderType {

    type OrderType = String

    val Phone = new OrderType("phone")
    val Web = new OrderType("web")
    val Promo = new OrderType("promo")
    val OrderTypes: List[OrderType] = List(Phone, Web, Promo)
  }

  // 表明命令被接收的简单确认对象
  case object CommandAccepted

  // 所有命令都必须指定订单的上一个已知版本
  case class ExpectedVersionMismatch(expected: Long, actual: Long)

  // 创建和添加订单记录行的命令
  case class CreateOrder(
    id: UUID,
    customerId: String,
    date: Long,
    orderType: OrderType,
    orderLines: List[OrderLine])

  case class AddOrderLine(
    id: UUID,
    orderLine: OrderLine,
    expectedVersion: Long)

  // 订单已创建和订单记录行已添加的事件
  case class OrderCreated(
   id: UUID,
   customerId: String,
   date: Long,
   orderType: OrderType,
   orderLines: List[OrderLine],
   version: Long)

  case class OrderLineAdded(
    id: UUID,
    orderLine: OrderLine,
    version: Long)
}

// 这个 Actor 在集群中具有单一实例
class OrderActor extends PersistentActor {

  import OrderActor._

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  // Actor 的内部状态, 每次处理命令时都会修改这一状态
  private case class OrderState(
    id: UUID = null,
    customerId: String = null,
    date: Long = -1L,
    orderType: OrderType = null,
    orderLines: List[OrderLine] = Nil, version: Long = -1L)

  private var state = OrderState()

  // 在订单未被创建时使用, 生成订单的初始化状态
  def create: Receive = {
    case CreateOrder(id, customerId, date, orderType, orderLines) =>
      val validations = withGood(
        validateCustomerId(customerId),
        validateDate(date),
        validateOrderType(orderType),
        validateOrderLines(orderLines)
      ) { (cid, d, ot, ol) => OrderCreated(UUID.randomUUID(), cid, d, ot, ol, 0L) }
      sender ! validations.fold(
        event => {
          sender ! CommandAccepted
          persist(event) { e =>
            state = OrderState(event.id, event.customerId, event.date, event.orderType, event.orderLines, 0L)
            // 在验证成功之后, 就可以存储事件并且执行影响任意一端的逻辑,
            // 比如通过事件流将事件发送到干系方、转向新的命令处理程序以及更新状态.
            context.system.eventStream.publish(e)
            context.become(created)
          }
        },
        bad  => sender ! bad
      )
  }

  // create 处理程序会处理除创建外的其他命令, 这行命令展示了订单是如何添加的.
  def created: Receive = {
    case AddOrderLine(id, orderLine, expectedVersion) =>
      if (expectedVersion != state.version)
        sender ! ExpectedVersionMismatch(expectedVersion, state.version)
      else {
        val validations = withGood(
          validateOrderLines(state.orderLines :+ orderLine)
        ) { (ol) => OrderLineAdded(id, orderLine, state.version + 1) }
        .fold(
          event => {
            persist(OrderLineAdded(id, orderLine, state.version + 1)) { e =>
              state = state.copy(orderLines = state.orderLines :+ e.orderLine, version = state.version + 1)
              context.system.eventStream.publish(e)
            }
          },
          bad => sender ! bad
        )
      }
  }

  // 设置初始化命令处理程序, 以便创建分部函数, 也就是聚合的第一个状态.
  override def receiveCommand = create

  // receiveRecover 会从过去已发生的事件中构建状态.
  // 其中无须验证.
  // 当 Actor 被集群实例化时就会进行恢复.
  override def receiveRecover: Receive = {
    case CreateOrder(id, customerId, date, orderType, orderLines) =>
      state = OrderState(id, customerId, date, orderType, orderLines, 0L)
      context.become(created)
    case AddOrderLine(id, orderLine, expectedVersion)             =>
      state = state.copy(orderLines = state.orderLines :+ orderLine, version = state.version + 1)
  }

  def validateCustomerId(customerId: String): String Or Every[ValidationFailure] =
    if (Option(customerId).exists(_.trim.nonEmpty))
      Good(customerId)
    else
      Bad(One(InvalidCustomerId(customerId)))

  private def validateDate(date: Long): Long Or Every[ValidationFailure] =
    if (date > 0)
      Good(date)
    else
      Bad(One(InvalidDate(date.toString)))

  private def validateOrderType(orderType: OrderType): OrderType Or Every[ValidationFailure] =
    if (OrderTypes.contains(orderType))
      Good(orderType)
    else
      Bad(One(InvalidOrderType(orderType)))

  private def validateOrderLines(orderLines: List[OrderLine]): List[OrderLine] Or Every[ValidationFailure] =
    if (!orderLines.isEmpty)
      Good(orderLines)
    else
      Bad(One(InvalidOrderLines(orderLines.mkString)))
}
