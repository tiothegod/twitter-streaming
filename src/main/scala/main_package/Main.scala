package main_package
import akka.actor.{ActorSystem, Props}
import speed_layer.{RealtimeProcessingActor, RealtimeProcessingSpark}
object Main {
  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem("ActorSystem")
    val realtimeActor = actorSystem.actorOf(Props(new RealtimeProcessingActor(new RealtimeProcessingSpark)))
//    actorSystem.scheduler.schedule(initialDelay,batchInterval,batchActor,HashTagProcessing)
  }
}
