val scala3Version = "3.0.2"

ThisBuild / scalacOptions ++= Seq()

val zioVersion = "1.0.9"
def zio(version: String = zioVersion) = "dev.zio" %% "zio" % version
def zioStreams(version: String = zioVersion) =
  "dev.zio" %% "zio-streams" % version
def zioLogging(version: String = "0.5.11") =
  Seq(
    "dev.zio" %% "zio-logging" % version,
    "dev.zio" %% "zio-logging-slf4j" % version,
    "org.slf4j" % "slf4j-api" % "1.7.32",
    "net.logstash.logback" % "logstash-logback-encoder" % "4.11"
  )

def zioConfigCore(version: String = "1.0.6") =
  "dev.zio" %% "zio-config" % version
def zioConfigMagnolia(version: String = "1.0.6") =
  "dev.zio" %% "zio-config-magnolia" % version
def zioConfigTypesafe(version: String = "1.0.6") =
  "dev.zio" %% "zio-config-typesafe" % version
def zioConfig(version: String = "1.0.6") = Seq(
  zioConfigCore(version),
  zioConfigTypesafe(version)
)
def zioTest(version: String = zioVersion) = "dev.zio" %% "zio-test" % version
def zioSbtTest(version: String = zioVersion) =
  "dev.zio" %% "zio-test-sbt" % version % "test"
def zioMagnoliaTest(version: String = zioVersion) =
  "dev.zio" %% "zio-test-magnolia" % version % "test"
def zioTestAll(version: String = zioVersion) =
  Seq(zioTest(version), zioSbtTest(version), zioMagnoliaTest(version))
val cats = "org.typelevel" %% "cats-core" % "2.6.1"
val zioCatsInterop = "dev.zio" %% "zio-interop-cats" % "3.1.1.0"
val monocle = "dev.optics" %% "monocle-core" % "3.0.0-RC2"
val doobieCore = "org.tpolecat" %% "doobie-core" % "1.0.0-M5"
val doobieHikari = "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC1"
val sqlLite = "org.xerial" % "sqlite-jdbc" % "3.36.0.1"
val db = Seq(doobieCore, doobieHikari, sqlLite)
val flyway = "org.flywaydb" % "flyway-core" % "7.2.0"
val catsLogging = Seq(
  "org.slf4j" % "slf4j-api" % "1.7.32",
  // "org.slf4j" % "slf4j-simple" % "1.7.32",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.typelevel" %% "log4cats-slf4j" % "2.1.1",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.11"
)
def circe(version: String = "0.14.1") = Seq(
  "io.circe" %% "circe-core" % version,
  "io.circe" %% "circe-generic" % version,
  "io.circe" %% "circe-parser" % version,
  "io.circe" %% "circe-yaml" % "0.14.0"
)
def clipp(version: String = "0.6.1") = Seq(
  "io.github.vigoo" %% "clipp-core" % version,
  "io.github.vigoo" %% "clipp-zio" % version,
  "io.github.vigoo" %% "clipp-cats-effect3" % version
)
def catsEffect(version: String = "3.2.0") = Seq(
  "org.typelevel" %% "cats-effect" % version
)
def munit(version: String = "0.7.29") = Seq(
  "org.scalameta" %% "munit" % version % Test,
  "org.scalameta" %% "munit-scalacheck" % version % Test,
  "org.typelevel" %% "munit-cats-effect-3" % "1.0.3" % Test,
  "com.github.poslegm" %% "munit-zio" % "0.0.2" % Test
)

def http4s(http4sVersion: String = "1.0.0-M25") = Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-server" % http4sVersion,
  "org.http4s" %% "http4s-prometheus-metrics" % http4sVersion
)

val catsParse = "org.typelevel" %% "cats-parse" % "0.3.4"

ThisBuild / assemblyMergeStrategy := {
  case x: String =>
    if (x.startsWith("scala"))
      MergeStrategy.last
    else if (x.endsWith("MANIFEST.MF"))
      MergeStrategy.discard
    else MergeStrategy.deduplicate
  case PathList("META-INF", xs @ _*) =>
    (xs map { _.toLowerCase }) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) |
          ("dependencies" :: Nil) | ("MANIFEST.MF" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs)
          if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.deduplicate
    }
  case _ => MergeStrategy.deduplicate
}

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala3-simple",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"
  )

lazy val model = project
  .in(file("model"))
  .settings(
    scalaVersion := scala3Version,
    libraryDependencies += zio(),
    libraryDependencies += zioStreams(),
    libraryDependencies ++= zioLogging(),
    libraryDependencies ++= zioConfig(),
    libraryDependencies += cats,
    libraryDependencies ++= zioTestAll(),
    libraryDependencies += zioCatsInterop,
    libraryDependencies += monocle,
    libraryDependencies ++= circe(),
    libraryDependencies ++= munit(),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    testFrameworks += new TestFramework("munit.Framework")
  )

