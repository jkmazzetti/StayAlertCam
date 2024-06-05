package utilidades

import android.content.Context
import android.media.RingtoneManager
import android.media.MediaPlayer

object Utilidades {

    fun reproducirSonidoNotificacion(context: Context) {
        val defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val mediaPlayer = MediaPlayer()

        try {
            mediaPlayer.setDataSource(context, defaultRingtoneUri)
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                // Liberar recursos cuando la reproducción haya finalizado
                mediaPlayer.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
