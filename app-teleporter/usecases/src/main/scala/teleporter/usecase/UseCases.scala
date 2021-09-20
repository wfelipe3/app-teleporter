package teleporter.usecase

import teleporter.model._
import teleporter.ports._
import cats.Monad
import cats.implicits._
import teleporter.usecase.OpenTeleportersUseCase.OpenStatus
import io.circe.Encoder
import io.circe.Json

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

  def openTeleporters[F[_]: TeleporterOpener: Catalog: Monad](
      names: List[Name]
  ) =
    for
      teleporters <- names.traverse(n => n.findTeleporter.map(f => n -> f))
      opened <- teleporters.traverse {
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
