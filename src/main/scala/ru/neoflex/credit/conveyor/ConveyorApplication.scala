package ru.neoflex.credit.conveyor

import ru.neoflex.credit.conveyor.service.OfferProtoService
import scalapb.zio_grpc.{ServerMain, ServiceList}
import zio.{Ref, ZIO}

object ConveyorApplication extends ServerMain {
  override def port: Int = 8090

  val s: ZIO[Any, Throwable, OfferProtoService.type] = ZIO.from(OfferProtoService)
  override def services: ServiceList[Any] = ServiceList.addZIO(s)
}
