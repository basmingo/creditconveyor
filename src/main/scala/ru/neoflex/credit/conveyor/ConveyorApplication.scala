package ru.neoflex.credit.conveyor

import ru.neoflex.credit.conveyor.OfferProtoService.ZioOfferProtoService.OfferProtoService
import ru.neoflex.credit.conveyor.ScoringProtoService.ZioScoringProtoService.ScoringProtoService
import ru.neoflex.credit.conveyor.service.{LoanValidator, LoanValidatorImpl, OfferServiceImpl, ScoringServiceImpl}
import ru.neoflex.credit.conveyor.service.utils.{
  LoanCalculationUtils,
  LoanCalculationUtilsImpl,
  ScoringUtils,
  ScoringUtilsImpl
}
import scalapb.zio_grpc.{ServerMain, ServiceList}
import zio.ZIO

object ConveyorApplication extends ServerMain {
  override def port: Int = 8090

  val offerService: ZIO[LoanCalculationUtils with LoanValidator, Nothing, OfferProtoService] = for {
    utils     <- ZIO.service[LoanCalculationUtils]
    validator <- ZIO.service[LoanValidator]
  } yield OfferServiceImpl(utils, validator)

  val scoringService: ZIO[ScoringUtils with LoanCalculationUtils, Nothing, ScoringProtoService] = for {
    scoring     <- ZIO.service[ScoringUtils]
    calculation <- ZIO.service[LoanCalculationUtils]
  } yield ScoringServiceImpl(scoring, calculation)

  override def services: ServiceList[Any] = ServiceList
    .addZIO(
      offerService
        .provide(LoanCalculationUtilsImpl.layer, LoanValidatorImpl.layer, ScoringUtilsImpl.layer)
    )
    .addZIO(
      scoringService
        .provide(ScoringUtilsImpl.layer, LoanCalculationUtilsImpl.layer)
    )
}
