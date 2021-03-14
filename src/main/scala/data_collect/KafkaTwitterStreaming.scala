package data_collect

import twitter4j._
import main_package.AppConfiguration
import org.json4s.DefaultFormats
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import net.liftweb.json.Serialization.write
import java.util.Properties

case class Tweet(tweet_id: Long,
                 user_id: Long,
                 user_name: String,
                 user_loc: String,
                 content: String,
                 hashtag: String,
                 created_date: Long)

object KafkaTwitterStreaming {
  def run(): Unit = {
    val kafkaTopic = AppConfiguration.kafkaTopic
    val producer = new KafkaProducer[String, String](AppConfiguration.getKafkaProp())
    getStreamTweets(kafkaTopic, producer)
  }

  def getStreamTweets(kafkaTopic: String, producer: KafkaProducer[String, String]): Unit = {
    val twitterStream: TwitterStream = new TwitterStreamFactory(AppConfiguration.TwitterConfig).getInstance
    val twitterStatusListener = new StatusListener() {
      def onStatus(status: Status) {
        //      val statusJson = TwitterObjectFactory.getRawJSON(status)
        val tweet_id = status.getId
        val created_date = status.getCreatedAt.getTime
        var content = status.getText
        val lang = status.getLang
        val user = status.getUser
        val hashtag = status.getHashtagEntities.toList.map(_.getText).mkString(",")
        if (status.getRetweetedStatus != null) content = status.getRetweetedStatus.getText
        if (lang.equals("en")) {
          implicit val formats = net.liftweb.json.DefaultFormats
          val message = write(Tweet(tweet_id, user.getId, user.getName, user.getLocation, content, hashtag, created_date))
          val data = new ProducerRecord[String, String](kafkaTopic, message)
          print(message)
          producer.send(data)
        }
      }

      override def onException(ex: Exception): Unit = {
        ex.printStackTrace()
      }

      override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {
      }

      override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = {
      }

      override def onScrubGeo(userId: Long, upToStatusId: Long): Unit = {
      }

      override def onStallWarning(warning: StallWarning): Unit = {
      }
    }
    twitterStream.addListener(twitterStatusListener)
    twitterStream.sample("en")
  }

  def main(args: Array[String]): Unit = {
    KafkaTwitterStreaming.run()
  }
}
