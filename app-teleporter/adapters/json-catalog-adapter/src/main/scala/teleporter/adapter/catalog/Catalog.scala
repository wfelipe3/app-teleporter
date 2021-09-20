package teleporter.adapter.catalog

import teleporter.ports.Catalog
import teleporter.model._
import cats.implicits._
import cats.Id
import zio.Task
import zio.Ref
import zio.interop.catz._
import zio.interop.catz.implicits._
import cats.effect.IO
import teleporter.adapter.trie.Trie
import scala.util.chaining._
import java.nio.file.Path
import java.nio.file.Files
import scala.jdk.CollectionConverters._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import java.nio.file.StandardOpenOption

private case class DB(
    teleporters: List[Teleporter],
    envs: List[TeleporterEnv]
)

private def load(path: Path) =
  Task
    .effect {
      Files.readAllLines(path).asScala.mkString("")
    }
    .flatMap { content =>
      Task.fromEither(decode[DB](content))
    }

def jsonCatalogLive(path: Path) =
  for
    db <- load(path)
    teleporters <- Ref.make(db.teleporters.map(t => t.name -> t).toMap)
    envs <- Ref.make(db.envs.map(e => e.name -> e).toMap)
    trieTl <- Ref.make(
      db.teleporters.foldLeft(Trie.empty)((t, tl) => t.add(tl.name.show))
    )
    trieEnvs <- Ref.make(
      db.envs.foldLeft(Trie.empty)((t, e) => t.add(e.name.show))
    )
  yield new Catalog[Task] {

    extension (name: Name)
      def findTeleporter: Task[Option[Teleporter]] =
        for values <- teleporters.get
        yield values.get(name)

      def findEnv: Task[Option[TeleporterEnv]] =
        for values <- envs.get
        yield values.get(name)

    extension (teleporter: Teleporter)
      def save: Task[Unit] =
        for
          _ <- teleporters.update(_ + (teleporter.name -> teleporter))
          _ <- trieTl.update(_.add(teleporter.name.show))
          _ <- saveDb
        yield ()

    extension (env: TeleporterEnv)
      def save: Task[Unit] =
        for
          _ <- envs.update(_ + (env.name -> env))
          _ <- trieEnvs.update(_.add(env.name.show))
          _ <- saveDb
        yield ()

    def fuzzyFindTeleporter(key: String): Task[List[Teleporter]] =
      for
        tls <- trieTl.get
        teleports <- tls
          .fzf(key)
          .toList
          .traverse(Name.apply)
          .fold(_ => List.empty, identity)
          .traverse(_.findTeleporter)
      yield teleports.filter(_.isDefined).map(_.get)

    def fuzzyFindEnv(key: String): Task[List[TeleporterEnv]] =
      for
        tls <- trieEnvs.get
        teleports <- tls
          .fzf(key)
          .toList
          .traverse(Name.apply)
          .fold(_ => List.empty, identity)
          .traverse(_.findEnv)
      yield teleports.filter(_.isDefined).map(_.get)

    def fuzzyFindAll(key: String): Task[List[Teleporter | TeleporterEnv]] =
      for
        tls <- fuzzyFindTeleporter(key)
        envs <- fuzzyFindEnv(key)
      yield tls
        .foldRight(List.empty[Teleporter | TeleporterEnv])(_ +: _)
        .pipe { all =>
          envs.foldRight(all)(_ +: _)
        }

    private def saveDb =
      for
        tls <- teleporters.get
        e <- envs.get
        db = DB(
          teleporters = tls.values.toList,
          envs = e.values.toList
        ).asJson.spaces2
        _ <- Task.effect {
          Files.writeString(path, db, StandardOpenOption.TRUNCATE_EXISTING)
        }
      yield ()
  }
