name := "graduation-project"

version := "0.1"

lazy val Version = new {
  val scala = "2.13.6"
  val http4s = "0.23.0"
  val cats = new {
    val core = "2.7.0"
    val effect = "3.3.11"
  }
  val monocle = "2.1.0"
  val circe = "0.14.1"
  val tapir = "1.0.0-M6"
  val silencer = "1.7.7"
  val doobie = "1.0.0-RC1"
}


lazy val scalaDeps = Seq(
  "org.scala-lang" % "scala-compiler" % Version.scala,
  "org.scala-lang" % "scala-reflect" % Version.scala
)

lazy val catsDeps = Seq(
  "org.typelevel" %% "cats-core" % Version.cats.core,
  "org.typelevel" %% "cats-kernel" % Version.cats.core,
  "org.typelevel" %% "cats-effect" % Version.cats.effect,
  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test
)

lazy val http4sDeps = Seq(
  "org.http4s" %% "http4s-dsl" % Version.http4s,
  "org.http4s" %% "http4s-blaze-server" % Version.http4s,
  "org.http4s" %% "http4s-blaze-client" % Version.http4s,
  "org.http4s" %% "http4s-circe" % Version.http4s
)

lazy val circeDeps = Seq(
  "io.circe" %% "circe-core" % Version.circe,
  "io.circe" %% "circe-parser" % Version.circe,
  "io.circe" %% "circe-literal" % Version.circe,
  "io.circe" %% "circe-derivation" % "0.13.0-M4"
)

lazy val doobieDeps = Seq(
  "org.tpolecat" %% "doobie-core" % Version.doobie,
"org.tpolecat" %% "doobie-postgres"  % Version.doobie
)

lazy val monocleDeps = Seq(
  "com.github.julien-truffaut" %% "monocle-core" % Version.monocle,
  "com.github.julien-truffaut" %% "monocle-macro" % Version.monocle,
  "com.github.julien-truffaut" %% "monocle-law" % Version.monocle % "test"
)

lazy val tapirDeps = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core" % Version.tapir,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % Version.tapir,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Version.tapir,
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % Version.tapir,
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % Version.tapir,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % "0.19.0-M4"
)

lazy val tofuDeps = Seq(
  "tf.tofu" %% "tofu-kernel" % "0.10.7",
  "tf.tofu" %% "tofu-logging" % "0.10.7",
  "tf.tofu" %% "tofu-logging-derivation" % "0.10.7"
)

lazy val silencerDeps = Seq(
  compilerPlugin(
    "com.github.ghik" % "silencer-plugin" % Version.silencer cross CrossVersion.full
  ),
  "com.github.ghik" % "silencer-lib" % Version.silencer % Provided cross CrossVersion.full
)

scalaVersion := Version.scala

libraryDependencies ++=
  (scalaDeps ++
    http4sDeps ++
    catsDeps ++
    circeDeps ++
    tapirDeps ++
    silencerDeps ++
    doobieDeps ++
    monocleDeps ++
    tofuDeps) ++
    Seq(
      "org.scalatest" %% "scalatest" % "3.2.9" % "test",
      "io.scalaland" %% "chimney" % "0.6.1"
      //      "com.github.fd4s" %% "fs2-kafka" % "2.5.0-M3"
    )

lazy val compilerThreads = {
  val threadsMax = 16
  val threadsAvailable = java.lang.Runtime.getRuntime.availableProcessors()
  val compilerThreads =
    if (threadsAvailable > threadsMax) threadsMax else threadsAvailable
  println(s"threads used for compilations: $compilerThreads")
  compilerThreads
}

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-unchecked",
  "-feature",
  "-deprecation",
  "-Ywarn-dead-code",
  "-explaintypes",
  "-language:postfixOps",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-Ymacro-annotations",
  "-Ywarn-extra-implicit",
  "-Ybackend-parallelism",
  compilerThreads.toString
)

