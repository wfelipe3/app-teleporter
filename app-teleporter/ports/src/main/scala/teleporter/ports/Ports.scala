package teleporter.ports

import teleporter.model._

trait Catalog[F[_]]:

  extension (name: Name)
    def findTeleporter: F[Option[Teleporter]]
    def findEnv: F[Option[TeleporterEnv]]

  extension (teleporter: Teleporter) def save: F[Unit]
  extension (env: TeleporterEnv) def save: F[Unit]

  def fuzzyFindTeleporter(key: String): F[List[Teleporter]]
  def fuzzyFindEnv(key: String): F[List[TeleporterEnv]]
  def fuzzyFindAll(key: String): F[List[Teleporter | TeleporterEnv]]

end Catalog

object Catalog:
  def apply[F[_]: Catalog] = summon[Catalog[F]]
end Catalog

trait TeleporterOpener[F[_]]:
  extension (teleporter: Teleporter) def open: F[Status]
  extension (env: TeleporterEnv) def open: F[List[Status]]
end TeleporterOpener
