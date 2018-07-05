package jp.pigumer.polly

import java.io.ByteArrayInputStream

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.{ByteString, Timeout}
import com.amazonaws.services.polly.{AmazonPollyAsync, AmazonPollyAsyncClientBuilder}
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait S3 {
  val region: String
  val key = "hello.mp3"
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard.withRegion(region).build

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

object Main extends App with S3 {
  val region = "ap-northeast-1"

  private val polly: AmazonPollyAsync =
    AmazonPollyAsyncClientBuilder.standard.withRegion(region).build

  val bucketName: String = sys.env.getOrElse("BUCKET_NAME", "YOUR BUCKETNAME")

  implicit val system = ActorSystem("sbt-aws-cloudformation-polly-task")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 30 seconds

  val logger = Logging(system, this.getClass.getName)

  val text =
    """Please stand by.
    Your suite's control center is rebooting."""

  val speechActor = system.actorOf(Props(classOf[Speech], polly))

  val done = Source.single(Speech.request(text))
    .ask[ByteString](speechActor)
    .via(Flow[ByteString].map(putObject(bucketName)).async("akka.stream.sound-io-dispatcher"))
    .runWith(Sink.ignore)

  done.onComplete(_ ⇒
    system.terminate().onComplete(_ ⇒ sys.exit)
  )

}
