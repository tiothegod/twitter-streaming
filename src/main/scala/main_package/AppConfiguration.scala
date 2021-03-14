package main_package

import com.typesafe.config.{Config, ConfigFactory}
import twitter4j.conf.Configuration

import java.util.Properties
import scala.collection.JavaConversions._
import scala.concurrent.duration.{Duration, FiniteDuration}

object AppConfiguration {
  val config: Config = ConfigFactory.load("application.conf")
  val consumerKey: String = config.getString("twitter.consumerKey")
  val consumerSecret: String = config.getString("twitter.consumerSecret")
  val accessToken: String = config.getString("twitter.accessToken")
  val accessTokenSecret: String = config.getString("twitter.accessTokenSecret")

  val TwitterConfig: Configuration = new twitter4j.conf.ConfigurationBuilder()
    .setOAuthConsumerKey(consumerKey)
    .setOAuthConsumerSecret(consumerSecret)
    .setOAuthAccessToken(accessToken)
    .setOAuthAccessTokenSecret(accessTokenSecret)
    .setJSONStoreEnabled(true)
    .build

  val kafkaTopic: String = config.getString("kafka.topic")

  def getKafkaProp(): Properties = {
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092".asInstanceOf[AnyRef])
    props.put("acks", "all".asInstanceOf[AnyRef])
    props.put("retries", 0.asInstanceOf[AnyRef])
    props.put("batch.size", 16384.asInstanceOf[AnyRef])
    props.put("linger.ms", 1.asInstanceOf[AnyRef])
    props.put("buffer.memory", 33554432.asInstanceOf[AnyRef])
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props
  }


  // Batch processing config
  // Convert Duration to Finite Duration
  //    val tweetDuration: FiniteDuration =Duration.fromNanos(config.getDuration("batchProcessing.tweetDuration").toNanos)
  //    val batchInterval: FiniteDuration =Duration.fromNanos(config.getDuration("batchProcessing.batchInterval").toNanos)

}