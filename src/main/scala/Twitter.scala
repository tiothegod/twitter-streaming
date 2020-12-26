import Utilities.{setupLogging, setupTwitter}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.twitter.TwitterUtils

object Twitter {
  def main(args: Array[String]): Unit = {
    setupTwitter()
    val ssc = new StreamingContext("local[*]", "Twitter-Streaming", Seconds(2))
    ssc.checkpoint("./checkpoint")
    setupLogging()

    val tweets = TwitterUtils.createStream(ssc, None)
    val statuses = tweets.map(status => status.getText)

    val hashtag = statuses
      .flatMap(twitterText => twitterText.split(" "))
      .filter(x => x.startsWith("#"))

    val hashTagCount = hashtag
      .map(word => (word, 1))
      .reduceByKeyAndWindow((x, y) => x + y, (x, y) => x - y, Seconds(300), Seconds(2))

    val result = hashTagCount
      .transform(rdd => rdd.sortBy(x => x._2, false))

    result.print()
    ssc.start()
    ssc.awaitTermination()
  }
}
