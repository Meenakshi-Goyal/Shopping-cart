ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file(".")).settings(
  name := "shopping-cart-assignment",
  libraryDependencies ++= Seq(
    // Cats-Effect (pre-existing)
    "org.typelevel" %% "cats-effect" % "3.5.4",
    "org.typelevel" %% "cats-effect-kernel" % "3.5.4",
    "org.typelevel" %% "cats-effect-std" % "3.5.4",

    // HTTP Client: http4s Ember Client
    "org.http4s" %% "http4s-ember-client" % "0.23.27",
    "org.http4s" %% "http4s-dsl" % "0.23.27",

    // JSON Serialization: Circe
    "io.circe" %% "circe-core" % "0.14.7",
    "io.circe" %% "circe-generic" % "0.14.7",
    "io.circe" %% "circe-parser" % "0.14.7",
    "org.http4s" %% "http4s-circe" % "0.23.27", // Http4s-Circe integration

    // Better Monadic For (pre-existing)
    compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),

    // Testing (pre-existing)
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
  )
)