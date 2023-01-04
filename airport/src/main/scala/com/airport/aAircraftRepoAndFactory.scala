package com.airport

object AircraftRepositoryAndFactory {
  val MockAircraft = Aircraft("someId", "300", 0.0, 0.0, 0.0, Nil, Nil)

  def create(id: String, callsign: String): Aircraft = Aircraft(id, callsign, 0.0, 0.0, 0.0, Nil, Nil)

  def get(id: String): Aircraft = MockAircraft

  def getAll(): List[Aircraft] = List(MockAircraft)

  def findByCallsign(callsign: String): Aircraft = MockAircraft
}
