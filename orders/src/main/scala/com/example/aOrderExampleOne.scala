package com.example

import java.util.UUID

import org.scalactic._
import Accumulation._
import OrderType._

trait ValidationFailure {
  def message: String
}

case class InvalidId(message: String) extends ValidationFailure
case class InvalidCustomerId(message: String) extends ValidationFailure
case class InvalidOrderType(message: String) extends ValidationFailure
case class InvalidDate(message: String) extends ValidationFailure
case class InvalidOrderLine(message: String) extends ValidationFailure
case class InvalidOrderLines(message: String) extends ValidationFailure

// Order 的领域聚合
case class Order private[Order] (
  id: String,
  customerId: String,
  date: Long,
  orderType: OrderType,
  orderLines: List[OrderLine])

// 一种可以针对订单类型进行验证的结构
object OrderType {

  type OrderType = String

  val Phone = new OrderType("phone")
  val Web = new OrderType("web")
  val Promo = new OrderType("promo")
  val OrderTypes: List[OrderType] = List(Phone, Web, Promo)
}

case class OrderLine(
  itemId: String,
  quantity: Int
)

object Order {

  def apply(customerId: String, date: Long, orderType: OrderType, orderLines: List[OrderLine]): Order Or Every[ValidationFailure] = {
    withGood(
      validateCustomerId(customerId),
      validateDate(date),
      validateOrderType(orderType),
      validateOrderLines(orderLines)
    ) { (cid, dt, ot, ols) => Order(UUID.randomUUID.toString, cid, dt, ot, ols) }
  }

  // 默认的构造函数是私有的, 因此可以通过在同伴对象中使用 apply() 方法来确保创建时得到正确验证.
  // 所有的验证都会执行并且引发对订单的默认构造函数的调用(也就是创建订单并且返回)或者返回失败的集合.
  private def validateId(id: String): String Or Every[ValidationFailure] =
    if (id !=null && !id.isEmpty)
      Good(id)
    else
      Bad(One(InvalidId(id)))

  private def validateDate(date: Long): Long Or Every[ValidationFailure] =
    if (date > 0)
      Good(date)
    else
      Bad(One(InvalidDate(date.toString)))

  private def validateCustomerId(customerId: String): String Or Every[ValidationFailure] =
    if (customerId !=null && !customerId.isEmpty)
      Good(customerId)
    else
      Bad(One(InvalidCustomerId(customerId)))

  private def validateOrderType(orderType: OrderType): OrderType Or Every[ValidationFailure] =
    if (OrderTypes.contains(orderType))
      Good(orderType)
    else
      Bad(One(InvalidOrderType(orderType)))

  // 每项验证都应该尽可能详尽地进行处理, 这些验证都很简单
  private def validateOrderLines(orderLines: List[OrderLine]): List[OrderLine] Or Every[ValidationFailure] =
    if (!orderLines.isEmpty)
      Good(orderLines)
    else
      Bad(One(InvalidOrderLines(orderLines.mkString)))
}
