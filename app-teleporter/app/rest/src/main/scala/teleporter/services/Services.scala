package teleporter.services

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.HttpRoutes
import org.http4s.dsl._
import org.http4s.implicits._
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.circe._
import zio.Task
import zio.interop.catz._
import zio.interop.catz.implicits._
import teleporter.model._
import cats.implicits._
import teleporter.usecase._
import teleporter.ports._

private val dsl = Http4sDsl[Task]
import dsl._
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.Status.ClientError

given EntityDecoder[Task, List[Name]] = jsonOf[Task, List[Name]]
given EntityDecoder[Task, Teleporter] = jsonOf[Task, Teleporter]

val helloWorldService = HttpRoutes
  .of[Task] { case GET -> Root / "hello" / name =>
    Ok(s"Hello, $name.")
  }

def openTeleporters(using Catalog[Task])(using TeleporterOpener[Task]) =
  HttpRoutes.of[Task] { case req @ POST -> Root / "teleporters" / "open" =>
    for
      names <- req.as[List[Name]]
      opened <- OpenTeleportersUseCase.openTeleporters(names)
      ok <- Ok(opened.asJson)
    yield ok
  }

object SearchQueryParamMatcher
    extends QueryParamDecoderMatcher[String]("search")

def searchTeleporters(using Catalog[Task]) =
  HttpRoutes.of[Task] {
    case req @ GET -> Root / "teleporters" :? SearchQueryParamMatcher(s) =>
      for
        found <- FuzzyFindTeleporterUseCase.search(s)
        ok <- Ok(found.asJson)
      yield ok
  }

def saveTeleporter(using Catalog[Task]) =
  HttpRoutes.of[Task] { case req @ POST -> Root / "teleporters" =>
    for
      teleporter <- req.as[Teleporter]
      status <- SaveTeleporterUseCase.save(teleporter)
      response <- status match
        case SaveTeleporterUseCase.SaveStatus.Ok => Ok()
        case SaveTeleporterUseCase.SaveStatus.AlreadyExists(name) =>
          BadRequest("teleporter already exists")
    yield response
  }
