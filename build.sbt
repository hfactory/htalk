name := """HTalk"""

version := "1.6"

organization := "com.ubeeko"

scalaVersion := "2.10.6"

lazy val root = project
    .in(file("."))
    .settings(
    updateOptions := updateOptions.value.withCachedResolution(true),
    publishArtifact := true)

javaOptions ++= Seq("-source=1.8","-target=1.8")

resolvers ++= Seq(
 "Apache HBase" at "https://repository.apache.org/content/repositories/releases",
 "Thrift" at "http://people.apache.org/~rawson/repo/",
 "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
 "Ubeeko" at "http://nexus.hfactory.io/nexus/content/repositories/releases/"
)

val hbaseVersion = "1.2.4"

libraryDependencies ++= Seq(
  "ch.qos.logback"     % "logback-classic"          % "1.0.13",
  "org.json4s"         %% "json4s-native"           % "3.3.0",
  "com.typesafe"       %% "scalalogging-slf4j"      % "1.0.1",
  "org.apache.hbase"   %  "hbase-common"            % hbaseVersion,
  "org.apache.hbase"   %  "hbase-client"            % hbaseVersion,
  "org.apache.hbase"   %  "hbase-hadoop-compat"     % hbaseVersion,
  "org.apache.hadoop"  %  "hadoop-client"           % "2.7.1",
  "org.apache.hadoop"  %  "hadoop-hdfs"             % "2.7.1",
  "org.apache.hadoop"  %  "hadoop-common"           % "2.7.1",
  "net.liftweb"        %% "lift-json"               % "2.6.3",
  "org.scalatest"      %  "scalatest_2.10"          % "2.2.6"        % "test",
  "org.specs2"         %% "specs2"                  % "2.3.10"       % "test"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

// allow circular dependencies for test sources
compileOrder in Test := CompileOrder.Mixed

// Make scaladoc only process Scala source files (and ignore Java ones).
sources in (Compile, doc) ~= (_ filter (_.getName endsWith ".scala"))

testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oDF", "-w", "com.ubeeko")

/* Publish artifacts to Nexus */
publishMavenStyle := true

publishTo := {
  val nexus = "http://nexus.hfactory.io/nexus"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "/content/repositories/snapshots/")
  else
    Some("releases"  at nexus + "/content/repositories/releases/")
}
//publishArtifact in Test := true
