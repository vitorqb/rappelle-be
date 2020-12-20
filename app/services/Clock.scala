package services

import org.joda.time.DateTime

trait ClockLike {
  def now(): DateTime
}

class Clock extends ClockLike {
  def now(): DateTime = DateTime.now()
}

class FakeClock(now: DateTime) extends ClockLike {
  def now() = now
}
