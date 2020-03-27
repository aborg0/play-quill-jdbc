name := """play-quill-jdbc"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  jdbc,
//  cache,
  caffeine,
  ws,
  evolutions,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  "com.h2database" % "h2" % "1.4.199",
  "io.getquill" %% "quill-jdbc" % "3.5.1"/*,
  "com.typesafe.play" % "play-jdbc-evolutions_2.11" % "2.5.12"*/
  , "org.scala-lang.modules" %% "scala-async" % "0.10.0"
  ,"org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
  ,
)

resolvers ++= Seq(
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
)

routesGenerator := InjectedRoutesGenerator