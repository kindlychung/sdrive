name := "scaladrive"

version := "1.0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= List(
  "com.google.api-client" % "google-api-client" % "1.21.0",
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.21.0",
  "com.google.apis" % "google-api-services-drive" % "v3-rev6-1.21.0"
)

libraryDependencies += "com.madgag" % "scala-arm_2.11" % "1.3.4"


//libraryDependencies += "com.j256.simplemagic" % "simplemagic" % "1.6"

libraryDependencies += "org.apache.commons" % "commons-compress" % "1.11"

libraryDependencies += "org.apache.directory.studio" % "org.apache.commons.io" % "2.4"

libraryDependencies += "eu.medsea.mimeutil" % "mime-util" % "2.1.3"



resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.kindlychung" % "docopt.scala" % "0.2.2"


// The main class name must be full (including package names)
mainClass in Compile := Some("vu.co.kaiyin.scaladrive.SDrive")

mainClass in assembly := Some("vu.co.kaiyin.scaladrive.SDrive")


// How to invoke the tool:
// java -jar /Users/kaiyin/IdeaProjects/scaladrive/target/scala-2.11/scaladrive-assembly-1.0.jar
// java -cp /Users/kaiyin/IdeaProjects/scaladrive/target/scala-2.11/scaladrive-assembly-1.0.jar vu.co.kaiyin.scaladrive.SDrive

