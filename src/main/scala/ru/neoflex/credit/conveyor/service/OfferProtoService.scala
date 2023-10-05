package ru.neoflex.credit.conveyor.service

import io.grpc.{Status, StatusException}
import ru.neoflex.credit.conveyor.OfferProtoService.ZioOfferProtoService.OfferProtoService
import ru.neoflex.credit.conveyor.OfferProtoService.{LoanRequest, LoanResponse}
import zio.stream.ZStream
import zio.{Ref, ZIO, stream}

import java.util.UUID


object OfferProtoService extends OfferProtoService {
  def makeOffers(request: LoanRequest): stream.Stream[StatusException, LoanResponse] = {
    LoanValidator.validateLoanRequest(request).fold(
      error =>
        ZStream.fail(new StatusException(
          Status
            .INVALID_ARGUMENT
            .augmentDescription(error.head))),
      _ =>
        ZStream.fromIterable(1 to 10).map(it => LoanResponse(id = it.toString))
    )
  }
}
