package teleporter.experiments

import munit.*
import teleporter.ports.Catalog
import teleporter.model._
import cats.implicits._
import cats.Id
import zio.Task
import zio.Ref
import zio.interop.catz._
import zio.interop.catz.implicits._
import cats.effect.IO
import Trie._
import zio.console._
import scala.util.chaining._

class ExperimentsTest extends munit.ZSuite:
  testZ("Catalog exists") {
    for
      cat <- catalog
      given Catalog[Task] = cat
      n <- Task.fromEither(
        Name("hello")
          .leftMap(_ => new Exception("error"))
      )
      t <- Task
        .fromEither(
          Teleporter
            .createRaw("hello", "he", "world".pure[List])
            .leftMap(_ => new Exception("error"))
        )
      _ <- t.save
      found <- n.findTeleporter
      foundFzf <- cat.fuzzyFindAll("hl")
      _ <- Task.effect(println(foundFzf))
    yield assert(foundFzf == List(t))
  }
end ExperimentsTest

val catalog =
  for
    teleporters <- Ref.make(Map.empty[Name, Teleporter])
    envs <- Ref.make(Map.empty[Name, TeleporterEnv])
    trieTl <- Ref.make(Trie.empty)
    trieEnvs <- Ref.make(Trie.empty)
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
        yield ()

    extension (env: TeleporterEnv)
      def save: Task[Unit] =
        for
          _ <- envs.update(_ + (env.name -> env))
          _ <- trieEnvs.update(_.add(env.name.show))
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
  }
