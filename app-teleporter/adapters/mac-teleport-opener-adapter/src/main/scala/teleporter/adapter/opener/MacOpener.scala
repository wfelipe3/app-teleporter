package teleporter.adapter.opener

import zio.Task
import teleporter.ports.TeleporterOpener
import teleporter.model._
import scala.sys.process._
import cats.implicits._
import java.io.StringWriter
import java.io.PrintWriter
import zio.interop.catz._
import zio.interop.catz.implicits._

private def throwableToString(t: Throwable) =
  val s = new StringWriter
  t.printStackTrace(new PrintWriter(s))
  s.toString

def processTeleporterOpener() = new TeleporterOpener[Task] {
  extension (teleporter: Teleporter)
    def open: Task[Status] =
      val t = for
        command <- Task.succeed(
          s"${teleporter.app.show} ${teleporter.args.map(_.show).mkString(" ")}"
        )
        result <- Task.effect {
          command.!!
        }
      yield result
      t.map(Status.Ok.apply)
        .catchAll { e =>
          Status.Error(throwableToString(e)).pure[Task]
        }

  extension (env: TeleporterEnv)
    def open: Task[List[Status]] =
      env.teleporters.parTraverse(_.open)
}
