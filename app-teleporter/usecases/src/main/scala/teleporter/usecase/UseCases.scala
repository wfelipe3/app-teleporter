package teleporter.usecase

import teleporter.model._
import teleporter.ports._
import cats.Monad
import cats.implicits._
import teleporter.usecase.OpenTeleportersUseCase.OpenStatus
import io.circe.Encoder
import io.circe.Json
import cats.Parallel
import teleporter.usecase.FuzzyFindAllUseCase

object OpenTeleportersUseCase:
  enum OpenStatus:
    case NotFound(name: Name)
    case Opened(name: Name, result: String)
    case Error(name: Name, error: String)
  end OpenStatus
  object OpenStatus:
    given Encoder[OpenStatus] = Encoder.instance {
      case NotFound(name) =>
        Json.obj(
          "name" -> Json.fromString(name.show),
          "status" -> Json.fromString("not-found")
        )
      case Opened(name, result) =>
        Json.obj(
          "name" -> Json.fromString(name.show),
          "status" -> Json.fromString("opened"),
          "result" -> Json.fromString(result)
        )
      case Error(name, result) =>
        Json.obj(
          "name" -> Json.fromString(name.show),
          "status" -> Json.fromString("error"),
          "result" -> Json.fromString(result)
        )
    }
  end OpenStatus

  def openTeleporters[F[_]: TeleporterOpener: Catalog: Monad: Parallel](
      names: List[Name]
  ) =
    for
      teleporters <- names.traverse(n => n.findTeleporter.map(f => n -> f))
      opened <- teleporters.parTraverse {
        case (name, None) => OpenStatus.NotFound(name).pure[F]
        case (name, Some(t)) =>
          t.open.map {
            case Status.Ok(result)    => OpenStatus.Opened(name, result)
            case Status.Error(result) => OpenStatus.Error(name, result)
          }
      }
    yield opened
end OpenTeleportersUseCase

object FuzzyFindTeleporterUseCase:
  def search[F[_]: Catalog: Monad](s: String) =
    Catalog[F].fuzzyFindTeleporter(s)
end FuzzyFindTeleporterUseCase

object SaveTeleporterUseCase:
  enum SaveStatus:
    case Ok
    case AlreadyExists(name: Name)
  end SaveStatus

  def save[F[_]: Catalog: Monad](teleporter: Teleporter) =
    teleporter.name.findTeleporter >>= { found =>
      found.fold(teleporter.save.map(_ => SaveStatus.Ok))(_ =>
        SaveStatus.AlreadyExists(teleporter.name).pure[F]
      )
    }

end SaveTeleporterUseCase

object SaveEnvUseCase:
  enum SaveStatus:
    case Ok
    case AlreadyExists(name: Name)
  end SaveStatus

  def save[F[_]: Catalog: Monad](env: TeleporterEnv) =
    env.name.findEnv >>= { found =>
      found.fold(env.save.map(_ => SaveStatus.Ok))(_ =>
        SaveStatus.AlreadyExists(env.name).pure[F]
      )
    }
end SaveEnvUseCase

object EnvOpenerUseCase:
  enum OpenStatus:
    case NotFound(name: Name)
    case Opened(name: Name)
  end OpenStatus
  object OpenStatus:
    given Encoder[OpenStatus] = Encoder.instance {
      case OpenStatus.NotFound(name) =>
        Json.obj(
          "name" -> Json.fromString(name.show),
          "status" -> Json.fromString("not-found")
        )
      case OpenStatus.Opened(name) =>
        Json.obj(
          "name" -> Json.fromString(name.show),
          "status" -> Json.fromString("opened")
        )
    }
  end OpenStatus

  def openEnvs[F[_]: Catalog: TeleporterOpener: Monad: Parallel](
      names: List[Name]
  ) =
    for
      envs <- names.traverse(n => n.findEnv.map(e => n -> e))
      opened <- envs.parTraverse {
        case (name, None)      => OpenStatus.NotFound(name).pure[F]
        case (name, Some(env)) => env.open.map(_ => OpenStatus.Opened(name))
      }
    yield opened

  def openTeleporters[F[_]: Catalog: TeleporterOpener: Monad: Parallel](
      env: Name,
      teleporters: List[Name]
  ) =
    for
      e <- env.findEnv
      opened <- e.fold(OpenStatus.NotFound(env).pure[F]) { env =>
        env.teleporters
          .foldLeft(List.empty[Teleporter]) { (found, t) =>
            if teleporters.exists(te => t.name == te) then t +: found
            else found
          }
          .parTraverse(_.open)
          .as(OpenStatus.Opened(env.name))
      }
    yield opened
end EnvOpenerUseCase

object FuzzyFindEnvUseCase:
  def search[F[_]: Catalog: Monad](s: String) =
    Catalog[F].fuzzyFindEnv(s)
end FuzzyFindEnvUseCase

object FuzzyFindAllUseCase:
  def search[F[_]: Catalog: Monad](s: String) =
    Catalog[F].fuzzyFindAll(s)
end FuzzyFindAllUseCase
