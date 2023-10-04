package ru.neoflex.credit.conveyor

import akka.actor.ActorSystem
import ru.neoflex.credit.conveyor.service.CreditConveyorServer

import scala.concurrent.ExecutionContextExecutor

object ConveyorApplication extends App {
  implicit val system: ActorSystem = ActorSystem("my-system")

  val creditConveyorServer = new CreditConveyorServer()
  creditConveyorServer.startServer
}
