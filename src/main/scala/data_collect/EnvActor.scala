package data_collect
import java.io.{File, InputStreamReader,BufferedReader}
import akka.actor.{Actor, ActorSystem, Props}
case object StartZookeeper
case object StartKafkaServer
case object StartKafkaCassandra
class EnvActor extends Actor{
  val kafkaDir="/home/kafka/kafka_2.11-2.4.1"
  val kafkaCassandraCmd="bin/connect-standalone.sh config/connect-standalone.properties config/cassandra-sink-twitter.properties"
  val kafkaServerCmd="bin/kafka-server-start.sh config/server.properties"
  val zookeeperCmd="bin/zookeeper-server-start.sh config/zookeeper.properties"

  def cmdExecuter(cmd: String, dir : String): Int ={
    //using .exec() method to run the command
    val process = Runtime.getRuntime.exec(cmd, null, new File(dir))

    //Print the output of the process
    val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
    var line:String= null
    while ((line = reader.readLine()) != null) {
      System.out.println(line)
    }

    //Waiting until the process has been terminated
    process.waitFor()

  }
  def receive = {

    //Start Zookeeper
    case StartZookeeper => {
      println("nStart Zookeeper...")
      cmdExecuter(zookeeperCmd,kafkaDir)
    }

    //Start Kafka Server
    case StartKafkaServer => {
      println("nStart Kafka Server...")
      cmdExecuter(kafkaServerCmd,kafkaDir)
    }

    //Start Kafka Cassandra Connector
    case StartKafkaCassandra => {
      println("nStart Kafka Cassandra Connector...")
      cmdExecuter(kafkaCassandraCmd,kafkaDir)
    }
  }
}
object EnvRunner {
  def start(): Unit = {
    {
      val actorSystem = ActorSystem("ActorSystem");
      val actor1 = actorSystem.actorOf(Props[EnvActor])
      val actor2 = actorSystem.actorOf(Props[EnvActor])
      val actor3 = actorSystem.actorOf(Props[EnvActor])

      //Send message to each actor to ask them doing their job
      actor1 ! StartZookeeper
      //Waiting until the Zookeeper successfully started
      Thread.sleep(10000)

      actor2 ! StartKafkaServer
      Thread.sleep(10000)

      actor3 ! StartKafkaCassandra
      Thread.sleep(5000)
    }
  }

  def main(args: Array[String]): Unit = {
    EnvRunner.start()
  }
}
