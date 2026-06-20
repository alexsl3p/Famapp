package com.kinly.famapp.features.chat

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Простой диктофон: пишет голосовое в m4a и отдаёт байты. */
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    fun start(): Boolean = runCatching {
        val out = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        rec.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64000)
            setAudioSamplingRate(44100)
            setOutputFile(out.absolutePath)
            prepare()
            start()
        }
        recorder = rec
        file = out
        true
    }.getOrElse { false }

    /** Останавливает запись и возвращает байты (или null, если слишком коротко/ошибка). */
    fun stop(): ByteArray? {
        val r = recorder ?: return null
        val f = file
        recorder = null
        val bytes = runCatching {
            r.stop()
            r.release()
            f?.readBytes()
        }.getOrNull()
        runCatching { f?.delete() }
        file = null
        return bytes?.takeIf { it.size > 1000 }
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { file?.delete() }
        file = null
    }
}
