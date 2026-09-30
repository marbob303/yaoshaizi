package com.marbob303.yaoshaizi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.SecureRandom
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 一颗骰子的物理状态。
 * 坐标系为 1000x1000 的虚拟正方形，绘制时再映射到屏幕像素，保证骰子始终是正圆/正方形。
 */
class DieState {
    var x by mutableFloatStateOf(500f)
    var y by mutableFloatStateOf(520f)
    var vx = 0f
    var vy = 0f
    var angle by mutableFloatStateOf(0f) // 弧度
    var angVel = 0f
    var face by mutableIntStateOf(1)     // 当前显示点数（滚动时会快速变化制造翻滚感）
    var finalFace = 1                   // 本次摇骰锁定的最终点数
}

/**
 * 轻量 2D 物理：5 颗骰子在骰盅椭圆边界内弹跳、骰子之间圆形碰撞、摩擦减速。
 * 不依赖任何物理引擎。
 */
class DiceGame {

    companion object {
        // 骰盅：骰子中心可活动的椭圆（已扣除骰子半径，绘制时内衬椭圆比它稍大）
        const val CUP_CX = 500f
        const val CUP_CY = 545f
        const val CUP_A = 250f
        const val CUP_B = 178f
        const val DIE_R = 72f
        // 低于此速度认为静止
        const val SETTLE_SPEED = 45f
        // 最长滚动时间（秒），超时强制静止
        const val MAX_ROLL_TIME = 2.6f
    }

    val dice = List(5) { DieState() }

    /** 5 颗骰子点数总和（静止后更新） */
    var total by mutableIntStateOf(15)
        private set

    /** 是否正在摇骰 */
    var rolling by mutableStateOf(false)
        private set

    /** 手机倾斜输入：-1..1，由加速度计低通滤波后的重力方向换算 */
    var tiltX = 0f
    var tiltY = 0f

    /** 碰撞音效回调（参数为强度 0..1） */
    var onClack: ((Float) -> Unit)? = null

    private val secureRandom = SecureRandom()
    private var settleTimer = 0f
    private var rollTime = 0f
    private var tumbleTimer = 0f

    init {
        // 初始摆位：均匀散在盅内
        val spots = listOf(
            500f to 545f,
            392f to 486f,
            608f to 486f,
            420f to 636f,
            580f to 636f,
        )
        dice.forEachIndexed { i, d ->
            d.x = spots[i].first
            d.y = spots[i].second
            d.angle = secureRandom.nextFloat() * (Math.PI * 2).toFloat()
            d.face = 1 + secureRandom.nextInt(6)
            d.finalFace = d.face
        }
        updateTotal()
    }

    /** 触发一次摇骰：随机初速度 + 角速度 + 最终点数 */
    fun roll() {
        if (rolling) return
        rolling = true
        rollTime = 0f
        settleTimer = 0f
        tumbleTimer = 0f
        for (d in dice) {
            val a = secureRandom.nextFloat() * (Math.PI * 2).toFloat()
            val speed = 750f + secureRandom.nextFloat() * 950f
            d.vx = cos(a) * speed
            d.vy = sin(a) * speed
            d.angVel = (secureRandom.nextFloat() - 0.5f) * 22f
            d.finalFace = 1 + secureRandom.nextInt(6)
        }
    }

