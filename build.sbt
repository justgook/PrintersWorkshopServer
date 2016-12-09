import scala.language.postfixOps

name := "PrinterWorkshopServer"
version := "1.0.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayAkkaHttpServer).disablePlugins(PlayNettyServer)

ivyConfigurations += (config("external") hide)

scalaVersion := "2.11.8"
//coverageEnabled := true
coverageEnabled.in(Test, test) := true

//libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19"

val akkaV = "2.4.14"
val betterFilesV = "2.16.0"
val `odersky-flow` = "3.0.4"
val diffsonV = "2.0.2"
val cucumberV = "1.2.5"
libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,

  "com.github.pathikrit" %% "better-files" % betterFilesV,
  "ch.jodersky" %% "flow-core" % `odersky-flow`,
  "ch.jodersky" % "flow-native" % `odersky-flow` % "runtime",
  "org.gnieh" %% "diffson" % diffsonV,
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.11", //update me
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-persistence" % akkaV,

  "junit" % "junit" % "4.12",
  "info.cukes" % "cucumber-junit" % cucumberV,
  "info.cukes" %% "cucumber-scala" % cucumberV % Test,
  "org.dmonix.akka" % "akka-persistence-mock_2.11" % "1.1.1", //TODO create own mock
  "com.typesafe.akka" %% "akka-testkit" % akkaV
)

libraryDependencies += "com.github.justgook" % "PrintersWorkshopUI" % "0.9.1" % "external->default" from "https://github.com/justgook/PrintersWorkshopUI/archive/0.9.1.zip"
scalacOptions ++= Seq("-feature", "-deprecation", "-language:postfixOps")

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "features")



resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

resourceGenerators in Compile += Def.task {
  val externalArchives = update.value.select(configurationFilter("external"))
  externalArchives flatMap { zip =>
    IO.unzip(zip, (resourceManaged in Compile).value).toSeq
  }
}.taskValue
