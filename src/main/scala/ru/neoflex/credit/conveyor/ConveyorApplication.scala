package ru.neoflex.credit.conveyor

import ru.neoflex.credit.conveyor.OfferProtoService.ZioOfferProtoService.OfferProtoService
import ru.neoflex.credit.conveyor.service.OfferServiceImpl
import ru.neoflex.credit.conveyor.service.utils.{LoanCalculationUtils, LoanCalculationUtilsImpl}
import scalapb.zio_grpc.{ServerMain, ServiceList}
import zio.ZIO

object ConveyorApplication extends ServerMain {
  override def port: Int = 8090

  val offerService: ZIO[LoanCalculationUtils, Nothing, OfferProtoService] = for {
    utils <- ZIO.service[LoanCalculationUtils]
  } yield OfferServiceImpl(utils)

  override def services: ServiceList[Any] = ServiceList.addZIO(offerService)
    .provide(LoanCalculationUtilsImpl.layer)
}
