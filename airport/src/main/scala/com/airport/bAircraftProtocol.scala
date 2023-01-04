package com.airport

// 消息传递协议
object AircraftProtocol {
  // 最好的做法是密封这些消息, 这样就可以让 Scala 在某条消息未实现时匹配错误.
  sealed trait AircraftProtocolMessage

  // 下面这些消息都是不可变的并且不会造成任何数据直接返回.
  final case class ChangeAltitude(altitude: Double) extends AircraftProtocolMessage
  final case class ChangeSpeed(speed: Double) extends AircraftProtocolMessage
  final case class ChangeHeading(heading: Double) extends AircraftProtocolMessage
  final case class BoardPassenger(passenger: Passenger) extends AircraftProtocolMessage
  final case class AddWeather(weather: Weather) extends AircraftProtocolMessage
  final case object OK
}
