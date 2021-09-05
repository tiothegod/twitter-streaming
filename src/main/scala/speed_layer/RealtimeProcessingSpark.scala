package speed_layer

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{LongType, StringType, StructField, StructType}
import org.apache.spark.sql.functions.{col, current_timestamp, desc, from_json, lower, sum, to_utc_timestamp, window}
import org.apache.spark.sql.streaming.{OutputMode, StreamingQuery}
import main_package.AppConfiguration
import akka.actor.{Actor, ActorSystem, Props}
import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.sql.cassandra._

import java.util.Calendar
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class RealtimeProcessingSpark {
  val spark: SparkSession = SparkSession
    .builder()
    .appName("Spark Speed Layer")
    .master("local")
    .getOrCreate()
  spark.sparkContext.setLogLevel("WARN")

  import spark.implicits._
  val connector: CassandraConnector = CassandraConnector(spark.sparkContext.getConf)
  val twitterDataScheme: StructType = StructType(
    List(
      StructField("tweet_id", LongType, nullable = true),
      StructField("user_id", LongType, nullable = true),
      StructField("user_name", StringType, nullable = true),
      StructField("user_loc", StringType, nullable = true),
      StructField("content", StringType, nullable = true),
      StructField("hashtag", StringType, nullable = true),
      StructField("created_date", LongType, nullable = true)
    )
  )
  var activeQuery: StreamingQuery = null

  def restartQuery(): Unit = {
//     Remove all existing data from hashtag_realtimeview table
        connector.withSessionDo(session => session.execute("TRUNCATE lambda_architecture.hashtag_realtimeview"))

    if (activeQuery == null) {
      activeQuery = realtimeAnalysis()
    }
    else {
      activeQuery.stop()
      activeQuery = realtimeAnalysis()
    }
  }

  def realtimeAnalysis(): StreamingQuery = {
    val df = spark.readStream.format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", AppConfiguration.kafkaTopic)
      .load()
    val twitterStreamData = df.selectExpr("CAST(value AS STRING) as jsonData")
      .select(from_json($"jsonData", schema = twitterDataScheme).as("data"))
      .select("data.*")
    val hashtagDf = twitterStreamData
      .select(lower($"hashtag"))
      .filter($"hashtag".notEqual("null"))
    val splitedHashtag = hashtagDf.as[String]
      .flatMap(_.split(","))
      .filter($"value".notEqual(""))
    val hashtagCount = splitedHashtag
      .groupBy("value")
      .count()
      .sort(desc("count"))
      .withColumnRenamed("value", "hashtag")
    val query = hashtagCount.writeStream.foreachBatch { (batchDF, batchId) =>
      batchDF.persist()
      batchDF.show()
      batchDF.write.format("org.apache.spark.sql.cassandra")
        .mode("Append")
        .options(
          Map("table" -> "hashtag_realtimeview",
            "keyspace" -> "lambda_architecture",
            "spark.cassandra.connection.host"-> "127.0.0.1",
            "spark.cassandra.connection.port" -> "9042"
          )).save()
    }.outputMode("complete").start()
    query
  }

}

case object StartProcessing

class RealtimeProcessingActor(spark_realtimeProc: RealtimeProcessingSpark) extends Actor {
  def receive: Receive = {
    //Start hashtag realtime processing
    case StartProcessing =>
      println("\nRestart hashtag realtime processing...")
      spark_realtimeProc.restartQuery()
  }
}

object Runner {
  def main(args: Array[String]): Unit = {
    //Creating an ActorSystem
    val actorSystem = ActorSystem("ActorSystem")

    //Create realtime processing actor
    val realtimeActor = actorSystem.actorOf(Props(new RealtimeProcessingActor(new RealtimeProcessingSpark)))

    //Using akka scheduler to test restartQuery() function
    import actorSystem.dispatcher
    val initialDelay = 100 milliseconds
    val interval = 3 minutes

    actorSystem.scheduler.schedule(initialDelay, interval, realtimeActor, StartProcessing)
  }
}