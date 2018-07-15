package jp.pigumer.javasound

import javax.sound.sampled.AudioSystem

object MixerInfoList extends App {

  val info = AudioSystem.getMixerInfo()
  info.zipWithIndex.foreach {
    case (mi, index) ⇒
      println(s"$index: ${mi.getName}")
  }
}
