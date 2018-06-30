package jp.pigumer.javasound

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.scaladsl.{Flow, Source}
import akka.util.{ByteString, Timeout}
import com.amazonaws.services.polly.{AmazonPollyAsync, AmazonPollyAsyncClientBuilder}
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import jp.pigumer.polly.Speech

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends App {
  private val region = "ap-northeast-1"
  private val queueName = "SampleQueue"

  private implicit val sqs: AmazonSQSAsync =
    AmazonSQSAsyncClientBuilder.standard.withRegion(region).build
  private val polly: AmazonPollyAsync =
    AmazonPollyAsyncClientBuilder.standard.withRegion(region).build

  implicit val system = ActorSystem("sbt-aws-cloudformation-example-javasound")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 30 seconds

  private val queueUrl = sqs.getQueueUrl(queueName).getQueueUrl
  private val speechActor = system.actorOf(Props(classOf[Speech], polly))

  val playFlow =
    Flow[Message].flatMapConcat { message ⇒
      Source.single(Speech.request)
        .ask[ByteString](speechActor)
        .map(Player.convert)
        .map(Player.play(0))
        .map(_ ⇒ (message, MessageAction.Delete))
        .async(dispatcher = "akka.stream.polly-io-dispatcher")
    }

  val done = SqsSource(queueUrl)
    .via(playFlow)
    .runWith(SqsAckSink(queueUrl))

  done.onComplete(_ ⇒
    system.terminate().onComplete(_ ⇒ sys.exit)
  )
}
