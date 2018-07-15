package jp.pigumer.javasound

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, Attributes}
import akka.util.{ByteString, Timeout}
import javax.sound.sampled.{AudioSystem, Mixer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait S3Mock {
  private val key = "hello.mp3"

  def readBytes: ByteString= {
    val is = Thread.currentThread().getContextClassLoader.getResourceAsStream("hello.mp3")
    try {
      ByteString(Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray)
    } finally {
      is.close()
    }
  }
}

object PlayerSpec extends App with S3Mock {
  implicit val system = ActorSystem("sbt-aws-cloudformation-example-javasound")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 30 seconds

  private val logger = Logging(system, Main.getClass.getName)

  private val mixerIndex: Int = sys.env("INDEX").toInt

  private val mixerInfo: Array[Mixer.Info] = {
    val info = AudioSystem.getMixerInfo()
    info.zipWithIndex.foreach {
      case (mi, index) ⇒
        logger.info(s"$index: ${mi.getName}")
    }
    info
  }

  val play =
      Source.single(readBytes)
        .log("source")
        .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
        .via(Flow[ByteString]
          .map(Player.convert)
          .log("convert")
          .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel)))
        .via(Flow[ByteString]
          .map(Player.play(mixerInfo(mixerIndex)))
          .log("play")
          .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel)))
        .async(dispatcher = "akka.stream.sound-io-dispatcher")

  val done = play
    .runWith(Sink.ignore)

  done.onComplete(_ ⇒
    system.terminate().onComplete(_ ⇒ sys.exit)
  )
}
