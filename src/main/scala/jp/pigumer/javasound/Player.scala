package jp.pigumer.javasound

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, OutputStream}

import akka.util.ByteString
import javax.sound.sampled.AudioFormat.Encoding
import javax.sound.sampled._

object Player {

  private val mixerInfo: Array[Mixer.Info] = {
    val info = AudioSystem.getMixerInfo()
    info.zipWithIndex.foreach {
      case (mixerInfo, index) ⇒
        println(s"$index: ${mixerInfo.getName}")
    }
    info
  }

  def convert(bytes: ByteString) = {

    def write(f: OutputStream ⇒ Unit) = {
      val output = new ByteArrayOutputStream()
      try {
        f(output)
        ByteString(output.toByteArray)
      } finally {
        output.close()
      }
    }

    val originalAudioInput = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes.toArray))
    val originalFormat = originalAudioInput.getFormat()
    val converted: AudioInputStream = AudioSystem.getAudioInputStream(
      new AudioFormat(
        Encoding.PCM_SIGNED,
        originalFormat.getSampleRate,
        16,
        originalFormat.getChannels,
        originalFormat.getChannels * 2,
        originalFormat.getSampleRate,
        false
      ),
      originalAudioInput)

    write(output ⇒ AudioSystem.write(converted, AudioFileFormat.Type.AU, output))
  }

  def play(index: Int)(bytes: ByteString) = {
      val input = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes.toArray))
      val mixer = mixerInfo(index)
      val clip = AudioSystem.getClip(mixer)
      clip.open(input)
      clip.start()
      while (!clip.isRunning) {
        Thread.sleep(500)
      }
      while (clip.isRunning) {
        Thread.sleep(500)
      }
      clip.close()
      input
    }
}
