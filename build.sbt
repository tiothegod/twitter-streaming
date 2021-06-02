name := "twitter-streaming"

version := "0.1"

scalaVersion := "2.11.0"

val sparkVersion = "2.4.5"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" %% "spark-streaming-twitter" % "1.6.3",
  "org.twitter4j" % "twitter4j-stream" % "3.0.3",
  "com.typesafe" % "config" % "1.2.0",
  "org.apache.kafka" %% "kafka" % "2.4.1",
  "net.liftweb" %% "lift-json" % "2.6.2",
  "org.apache.spark" %% "spark-sql-kafka-0-10" % "2.4.1" ,
  "com.typesafe.akka" %% "akka-actor" % "2.3.8",
  "org.apache.spark" % "spark-streaming_2.11" % "2.2.0",
  "com.datastax.spark" %% "spark-cassandra-connector" % "2.4.2",
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.4.4"

)