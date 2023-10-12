package ru.neoflex.credit.conveyor.service

import com.google.protobuf.timestamp.Timestamp
import io.grpc.{Status, StatusException}
import ru.neoflex.credit.conveyor.ScoringProtoService.ZioScoringProtoService.ScoringProtoService
import ru.neoflex.credit.conveyor.ScoringProtoService._
import ru.neoflex.credit.conveyor.config.{CreditPercentages, PositionFilter}
import ru.neoflex.credit.conveyor.service.utils.{LoanCalculationUtils, ScoringUtils}
import zio.{IO, ZIO}

import java.time.{Duration, Instant}

case class ScoringServiceImpl(scoringUtils: ScoringUtils, loanCalculationUtils: LoanCalculationUtils)
  extends ScoringProtoService {
  override def scoreData(request: ScoringRequest): IO[StatusException, Credit] = {
    println(Instant.ofEpochSecond(request.birthdate.get.seconds, request.birthdate.get.nanos))
    ZIO.succeed(Credit())
    for {
      validRequest <- validateRequest(request)
      calculateRate <- calculateRate(validRequest)
      created <- creditResp(request, calculateRate)
    } yield created
  }

  private def creditResp(request: ScoringRequest, inputRate: BigDecimal): IO[StatusException, Credit] =
    ZIO.succeed(loanCalculationUtils.getTotalAmount(
      request.term,
      inputRate,
      BigDecimal(request.amount),
      request.isInsuranceEnabled))
      .map(totalAmount =>
        Credit(
          amount = request.amount,
          term = request.term,
          monthlyPayment = (totalAmount / request.term).toString,
          rate = inputRate.toString,
          psk = "???",
          isInsuranceEnabled = request.isInsuranceEnabled,
          isSalaryClient = request.isSalaryClient,
          paymentSchedule = makeSchedules(request, totalAmount))
      )

  private def makeSchedules(request: ScoringRequest, totalPayment: BigDecimal): Seq[PaymentSchedule] =
    (0 until request.term)
      .map(it =>
        (it, Instant.now.plus(Duration.ofDays(30 * it)), totalPayment / request.term))
      .map(it =>
        PaymentSchedule(
          it._1 + 1,
          Some(Timestamp(it._2.getEpochSecond, it._2.getNano)),
          totalPayment.toString,
          it._3.toString,
          (it._3 * it._1).toString,
          (it._3 * (request.term - it._1)).toString)
      )

  private def validateRequest(request: ScoringRequest): ZIO[Any, StatusException, ScoringRequest] =
    ZIO.succeed(request)
      .filterOrFail(_.birthdate.isDefined)(Status.INVALID_ARGUMENT)
      .filterOrFail(req =>
        scoringUtils.clientIsOlderThan(req.birthdate.get, 20))(Status.INVALID_ARGUMENT)
      .filterOrFail(req =>
        scoringUtils.clientIsYoungerThan(req.birthdate.get, 60))(Status.INVALID_ARGUMENT)
      .filterOrFail(_.employment.isDefined)(Status.INVALID_ARGUMENT)
      .filterOrFail(req =>
        (BigDecimal(req.employment.get.salary) * req.term) > BigDecimal(req.amount))(Status.INVALID_ARGUMENT)
      .filterOrFail(_.employment.get.workExperienceTotal > 12)(Status.INVALID_ARGUMENT)
      .filterOrFail(_.employment.get.workExperienceCurrent > 3)(Status.INVALID_ARGUMENT)
      .mapError(error => new StatusException(error))

  private def calculateRate(request: ScoringRequest): ZIO[Any, Nothing, BigDecimal] =
    ZIO.succeed(CreditPercentages.baseRate)
      .map(rate => if (request.employment.get.employmentStatus == EmploymentStatus.UNEMPLOYED) rate + 0.01 else rate)
      .map(rate => if (request.employment.get.employmentStatus == EmploymentStatus.BUSINESS_OWNER) rate + 0.03 else rate)
      .map(rate => if (PositionFilter.middleManagers.contains(request.employment.get.position)) rate - 0.02d else rate)
      .map(rate => if (PositionFilter.topManagers.contains(request.employment.get.position)) rate - 0.04d else rate)
      .map(rate => if (request.materialStatus == MaterialStatus.MARRIED) rate - 0.03 else rate)
      .map(rate => if (request.materialStatus == MaterialStatus.DIVORCED) rate + 0.01 else rate)
      .map(rate => if (request.dependentAmount > 1) rate + 0.01 else rate)
      .map(rate => if (isUserInActiveAge(request)) rate - 0.03 else rate)

  private def isUserInActiveAge(request: ScoringRequest): Boolean =
    (request.gender == Gender.FEMALE
      && scoringUtils.clientIsOlderThan(request.birthdate.get, 35)
      || request.gender == Gender.MALE
      && scoringUtils.clientIsOlderThan(request.birthdate.get, 30)
      && scoringUtils.clientIsYoungerThan(request.birthdate.get, 55))
}
