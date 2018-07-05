package jp.pigumer.javasound

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.{ActorMaterializer, Attributes}
import akka.util.{ByteString, Timeout}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import javax.sound.sampled.{AudioSystem, Mixer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait S3 {
  protected val region: String
  private val key = "hello.mp3"
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard.withRegion(region).build

  def readBytes(bucketName: String) = {
    val s3Object = s3.getObject(bucketName, key)
    val is = s3Object.getObjectContent
    try {
      ByteString(Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray)
    } finally {
      is.close()
    }
  }
}

object Main extends App with S3 {
  protected val region = "ap-northeast-1"
  private val queueName = "SampleQueue"

  private implicit val sqs: AmazonSQSAsync =
    AmazonSQSAsyncClientBuilder.standard.withRegion(region).build

  val bucketName: String = sys.env.getOrElse("BUCKET_NAME", "YOUR BUCKETNAME")

  implicit val system = ActorSystem("sbt-aws-cloudformation-example-javasound")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 30 seconds

  private val queueUrl = sqs.getQueueUrl(queueName).getQueueUrl

  private val logger = Logging(system, Main.getClass.getName)

  private val mixerInfo: Array[Mixer.Info] = {
    val info = AudioSystem.getMixerInfo()
    info.zipWithIndex.foreach {
      case (mi, index) ⇒
        logger.info(s"$index: ${mi.getName}")
    }
    info
  }

  val playFlow =
    Flow[Message].flatMapConcat { message ⇒
      Source.single(readBytes(bucketName))
        .log("source")
        .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
        .map(Player.convert)
        .map(Player.play(mixerInfo(0)))
        .map(_ ⇒ (message, MessageAction.Delete))
        .async(dispatcher = "akka.stream.sound-io-dispatcher")
    }

  val done = SqsSource(queueUrl)
    .via(playFlow)
    .runWith(SqsAckSink(queueUrl))

  done.onComplete(_ ⇒
    system.terminate().onComplete(_ ⇒ sys.exit)
  )
}
