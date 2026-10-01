package com.marbob303.yaoshaizi

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.Executors
import kotlin.math.exp
import kotlin.math.max
import kotlin.random.Random

/**
 * 骰子碰撞音效：用 AudioTrack 实时合成短促噪声 burst，不依赖任何音频资源文件。
 * 音色为低通后的指数衰减噪声，模拟骰子/骰盅的"哒哒"声。
 */
class SoundEngine {

    var muted by mutableStateOf(false)

    private val sampleRate = 22050
    // AudioTrack 可能初始化失败（如音频服务暂不可用）；为 null 时整机静默，
    // 绝不能因为音频问题导致启动闪退——这是 0.10 闪退的首要嫌疑点。
    private val track: AudioTrack?
    private val executor = Executors.newSingleThreadExecutor()
    private var lastPlayAt = 0L

    init {
        track = try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(max(minBuf, sampleRate / 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
                .also { it.play() }
        } catch (_: Exception) {
            null
        }
    }

    /** 播放一次碰撞声，intensity 0..1；内部节流避免过密 */
    fun clack(intensity: Float) {
        val t = track ?: return // 音频不可用：静默跳过
        if (muted) return
        val now = SystemClock.uptimeMillis()
        if (now - lastPlayAt < 40) return
        lastPlayAt = now
        val k = intensity.coerceIn(0.15f, 1f)
        executor.execute { writeBurst(t, k) }
    }

    private fun writeBurst(t: AudioTrack, intensity: Float) {
        try {
            val n = (sampleRate * 0.07).toInt() // 70ms
            val buf = ShortArray(n)
            val rnd = Random(SystemClock.uptimeMillis())
            var lp = 0f
            for (i in 0 until n) {
                val t = i / n.toFloat()
                val env = exp(-t * 9.0).toFloat() * (1f - t * 0.3f)
                val noise = rnd.nextFloat() * 2f - 1f
                lp += 0.35f * (noise - lp) // 简易低通，让声音更"木质"
                // 开头加一点高频敲击瞬态
                val transient = if (i < n / 12) (rnd.nextFloat() * 2f - 1f) * 0.6f else 0f
                val s = (lp * env + transient * env * 0.5f) * intensity
                buf[i] = (s * 26000).toInt().toShort()
            }
            t.write(buf, 0, n, AudioTrack.WRITE_BLOCKING)
        } catch (_: Exception) {
            // 音频不可用时静默忽略，不影响游戏
        }
    }

    fun release() {
        executor.shutdown()
        try {
            track?.stop()
            track?.release()
        } catch (_: Exception) {
        }
    }
}
