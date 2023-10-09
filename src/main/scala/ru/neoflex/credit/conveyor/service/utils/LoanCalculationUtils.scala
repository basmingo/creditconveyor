package ru.neoflex.credit.conveyor.service.utils

import zio.{ULayer, URIO, ZIO, ZLayer}

trait LoanCalculationUtils {

  def getTotalAmount(term: Int, rate: BigDecimal, amount: BigDecimal, isInsuranceEnabled: Boolean): BigDecimal

  def getMonthlyRate(rate: BigDecimal): BigDecimal

  def getAnnuityCoefficient(term: Int, baseCreditRate: BigDecimal): BigDecimal
}

case class LoanCalculationUtilsImpl() extends LoanCalculationUtils {
  def getTotalAmount(term: Int, rate: BigDecimal, amount: BigDecimal, isInsuranceEnabled: Boolean): BigDecimal =
    if (isInsuranceEnabled) {
      (getAnnuityCoefficient(term, getMonthlyRate(rate)) * amount * term) + (amount / 1000 * term + 10_000)
    } else {
      getAnnuityCoefficient(term, getMonthlyRate(rate)) * amount * term
    }

  def getMonthlyRate(rate: BigDecimal): BigDecimal = rate / 12

  def getAnnuityCoefficient(term: Int, baseCreditRate: BigDecimal): BigDecimal =
    (baseCreditRate + 1).pow(term) * baseCreditRate / ((baseCreditRate + 1).pow(term) - 1)
}

object LoanCalculationUtilsImpl {
  val layer: ULayer[LoanCalculationUtils] = ZLayer.succeed(LoanCalculationUtilsImpl())
}
