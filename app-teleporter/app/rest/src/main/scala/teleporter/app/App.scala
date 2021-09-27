package teleporter

import zio.console._
import org.http4s.HttpRoutes
import org.http4s.dsl._
import org.http4s.implicits._
import org.http4s.blaze.server._
import scala.concurrent.ExecutionContext.global
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.Task
import teleporter.services.{
  helloWorldService,
  openTeleporters,
  searchTeleporters,
  saveTeleporter,
  openEnvironment,
  searchEnvs,
  openTeleportersInEnvironment,
  searchAll,
  saveEnv
}
import cats.implicits._
import teleporter.ports._
import teleporter.adapter.catalog.jsonCatalogLive
import teleporter.adapter.opener.processTeleporterOpener
import java.nio.file.Paths

object Main extends zio.App:

  def run(args: List[String]) =
    val server = for
      cat <- jsonCatalogLive(Paths.get("db.json"))
      given Catalog[Task] = cat
      given TeleporterOpener[Task] = processTeleporterOpener()
      s <- BlazeServerBuilder[Task](global)
        .bindHttp(8080, "localhost")
        .withHttpApp(
          (helloWorldService
            <+> openTeleporters
            <+> searchTeleporters
            <+> saveTeleporter
            <+> openEnvironment
            <+> searchEnvs
            <+> openTeleportersInEnvironment
            <+> searchAll
            <+> saveEnv).orNotFound
        )
        .serve
        .compile
        .drain
    yield s

    server.exitCode

end Main
