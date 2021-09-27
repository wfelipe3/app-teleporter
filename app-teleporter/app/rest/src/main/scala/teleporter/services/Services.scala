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
given EntityDecoder[Task, TeleporterEnv] = jsonOf[Task, TeleporterEnv]

val helloWorldService = HttpRoutes
  .of[Task] { case GET -> Root / "hello" / name =>
    Ok(s"Hello, $name.")
  }

def openTeleporters(using Catalog[Task])(using TeleporterOpener[Task]) =
  HttpRoutes.of[Task] { case req @ POST -> Root / "teleporter" / "open" =>
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
    case req @ GET -> Root / "teleporter" :? SearchQueryParamMatcher(s) =>
      for
        found <- FuzzyFindTeleporterUseCase.search(s)
        ok <- Ok(found.asJson)
      yield ok
  }

def saveTeleporter(using Catalog[Task]) =
  HttpRoutes.of[Task] { case req @ POST -> Root / "teleporter" =>
    for
      teleporter <- req.as[Teleporter]
      status <- SaveTeleporterUseCase.save(teleporter)
      response <- status match
        case SaveTeleporterUseCase.SaveStatus.Ok => Ok()
        case SaveTeleporterUseCase.SaveStatus.AlreadyExists(name) =>
          BadRequest("teleporter already exists")
    yield response
  }

def openEnvironment(using Catalog[Task])(using TeleporterOpener[Task]) =
  HttpRoutes.of[Task] { case req @ POST -> Root / "env" / "open" =>
    for
      names <- req.as[List[Name]]
      opened <- EnvOpenerUseCase.openEnvs(names)
      ok <- Ok(opened.asJson)
    yield ok
  }

def openTeleportersInEnvironment(using Catalog[Task])(using
    TeleporterOpener[Task]
) =
  HttpRoutes.of[Task] { case req @ POST -> Root / "env" / env / "open" =>
    Name(env).fold(
      e => BadRequest("Invalid env name"),
      n => {
        for
          names <- req.as[List[Name]]
          opened <- EnvOpenerUseCase.openTeleporters(n, names)
          res <- opened match
            case EnvOpenerUseCase.OpenStatus.NotFound(name) =>
              NotFound(s"${name.show} not found")
            case EnvOpenerUseCase.OpenStatus.Opened(name) =>
              Ok(opened.asJson)
        yield res
      }
    )
  }

def saveEnv(using Catalog[Task]) =
  HttpRoutes.of[Task] { case req @ POST -> Root / "env" =>
    for
      env <- req.as[TeleporterEnv]
      status <- SaveEnvUseCase.save(env)
      response <- status match
        case SaveEnvUseCase.SaveStatus.Ok => Ok()
        case SaveEnvUseCase.SaveStatus.AlreadyExists(name) =>
          BadRequest("env already exists")
    yield response
  }

def searchEnvs(using Catalog[Task]) =
  HttpRoutes.of[Task] {
    case req @ GET -> Root / "env" :? SearchQueryParamMatcher(s) =>
      for
        found <- FuzzyFindEnvUseCase.search(s)
        ok <- Ok(found.asJson)
      yield ok
  }

def searchAll(using Catalog[Task]) =
  given Encoder[Teleporter | TeleporterEnv] = Encoder.instance {
    case t: Teleporter    => t.asJson
    case e: TeleporterEnv => e.asJson
  }
  HttpRoutes.of[Task] {
    case req @ GET -> Root / "fzf" :? SearchQueryParamMatcher(s) =>
      for
        found <- FuzzyFindAllUseCase.search(s)
        ok <- Ok(found.asJson)
      yield ok
  }
