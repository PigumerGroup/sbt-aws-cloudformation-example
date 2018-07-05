package jp.pigumer.polly

import akka.actor.Actor
import akka.util.ByteString
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.polly.AmazonPollyAsync
import com.amazonaws.services.polly.model.{OutputFormat, SynthesizeSpeechRequest, SynthesizeSpeechResult, VoiceId}

class Speech(client: AmazonPollyAsync) extends Actor {

  override def receive = {
    case synthesizeSpeechRequest: SynthesizeSpeechRequest ⇒
      val originalSender = sender
      client.synthesizeSpeechAsync(
        synthesizeSpeechRequest,
        new AsyncHandler[SynthesizeSpeechRequest, SynthesizeSpeechResult] {
          override def onError(cause: Exception): Unit =
            throw cause
          override def onSuccess(request: SynthesizeSpeechRequest, result: SynthesizeSpeechResult): Unit = {
            val audioStream = result.getAudioStream
            val mp3 = Stream.continually(audioStream.read).takeWhile(_ != -1).map(_.toByte).toArray
            originalSender ! ByteString(mp3)
          }
        }
      )
  }
}

object Speech {


  def request(text: String) =
    new SynthesizeSpeechRequest().
      withOutputFormat(OutputFormat.Mp3).
      withText(text).
      withVoiceId(VoiceId.Ivy)

}