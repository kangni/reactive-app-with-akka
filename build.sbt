val akkaVersion = "2.5.4"
val logbackVersion = "1.2.3"
val scalaLogging = "3.7.2"
val scalacticVersion = "3.0.1"
val scalaTestVersion = scalacticVersion
val parserCombinatorVersion = "1.0.6"

lazy val commonSettings = Seq(
  version := "0.4-SNAPSHOT",
  scalaVersion := "2.12.3"
)

parallelExecution in Test := false

logBuffered in Test := false

parallelExecution in ThisBuild := false

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"

lazy val common = project.settings(commonSettings)

lazy val messaging = project.dependsOn(common % "test->test;compile->compile")
lazy val elasticity = project.dependsOn(common % "test->test;compile->compile")
