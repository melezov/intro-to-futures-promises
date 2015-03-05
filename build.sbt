scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.4.2",
  "io.spray"          %%  "spray-can"     % "1.3.2",
  "io.spray"          %%  "spray-routing" % "1.3.2",
  "com.typesafe.akka" %%  "akka-actor"    % "2.3.9",
  "org.scalatest"     %% "scalatest" % "2.2.4" % "test"
)

Revolver.settings