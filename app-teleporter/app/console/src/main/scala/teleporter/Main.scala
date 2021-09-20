package teleporter

import zio.App
import teleporter.ports._
import teleporter.model._
import teleporter.adapter.catalog._
import teleporter.adapter.opener._
import java.nio.file.Paths
import zio.Task
import zio.ZIO
import cats.implicits._
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.ExitCode

object Main extends App:

  def run(args: List[String]) =
    (for
      cat <- jsonCatalogLive(Paths.get("db.json"))
      given TeleporterOpener[Task] = processTeleporterOpener()
      given Catalog[Task] = cat
      tls <- cat.fuzzyFindTeleporter("hl")
      _ <- tls.traverse(_.open)
      newTls <- ZIO.fromEither(
        Teleporter.createRaw(
          "google",
          "open",
          List("-a", "safari", "http://google.com?q=hello")
        )
      )
      _ <- newTls.save
      goo <- cat.fuzzyFindTeleporter("goo")
      _ <- goo.traverse(_.open)
    yield ()).exitCode

end Main