lazy val ports = project
  .in(file("ports"))
  .dependsOn(model)
  .settings(
    scalaVersion := scala3Version,
    libraryDependencies += zio(),
    libraryDependencies += zioStreams(),
    libraryDependencies ++= zioLogging(),
    libraryDependencies ++= zioConfig(),
    libraryDependencies += cats,
    libraryDependencies ++= zioTestAll(),
    libraryDependencies += zioCatsInterop,
    libraryDependencies ++= catsEffect(),
    libraryDependencies += monocle,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val experiments = project
  .in(file("experiments"))
  .dependsOn(model, ports)
  .settings(
    resolvers += Resolver.sonatypeRepo("snapshots"),
    scalaVersion := scala3Version,
    libraryDependencies += zio(),
    libraryDependencies += zioStreams(),
    libraryDependencies ++= zioLogging(),
    libraryDependencies ++= zioConfig(),
    libraryDependencies += cats,
    libraryDependencies ++= zioTestAll(),
    libraryDependencies += zioCatsInterop,
    libraryDependencies += monocle,
    libraryDependencies += flyway,
    libraryDependencies ++= catsLogging,
    libraryDependencies ++= circe(),
    libraryDependencies ++= munit(),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    testFrameworks += new TestFramework("munit.Framework")
  )

lazy val jsonCatalogAdapter = project
  .in(file("adapters/json-catalog-adapter"))
  .dependsOn(model, ports)
  .settings(
    resolvers += Resolver.sonatypeRepo("snapshots"),
    scalaVersion := scala3Version,
    libraryDependencies += zio(),
    libraryDependencies += zioStreams(),
    libraryDependencies ++= zioLogging(),
    libraryDependencies ++= zioConfig(),
    libraryDependencies += cats,
    libraryDependencies ++= zioTestAll(),
    libraryDependencies += zioCatsInterop,
    libraryDependencies += monocle,
    libraryDependencies ++= catsLogging,
    libraryDependencies ++= circe(),
    libraryDependencies ++= munit(),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    testFrameworks += new TestFramework("munit.Framework")
  )

lazy val macTeleportOpenerAdapter = project
  .in(file("adapters/mac-teleport-opener-adapter"))
  .dependsOn(model, ports)
  .settings(
    resolvers += Resolver.sonatypeRepo("snapshots"),
    scalaVersion := scala3Version,
    libraryDependencies += zio(),
    libraryDependencies += zioStreams(),
    libraryDependencies ++= zioLogging(),
    libraryDependencies ++= zioConfig(),
    libraryDependencies += cats,
    libraryDependencies ++= zioTestAll(),
    libraryDependencies += zioCatsInterop,
    libraryDependencies += monocle,
    libraryDependencies ++= catsLogging,
    libraryDependencies ++= circe(),
    libraryDependencies ++= munit(),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    testFrameworks += new TestFramework("munit.Framework")
  )

lazy val useCases = project
  .in(file("usecases"))
  .dependsOn(model, ports)
  .settings(
    scalaVersion := scala3Version,
    libraryDependencies += zio(),
    libraryDependencies += zioStreams(),
    libraryDependencies ++= zioLogging(),
    libraryDependencies ++= zioConfig(),
    libraryDependencies += cats,
    libraryDependencies ++= zioTestAll(),
    libraryDependencies += zioCatsInterop,
    libraryDependencies += monocle,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val rest = project
  .in(file("app/rest"))
  .dependsOn(
    model,
    ports,
    useCases,
    macTeleportOpenerAdapter,
    jsonCatalogAdapter
  )
  .settings(
    mainClass / assembly := file("teleport.app.App"),
    scalaVersion := scala3Version,
    libraryDependencies += zio(),
    libraryDependencies += zioStreams(),
    libraryDependencies ++= zioLogging(),
    libraryDependencies ++= zioConfig(),
    libraryDependencies += cats,
    libraryDependencies ++= zioTestAll(),
    libraryDependencies += zioCatsInterop,
    libraryDependencies ++= clipp(),
    libraryDependencies += catsParse,
    libraryDependencies ++= munit(),
    libraryDependencies ++= http4s(),
    libraryDependencies ++= circe(),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val console = project
  .in(file("app/console"))
  .dependsOn(
    model,
    ports,
    useCases,
    macTeleportOpenerAdapter,
    jsonCatalogAdapter
  )
  .settings(
    mainClass / assembly := file("teleport.App"),
    scalaVersion := scala3Version,
    libraryDependencies += zio(),
    libraryDependencies += zioStreams(),
    libraryDependencies ++= zioLogging(),
    libraryDependencies ++= zioConfig(),
    libraryDependencies += cats,
    libraryDependencies ++= zioTestAll(),
    libraryDependencies += zioCatsInterop,
    libraryDependencies += monocle,
    libraryDependencies ++= clipp(),
    libraryDependencies += catsParse,
    libraryDependencies ++= munit(),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    testFrameworks += new TestFramework("munit.Framework")
  )
