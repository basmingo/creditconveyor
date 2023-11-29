package ru.neoflex.credit.conveyor.service

import io.grpc.{Status, StatusException}
import ru.neoflex.credit.conveyor.OfferProtoService.ZioOfferProtoService.OfferProtoService
import ru.neoflex.credit.conveyor.OfferProtoService.{LoanRequest, LoanResponse}
import ru.neoflex.credit.conveyor.config.CreditPercentages
import ru.neoflex.credit.conveyor.service.utils.LoanCalculationUtils
import zio.stream
import zio.stream.ZStream

import java.util.UUID

case class OfferServiceImpl(loanCalculationUtils: LoanCalculationUtils, loanValidator: LoanValidator)
    extends OfferProtoService {
  def makeOffers(request: LoanRequest): stream.Stream[StatusException, LoanResponse] =
    loanValidator
      .validateLoanRequest(request)
      .fold(
        error =>
          ZStream.fail(
            new StatusException(
              Status.INVALID_ARGUMENT
                .augmentDescription(error.head)
            )
          ),
        _ =>
          ZStream.fromIterable(
            List(
              makeLoanOffer(request, CreditPercentages.baseRate, isInsuranceEnabled = false, isSalaryClient = false),
              makeLoanOffer(
                request,
                CreditPercentages.baseRate - 0.01,
                isInsuranceEnabled = false,
                isSalaryClient = true
              ),
              makeLoanOffer(
                request,
                CreditPercentages.baseRate - 0.05,
                isInsuranceEnabled = true,
                isSalaryClient = false
              ),
              makeLoanOffer(
                request,
                CreditPercentages.baseRate - 0.06,
                isInsuranceEnabled = true,
                isSalaryClient = true
              )
            )
          )
      )

  private def makeLoanOffer(
    request: LoanRequest,
    rate: BigDecimal,
    isInsuranceEnabled: Boolean,
    isSalaryClient: Boolean
  ): LoanResponse = {

    val totalAmount = loanCalculationUtils
      .getTotalAmount(request.term, rate, BigDecimal(request.amount), isInsuranceEnabled)
    val monthlyPayment = loanCalculationUtils
      .getAnnuityCoefficient(request.term, loanCalculationUtils.getMonthlyRate(rate)) * BigDecimal(request.amount)

    LoanResponse(
      UUID.randomUUID().toString,
      request.amount,
      totalAmount.toString,
      request.term,
      monthlyPayment.toString,
      rate.toString,
      isInsuranceEnabled,
      isSalaryClient
    )
  }
}
