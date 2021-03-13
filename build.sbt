name := "twitter-streaming"

version := "0.1"

scalaVersion := "2.11.0"

val sparkVersion = "2.4.5"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion ,
  "org.apache.spark" %% "spark-streaming-twitter" % "1.6.3"
)