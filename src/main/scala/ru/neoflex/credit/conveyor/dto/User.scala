package ru.neoflex.credit.conveyor.dto

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class User(name: String, email: String, salary: Int)
object User {
  implicit val decoder: JsonDecoder[User] = DeriveJsonDecoder.gen[User]
  implicit val encoder: JsonEncoder[User] = DeriveJsonEncoder.gen[User]
}
