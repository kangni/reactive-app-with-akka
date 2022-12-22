name := "messaging"
organization := "com.rarebooks"

version := "1.0"

val akkaVersion = "2.4.17"
val logbackVer = "1.2.1"

scalaVersion := "2.12.3"

resolvers += "Lightbend Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %%  "akka-actor"      % akkaVersion,
  "com.typesafe.akka"  %%  "akka-slf4j"      % akkaVersion,
  "ch.qos.logback"      %  "logback-classic" % logbackVer,
  "com.typesafe.akka"  %%  "akka-testkit"    % akkaVersion  %  "test",
  "org.scalatest"      %%  "scalatest"       % "3.0.1"      %  "test"
)
