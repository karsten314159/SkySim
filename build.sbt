name := "SkySim"

version := "0.1"

scalaVersion := "2.13.0"

resolvers += "mvnrepository" at "http://mvnrepository.com/artifact/"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.23"
libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.17"

libraryDependencies += "org.specs2" %% "specs2-core" % "4.6.0" % Test

//libraryDependencies += "junit" % "junit" % "4.12" % Test

//scalaSource in Test := baseDirectory.value / "test"
