import scala.language.postfixOps

name := "PrinterWorksopServer"
version := "1.0.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

ivyConfigurations += (config("external") hide)

scalaVersion := "2.11.8"
//coverageEnabled := true
coverageEnabled.in(Test, test) := true

//libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)
scalacOptions ++= Seq("-feature", "-deprecation")

val akkaV = "2.4.8"
val `odersky-flow` = "3.0.2"
val diffsonV = "2.0.2"
//main artifact Serial-port
libraryDependencies += "ch.jodersky" %% "flow-core" % `odersky-flow`
//"fat" jar containing native libraries
libraryDependencies += "ch.jodersky" % "flow-native" % `odersky-flow` % "runtime"

libraryDependencies += "org.gnieh" %% "diffson" % diffsonV
libraryDependencies +=  "com.typesafe.akka" %% "akka-testkit" % akkaV

//libraryDependencies += "com.github.pathikrit"  %% "better-files-akka"  % "2.16.0"

libraryDependencies += "com.github.justgook" % "PrintersWorkshopUI" % "491e0cd" % "external->default" from "https://github.com/justgook/PrintersWorkshopUI/archive/491e0cd.zip" // todo: use some human-readable version

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

resourceGenerators in Compile += Def.task {
  val externalArchives = update.value.select(configurationFilter("external"))
  externalArchives flatMap { zip =>
    IO.unzip(zip, (resourceManaged in Compile).value).toSeq
  }
}.taskValue
