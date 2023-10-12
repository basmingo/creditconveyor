package ru.neoflex.credit.conveyor.service.utils

import com.google.protobuf.timestamp.Timestamp
import zio.{ULayer, ZLayer}

import java.time.Instant

trait ScoringUtils {

  def clientIsOlderThan(birthdate: Timestamp, age: Int): Boolean

  def clientIsYoungerThan(birthdate: Timestamp, age: Int): Boolean
}

case class ScoringUtilsImpl() extends ScoringUtils {
  private val secondsPerYear: Long = 31_536_000L

  override def clientIsOlderThan(birthdate: Timestamp, age: Int): Boolean =
    Instant
      .ofEpochSecond(birthdate.seconds, birthdate.nanos)
      .isBefore(getDateByYearsAgo(age))

  override def clientIsYoungerThan(birthdate: Timestamp, age: Int): Boolean =
    Instant
      .ofEpochSecond(birthdate.seconds, birthdate.nanos)
      .isAfter(getDateByYearsAgo(age))

  private def getDateByYearsAgo(years: Int): Instant =
    Instant.now.minusSeconds(secondsPerYear * years)
}

object ScoringUtilsImpl {
  val layer: ULayer[ScoringUtils] = ZLayer.succeed(ScoringUtilsImpl())
}
