package ru.neoflex.credit.conveyor.service

import ru.neoflex.credit.conveyor.OfferProtoService.LoanRequest
import ru.neoflex.credit.conveyor.service.utils.{ScoringUtils, ScoringUtilsImpl}
import zio.prelude.Validation
import zio.prelude.ZValidation.Failure
import zio.{Chunk, NonEmptyChunk, ULayer, ZIO, ZLayer}

import java.time.Instant

trait LoanValidator {
  def validateLoanRequest(request: LoanRequest): Validation[String, LoanRequest]
}

case class LoanValidatorImpl(scoringUtils: ScoringUtils) extends LoanValidator {
  def validateLoanRequest(request: LoanRequest): Validation[String, LoanRequest] = {
    for {
      name <- validateName(request)
      lastName <- validateLastName(name)
      email <- validateEmail(lastName)
      birthday <- validateBirthday(email)
      series <- validatePassportSeries(birthday)
      number <- validatePassportNumber(series)
      term <- validateTerm(number)
      result <- validateAmount(term)
    } yield result
  }

  private def validateName(request: LoanRequest): Validation[String, LoanRequest] = {
    val name: String = request.firstName
    Validation.fromPredicateWith("name is not valid")(request)(_ =>
      name(0).isUpper && name.length >= 2 && name.length < 30)
  }

  private def validateLastName(request: LoanRequest): Validation[String, LoanRequest] = {
    val lastName: String = request.lastName
    Validation.fromPredicateWith("last name is not valid")(request)(_ =>
      lastName(0).isUpper && lastName.length >= 2 && lastName.length < 30)
  }

  private def validateEmail(request: LoanRequest): Validation[String, LoanRequest] = {
    val email = request.email.split('@')
    Validation.fromPredicateWith("email is not valid")(request)(_ =>
      email.length == 2 && email(1).split('.').length == 2)
  }

  private def validateAmount(request: LoanRequest): Validation[String, LoanRequest] = {
    val amount = request.amount
    Validation.fromPredicateWith("amount less than 10_000")(request)(_ =>
      amount.forall(_.isDigit) && BigDecimal(amount) >= 10_000L)
  }

  private def validateBirthday(request: LoanRequest): Validation[String, LoanRequest] = {
    request.birthday match {
      case Some(date) =>
        Validation.fromPredicateWith("birthday is not valid")(request)(_ =>
          scoringUtils.clientIsYoungerThan(date, 18))
      case _ => Failure(Chunk.empty, NonEmptyChunk("no birthday is presented"))
    }
  }

  private def validatePassportSeries(request: LoanRequest): Validation[String, LoanRequest] =
    Validation.fromPredicateWith("passport series can consists only from 4 digits")(request)(_ =>
      request.passportSeries.toString.length == 4)

  private def validatePassportNumber(request: LoanRequest): Validation[String, LoanRequest] =
    Validation.fromPredicateWith("passport number can consists only from 6 digits")(request)(_ =>
      request.passportNumber.toString.length == 6)

  private def validateTerm(request: LoanRequest): Validation[String, LoanRequest] =
    Validation.fromPredicateWith("term should be more than 0")(request)(_ =>
      request.term > 0)
}

object LoanValidatorImpl {
  val layer: ZLayer[ScoringUtils, Nothing, LoanValidatorImpl] = ZLayer {
    for {
      utils <- ZIO.service[ScoringUtils]
    } yield LoanValidatorImpl(utils)
  }
}