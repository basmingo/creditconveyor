package ru.neoflex.credit.conveyor.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import ru.neoflex.credit.conveyor.OfferProtoServiceHandler

import scala.concurrent.{ExecutionContextExecutor, Future}


class CreditConveyorServer(implicit actorSystem: ActorSystem) {
  implicit val executionContext: ExecutionContextExecutor =
    actorSystem.dispatcher

  val service: HttpRequest => Future[HttpResponse] =
    OfferProtoServiceHandler(new OfferProtoServiceImpl())

  def startServer: Future[Http.ServerBinding] =
    Http().newServerAt("127.0.0.1", 8080).bind(service)
}
