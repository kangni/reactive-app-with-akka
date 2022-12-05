object Guidebook {
  // actor 的大部分行为都是通过扩展 akka.actor.Actor 特性来提供的.
  // 无法被内置到参与者特性中的一个行为就是接收信息时如何处理, 因为该行为是特定于应用的.
  // 该行为需要通过实现抽象的 receive 方法来提供.
  case class Inquiry(code: String)
}

import java.util.{Currency, Locale}

import akka.actor.Actor

import Guidebook.Inquiry
import Tourist.Guidance

class Guidebook extends Actor {
  def describe(locale: Locale) =
    s"""In ${locale.getDisplayCountry},
        ${locale.getDisplayLanguage} is spoken and the currency
        is the ${Currency.getInstance(locale).getDisplayName}"""

  // actor 的大部分行为都是通过扩展 akka.actor.Actor 特性来提供的.
  // 无法被内置到参与者特性中的一个行为就是接收信息时如何处理, 因为该行为是特定于应用的.
  // 该行为需要通过实现抽象的 receive 方法来提供.
  override def receive = { case Inquiry(code) =>
    println(s"Actor ${self.path.name} responding to inquiry about $code")
    Locale.getAvailableLocales.filter(_.getCountry == code).foreach { locale =>
      sender ! Guidance(code, describe(locale))
    }
  }
}