    /** 每帧推进物理，dt 为秒 */
    fun update(dt: Float) {
        if (!rolling) return
        rollTime += dt
        val tiltForce = 900f

        for (d in dice) {
            // 手机倾斜带来的持续轻微推力
            d.vx += tiltX * tiltForce * dt
            d.vy += tiltY * tiltForce * dt
            // 摩擦减速
            val damp = max(0f, 1f - 1.9f * dt)
            d.vx *= damp
            d.vy *= damp
            d.angVel *= max(0f, 1f - 2.2f * dt)
            // 积分
            d.x += d.vx * dt
            d.y += d.vy * dt
            d.angle += d.angVel * dt
            collideCup(d)
        }
        collideDice()

        // 翻滚感：滚动中骰面快速随机变化
        tumbleTimer += dt
        if (tumbleTimer > 0.09f) {
            tumbleTimer = 0f
            for (d in dice) d.face = 1 + secureRandom.nextInt(6)
        }

        // 高速滚动时的环境碰撞声
        var avgSpeed = 0f
        for (d in dice) avgSpeed += hypot(d.vx, d.vy)
        avgSpeed /= dice.size
        if (avgSpeed > 250f && Random.nextFloat() < dt * 6f) {
            onClack?.invoke((avgSpeed / 1600f).coerceIn(0.2f, 0.8f))
        }

        // 静止判定：全部足够慢并持续一小段时间
        var allSlow = true
        for (d in dice) {
            if (hypot(d.vx, d.vy) > SETTLE_SPEED || abs(d.angVel) > 1.6f) {
                allSlow = false
                break
            }
        }
        if (allSlow) settleTimer += dt else settleTimer = 0f

        if (settleTimer > 0.35f || rollTime > MAX_ROLL_TIME) {
            rolling = false
            for (d in dice) {
                d.vx = 0f
                d.vy = 0f
                d.angVel = 0f
                d.face = d.finalFace
            }
            updateTotal()
        }
    }

    private fun updateTotal() {
        total = dice.sumOf { it.face }
    }

    /** 骰盅椭圆边界碰撞：沿椭圆法线反射速度 */
    private fun collideCup(d: DieState) {
        val dx = d.x - CUP_CX
        val dy = d.y - CUP_CY
        val p = (dx / CUP_A) * (dx / CUP_A) + (dy / CUP_B) * (dy / CUP_B)
        if (p > 1f) {
            // 拉回椭圆内部
            val k = 1f / sqrt(p)
            d.x = CUP_CX + dx * k
            d.y = CUP_CY + dy * k
            // 椭圆在该点的外法线 = 梯度方向
            var nx = dx / (CUP_A * CUP_A)
            var ny = dy / (CUP_B * CUP_B)
            val nl = sqrt(nx * nx + ny * ny)
            nx /= nl
            ny /= nl
            val vn = d.vx * nx + d.vy * ny
            if (vn > 0f) {
                val restitution = 0.55f
                d.vx -= (1f + restitution) * vn * nx
                d.vy -= (1f + restitution) * vn * ny
                d.angVel += (secureRandom.nextFloat() - 0.5f) * 6f
                onClack?.invoke((vn / 1600f).coerceIn(0.15f, 1f))
            }
        }
    }

    /** 骰子之间圆形碰撞（等质量弹性碰撞 + 恢复系数） */
    private fun collideDice() {
        val minDist = DIE_R * 2f
        for (i in dice.indices) {
            for (j in i + 1 until dice.size) {
                val a = dice[i]
                val b = dice[j]
                var dx = b.x - a.x
                var dy = b.y - a.y
                var dist = sqrt(dx * dx + dy * dy)
                if (dist < minDist) {
                    if (dist < 0.001f) {
                        dx = 1f
                        dy = 0f
                        dist = 1f
                    }
                    val nx = dx / dist
                    val ny = dy / dist
                    // 位置分离，避免重叠
                    val overlap = (minDist - dist) / 2f
                    a.x -= nx * overlap
                    a.y -= ny * overlap
                    b.x += nx * overlap
                    b.y += ny * overlap
                    // 法向分量交换
                    val rvx = b.vx - a.vx
                    val rvy = b.vy - a.vy
                    val vn = rvx * nx + rvy * ny
                    if (vn < 0f) {
                        val restitution = 0.6f
                        val impulse = -(1f + restitution) * vn / 2f
                        a.vx -= impulse * nx
                        a.vy -= impulse * ny
                        b.vx += impulse * nx
                        b.vy += impulse * ny
                        onClack?.invoke((-vn / 1600f).coerceIn(0.15f, 1f))
                    }
                }
            }
        }
    }
}
