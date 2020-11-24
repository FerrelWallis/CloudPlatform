name := "CloudPlatform"

version := "1.0"

lazy val `cloudplatform` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(jdbc, ehcache, ws, specs2 % Test, guice)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

libraryDependencies += "commons-io" % "commons-io" % "2.5"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.6"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.47"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "4.0.2",
  "com.typesafe.slick" %% "slick-codegen" % "3.3.2",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.4.1",
  "org.rosuda.REngine" % "Rserve" % "1.8.1",
  "org.rosuda.REngine" % "REngine" % "2.1.0",
  "org.apache.commons" % "commons-compress" % "1.18",
  "org.apache.pdfbox" % "pdfbox" % "2.0.19",
  "com.aliyun" % "aliyun-java-sdk-core" % "3.7.1",
  "com.aliyun" % "aliyun-java-sdk-dysmsapi" % "1.1.0"  //短信服务
)