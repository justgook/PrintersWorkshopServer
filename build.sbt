name := "PrinterWorksopServer"
version := "1.0.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"
coverageEnabled := true



libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

val akkaV = "2.4.7"

//main artifact Serial-port
libraryDependencies += "ch.jodersky" %% "flow-core" % "3.0.1"
//"fat" jar containing native libraries
libraryDependencies += "ch.jodersky" % "flow-native" % "3.0.1" % "runtime"

libraryDependencies += "org.gnieh" %% "diffson" % "2.0.2"
libraryDependencies +=  "com.typesafe.akka" %% "akka-testkit" % akkaV

//libraryDependencies += "com.github.pathikrit"  %% "better-files-akka"  % "2.16.0"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
