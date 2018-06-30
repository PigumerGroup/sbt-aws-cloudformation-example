package jp.pigumer.cast

import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.Date

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.pattern._
import akka.stream.{ActorMaterializer, Attributes}
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.scaladsl.{Flow, Source}
import akka.util.{ByteString, Timeout}
import com.amazonaws.services.polly.{AmazonPollyAsync, AmazonPollyAsyncClientBuilder}
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import jp.pigumer.polly.Speech
import su.litvak.chromecast.api.v2.ChromeCast

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait S3 {
  val region: String
  val key = "hello.mp3"
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard.withRegion(region).build

  def getUrl(bucketName: String) = {
    val meta = new ObjectMetadata()
    meta.setContentType("audio/mp3")
    s3.generatePresignedUrl(bucketName,
      key,
      new Date(Instant.now.plusSeconds(60).toEpochMilli))
  }

  def putObject(bucketName: String)(audio: ByteString) = {
    val bytes = audio.toArray[Byte]
    val metadata = new ObjectMetadata()
    metadata.setContentLength(bytes.length)
    s3.putObject(bucketName,
      key,
      new ByteArrayInputStream(bytes),
      metadata)
  }
}

object Cast extends App with S3 {
  val region = "ap-northeast-1"
  val queueName = "SampleQueue"

  private implicit val sqs: AmazonSQSAsync =
    AmazonSQSAsyncClientBuilder.standard.withRegion(region).build
  private val polly: AmazonPollyAsync =
    AmazonPollyAsyncClientBuilder.standard.withRegion(region).build

  val defaultMediaReciever = "CC1AD845"

  val castAddress: Option[String] = sys.env.get("ADDRESS")
  //val bucketName: String = sys.env.getOrElse("BUCKET_NAME", "YOUR BUCKETNAME")
  val bucketName: String = sys.env.getOrElse("BUCKET_NAME", "jp-pigumer-ops")

  implicit val system = ActorSystem("sbt-aws-cloudformation-example-cast")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 30 seconds

  val logger = Logging(system, this.getClass.getName)

  val discoverer = system.actorOf(Props[Discoverer].withDispatcher("akka.stream.polly-io-dispatcher"))
  val speechActor = system.actorOf(Props(classOf[Speech], polly))

  val queueUrl = sqs.getQueueUrl(queueName).getQueueUrl
  val chromeCast: Future[ChromeCast] =
    castAddress.fold(
      (discoverer ? "Google-Home").mapTo[ChromeCast])(
      address ⇒ Future(new ChromeCast(address))
    )

  val playFlow =
    Flow[Message].flatMapConcat { message ⇒
      Source.fromFuture(chromeCast)
        .log("source", cast ⇒ s"${cast.getAddress} ${cast.getName}")
        .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
        .flatMapConcat(cast ⇒
          Source.single(Speech.request)
            .ask[ByteString](speechActor)
            .map(putObject(bucketName))
            .map(_ ⇒ getUrl(bucketName))
            .map(new CastPlayer(cast).play)
            .async(dispatcher = "akka.stream.polly-io-dispatcher")
        )
        .map(_ ⇒ (message, MessageAction.Delete))
        .log("playFlow")
        .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
    }

  val done = SqsSource(queueUrl)
    .via(playFlow)
    .runWith(SqsAckSink(queueUrl))

  done.onComplete(_ ⇒
    system.terminate().onComplete(_ ⇒ sys.exit)
  )
}
