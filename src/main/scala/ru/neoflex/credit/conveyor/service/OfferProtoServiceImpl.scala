package ru.neoflex.credit.conveyor.service

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import ru.neoflex.credit.conveyor.{LoanRequest, LoanResponse, OfferProtoService}
import zio.prelude._

import java.util.UUID

class OfferProtoServiceImpl(implicit val mat: Materializer) extends OfferProtoService {
  import mat.executionContext
  def validateLoanRequest(request: LoanRequest): Validation[String, LoanRequest] = {
    for {
      name <- Validation.fromPredicateWith("Name is not valid")(request)(_.firstName.startsWith("A"))
      lastName <- Validation.fromPredicateWith("Last name is not valid")(name)(_.lastName.startsWith("A"))
    } yield lastName
  }

  override def makeOffers(request: LoanRequest) = {
    val a = for {
      req <- validateLoanRequest(request)
    } yield (req)
    println(a)

    Source(
      (1 to 5).map(a =>
        LoanResponse(
          id = UUID.randomUUID().toString,
          requestedAmount = "100",
          totalAmount = "1000",
          term = 12,
          monthlyPayment = "1",
          rate = "3",
          isInsuranceEnabled = true,
          isSalaryClient = true
        )
      )
    )
  }
}
