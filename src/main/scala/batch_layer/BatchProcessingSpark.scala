package batch_layer

import akka.actor.{Actor, ActorSystem, Props}
import com.datastax.spark.connector.cql.CassandraConnector
import main_package.AppConfiguration
import org.apache.spark.SparkContext
import org.apache.spark.sql.{SaveMode, SparkSession}

import scala.concurrent.duration._
import org.apache.spark.sql.functions.lower

import scala.language.postfixOps

class BatchProcessingSpark {
  val spark: SparkSession = org.apache.spark.sql.SparkSession
    .builder()
    .master("local[*]")
    .config("spark.cassandra.connection.host", "localhost")
    .appName("Lambda architecture - Batch Processing")
    .getOrCreate()

  import spark.implicits._

  val sparkContext: SparkContext = spark.sparkContext
  sparkContext.setLogLevel("WARN")

  def hashtagAnalysis(): Unit = {
    val df = spark.read.format("org.apache.spark.sql.cassandra")
      .options(Map("table" -> "master_dataset", "keyspace" -> "lambda_architecture"))
      .load()
    println("Totals rows : " + df.count())
    println("First 10 rows :")
    df.show(10)
    var hashtag_df = df.select(lower($"hashtag"))
    val current_time = System.currentTimeMillis()
    val tweetDuration = AppConfiguration.tweetDuration
    hashtag_df = hashtag_df.filter($"created_date" > (current_time - tweetDuration.toMillis)
      && $"hashtag".notEqual("null"))
    println("The first ten tweets containing hashtag: ")
    hashtag_df.show(10)
    val hashtag_splitted = hashtag_df.as[String].flatMap(_.split(","))
    val hashtagCount = hashtag_splitted
      .groupBy("value")
      .count().sort($"value".desc)
      .withColumnRenamed("value", "hashtag")
    println("The top ten popular hashtags: ")
    hashtagCount.show(10)
    //Remove all existed data from hashtag_batchview table
    val connector = CassandraConnector(sparkContext.getConf)
    connector.withSessionDo(session => session.execute("TRUNCATE lambda_architecture.hashtag_batchview"))
    hashtagCount.write.format("org.apache.spark.sql.cassandra")
      .options(Map("keyspace" -> "lambda_architecture", "table" -> "hashtag_batchview"))
      .mode(SaveMode.Append)
      .save()

  }
}

case object HashTagProcessing

//Define BatchProcessing actor
class BatchProcessingActor(spark_processor: BatchProcessingSpark) extends Actor {

  //Implement receive method
  def receive = {
    //Start hashtag batch processing
    case HashTagProcessing => {
      println("nStart hashtag batch processing...")
      spark_processor.hashtagAnalysis()
    }
  }
}

object Runner {
  def main(args: Array[String]): Unit = {

    //Creating an ActorSystem
    val actorSystem = ActorSystem("ActorSystem");

    //Create batch actor
    val batchActor = actorSystem.actorOf(Props(new BatchProcessingActor(new BatchProcessingSpark)))


    //Using akka scheduler to run the batch processing periodically
    import actorSystem.dispatcher
    val initialDelay = 100 milliseconds
    val batchInterval = AppConfiguration.batchInterval //running batch processing after each 30 mins
    actorSystem.scheduler.schedule(initialDelay, batchInterval, batchActor, HashTagProcessing)

  }
}