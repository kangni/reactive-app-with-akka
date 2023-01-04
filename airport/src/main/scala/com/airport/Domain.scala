package com.airport

case class Weather(
  altitude: Double,
  longtitude: Double,
  floor: Double,
  ceiling: Double,
  temperature: Double,
  visibility: Double,
  precipitation: Double,
  windSpeed: Double,
  windDirection: Double
)

case class Passenger(
  id: String,
  name: String,
  bookingNumber: Int,
  seatNumber: String
)

case class Aircraft(
  id: String,
  callsign: Double,
  altitude: Double,
  speed: Double,
  heading: Double,
  passengers: List[Passenger],
  weather: List[Weather]
)
