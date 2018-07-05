package jp.pigumer.cast

import java.net.URL

import su.litvak.chromecast.api.v2.ChromeCast

class CastPlayer(cast: ChromeCast) {

  private val defaultMediaReciever = "CC1AD845"

  def play(url: URL) = {
    val status = cast.getStatus
    if (cast.isAppAvailable(defaultMediaReciever) && !status.isAppRunning(defaultMediaReciever))
      cast.launchApp(defaultMediaReciever)
    cast.load(url.toString)
    cast.play
    cast.disconnect()
  }
}
