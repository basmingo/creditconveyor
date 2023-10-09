package ru.neoflex.credit.conveyor.service

import io.grpc.{Status, StatusException}
import ru.neoflex.credit.conveyor.OfferProtoService.ZioOfferProtoService.OfferProtoService
import ru.neoflex.credit.conveyor.OfferProtoService.{LoanRequest, LoanResponse}
import ru.neoflex.credit.conveyor.service.utils.LoanCalculationUtils
import zio.stream.ZStream
import zio.{ZIO, ZLayer, stream}

import java.util.UUID

case class OfferServiceImpl(loanCalculationUtils: LoanCalculationUtils) extends OfferProtoService {
  private val BASE_CREDIT_PERCENTAGE: BigDecimal = 0.15

  def makeOffers(request: LoanRequest): stream.Stream[StatusException, LoanResponse] = {
    LoanValidator.validateLoanRequest(request).fold(
      error => ZStream.fail(new StatusException(
        Status.INVALID_ARGUMENT
          .augmentDescription(error.head))),
      _ => ZStream.fromIterable(
        List(
          makeLoanOffer(request, BASE_CREDIT_PERCENTAGE, isInsuranceEnabled = false, isSalaryClient = false),
          makeLoanOffer(request, BASE_CREDIT_PERCENTAGE - 0.01, isInsuranceEnabled = false, isSalaryClient = true),
          makeLoanOffer(request, BASE_CREDIT_PERCENTAGE - 0.05, isInsuranceEnabled = true, isSalaryClient = false),
          makeLoanOffer(request, BASE_CREDIT_PERCENTAGE - 0.06, isInsuranceEnabled = true, isSalaryClient = true)
        )
      )
    )
  }

  def makeLoanOffer(request: LoanRequest,
                    rate: BigDecimal,
                    isInsuranceEnabled: Boolean,
                    isSalaryClient: Boolean): LoanResponse = {

    val totalAmount = loanCalculationUtils.getTotalAmount(
      request.term,
      rate,
      BigDecimal(request.amount),
      isInsuranceEnabled)

    val monthlyPayment = loanCalculationUtils.getAnnuityCoefficient(
      request.term,
      loanCalculationUtils.getMonthlyRate(rate)) * BigDecimal(request.amount)

    LoanResponse(
      UUID.randomUUID().toString,
      request.amount,
      totalAmount.toString,
      request.term,
      monthlyPayment.toString,
      rate.toString,
      isInsuranceEnabled,
      isSalaryClient)
  }
}

object OfferServiceImpl {
  val layer: ZLayer[LoanCalculationUtils, Nothing, OfferProtoService] = ZLayer {
    for {
      utils <- ZIO.service[LoanCalculationUtils]
    } yield OfferServiceImpl(utils)
  }
}
