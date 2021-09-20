package teleporter.model

import io.circe.Decoder

import cats.Show
import cats.data.NonEmptyList
import cats.implicits._
import cats.kernel.Eq
import scala.reflect.TypeTest
import io.circe.Encoder
import io.circe.Json
import io.circe.Decoder
import io.circe.DecodingFailure

type ModelOrError[E, A] = Either[NonEmptyList[E], A]

enum Errors(messge: String):
  case Empty extends Errors("Empty name˝")
end Errors

extension [E](e: E) def errors[A] = e.pure[NonEmptyList]

extension [E, A](value: ModelOrError[E, A])
  def isNotError = !value.isError
  def isError = value.fold(_ => true, _ => false)

opaque type Name = String
object Name:
  import Errors._

  def apply(
      name: String
  ): ModelOrError[Errors, Name] =
    if name.isBlank then Empty.errors.asLeft
    else name.asRight

  given Show[Name] = Show.show(identity)

  given Encoder[Name] = Encoder.instance(Json.fromString)
  given Decoder[Name] = Decoder.instance(_.as[String](Decoder.decodeString))

end Name

opaque type Arg = String
object Arg:
  import Errors._

  def apply(arg: String): ModelOrError[Errors, Arg] =
    if arg.isBlank then Empty.errors.asLeft
    else arg.asRight

  given Show[Arg] = Show.show(identity)
  given Encoder[Arg] = Encoder.instance(Json.fromString)
  given Decoder[Arg] = Decoder.instance(_.as[String](Decoder.decodeString))

end Arg

case class Teleporter(name: Name, app: Name, args: List[Arg])

object Teleporter:

  def createRaw(name: String, app: String, args: List[String]) =
    (Name(name), Name(app), args.traverse(Arg(_))).parMapN(Teleporter.apply)

end Teleporter

case class TeleporterEnv(name: Name, teleporters: List[Teleporter])

enum Status:
  case Ok(result: String)
  case Error(message: String)
end Status
