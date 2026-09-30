package com.marbob303.yaoshaizi

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val game = DiceGame()
    private val soundEngine = SoundEngine()

    // 低通滤波后的重力向量
    private val gravity = FloatArray(3)
    private var lastShakeAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        game.onClack = { intensity -> soundEngine.clack(intensity) }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0E2E1F)) {
                    var muted by remember { mutableStateOf(false) }

                    // 物理主循环：跟随屏幕刷新推进
                    LaunchedEffect(Unit) {
                        var last = 0L
                        while (true) {
                            withFrameNanos { t ->
                                if (last != 0L) {
                                    val dt = ((t - last) / 1e9).toFloat().coerceAtMost(0.05f)
                                    game.update(dt)
                                }
                                last = t
                            }
                        }
                    }

                    DiceScreen(
                        game = game,
                        muted = muted,
                        onMuteToggle = {
                            muted = !muted
                            soundEngine.muted = muted
                        },
                        onShakeClick = { game.roll() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        soundEngine.release()
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // 低通滤波分离出重力分量
        val alpha = 0.85f
        for (i in 0..2) {
            gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i]
        }

        // 重力方向 → 持续的轻微倾斜力（屏幕坐标：x 向右，y 向下）
        game.tiltX = (gravity[0] / 9.81f).coerceIn(-1f, 1f)
        game.tiltY = (-gravity[1] / 9.81f).coerceIn(-1f, 1f)

        // 剧烈晃动检测：线性加速度（去除重力后）的突变
        val lx = event.values[0] - gravity[0]
        val ly = event.values[1] - gravity[1]
        val lz = event.values[2] - gravity[2]
        val mag = sqrt(lx * lx + ly * ly + lz * lz)
        if (mag > 13f) {
            val now = System.currentTimeMillis()
            if (now - lastShakeAt > 1200L) { // 冷却，避免一次晃动触发多次
                lastShakeAt = now
                game.roll()
            }
        }
    }
}
