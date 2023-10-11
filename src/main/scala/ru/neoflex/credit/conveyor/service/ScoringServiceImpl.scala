package ru.neoflex.credit.conveyor.service

import io.grpc.StatusException
import ru.neoflex.credit.conveyor.ScoringProtoService.{Credit, EmploymentStatus, Gender, MaterialStatus, Position, ScoringRequest}
import ru.neoflex.credit.conveyor.ScoringProtoService.ZioScoringProtoService.ScoringProtoService
import ru.neoflex.credit.conveyor.config.{CreditPercentages, PositionFilter}
import ru.neoflex.credit.conveyor.service.utils.ScoringUtils
import zio.{IO, ZIO}

case class ScoringServiceImpl(scoringUtils: ScoringUtils) extends ScoringProtoService {
  override def scoreData(request: ScoringRequest): IO[StatusException, Credit] = {
    println(request.employment.get.employmentStatus)
    println(request.employment.get.employmentStatus.getClass)
    println(PositionFilter.topManagers.contains(request.employment.get.position))
    ZIO.succeed(Credit())
  }

  def validateRequest(request: ScoringRequest): Option[ScoringRequest] = Some(request)
    .filter(_.birthdate.isDefined)
    .filter(req => scoringUtils.clientIsOlderThan(req.birthdate.get, 20))
    .filter(req => scoringUtils.clientIsYoungerThan(req.birthdate.get, 60))
    .filter(_.employment.isDefined)
    .filter(req => (BigDecimal(req.employment.get.salary) * req.term) > BigDecimal(req.amount))
    .filter(_.employment.get.workExperienceTotal > 12)
    .filter(_.employment.get.workExperienceCurrent > 3)

  def calculateRate(request: ScoringRequest): Option[BigDecimal] = {
    Some(CreditPercentages.baseRate)
      .map(rate => if (request.employment.get.employmentStatus == EmploymentStatus.UNEMPLOYED) rate + 0.01 else rate)
      .map(rate => if (request.employment.get.employmentStatus == EmploymentStatus.BUSINESS_OWNER) rate + 0.03 else rate)
      .map(rate => if (PositionFilter.middleManagers.contains(request.employment.get.position)) rate - 0.02d else rate)
      .map(rate => if (PositionFilter.topManagers.contains(request.employment.get.position)) rate - 0.04d else rate)
      .map(rate => if (request.materialStatus == MaterialStatus.MARRIED) rate - 0.03 else rate)
      .map(rate => if (request.materialStatus == MaterialStatus.DIVORCED) rate + 0.01 else rate)
      .map(rate => if (request.dependentAmount > 1) rate + 0.01 else rate)
      .map(rate => if (isUserInActiveAge(request)) rate - 0.03 else rate)
  }

  private def isUserInActiveAge(request: ScoringRequest): Boolean =
    (request.gender == Gender.FEMALE
      && scoringUtils.clientIsOlderThan(request.birthdate.get, 35)
      || request.gender == Gender.MALE
      && scoringUtils.clientIsOlderThan(request.birthdate.get, 30)
      && scoringUtils.clientIsYoungerThan(request.birthdate.get, 55))
}
