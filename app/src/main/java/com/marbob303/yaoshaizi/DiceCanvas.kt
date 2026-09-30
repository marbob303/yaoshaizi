package com.marbob303.yaoshaizi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

private val Gold = Color(0xFFE8C56A)
private val GoldDark = Color(0xFF9A7418)

@Composable
fun DiceScreen(
    game: DiceGame,
    muted: Boolean,
    onMuteToggle: () -> Unit,
    onShakeClick: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        val measurer = rememberTextMeasurer()
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawTable(w, h)
            drawTitle(measurer, w, h)
            drawTotal(measurer, game, w, h)
            drawCupAndDice(game, w, h)
            drawHint(measurer, game, w, h)
        }
        // 右上角静音开关
        IconButton(
            onClick = onMuteToggle,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp)
        ) {
            Icon(
                imageVector = if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = if (muted) "取消静音" else "静音",
                tint = Color(0xFFD8C690)
            )
        }
        // 底部"摇一摇"按钮（不想晃手机时点按触发）
        Button(
            onClick = onShakeClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB8860B),
                contentColor = Color(0xFF1A1206)
            )
        ) {
            Text("摇一摇", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 深色赌桌背景：深绿 → 深棕渐变 + 暗角 */
private fun DrawScope.drawTable(w: Float, h: Float) {
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFF0E2E1F), Color(0xFF241708)),
            startY = 0f,
            endY = h
        )
    )
    drawRect(
        Brush.radialGradient(
            0.0f to Color.Transparent,
            0.7f to Color.Transparent,
            1.0f to Color.Black.copy(alpha = 0.5f),
            center = Offset(w / 2f, h / 2f),
            radius = min(w, h) * 0.72f
        )
    )
}

private fun DrawScope.drawTitle(measurer: TextMeasurer, w: Float, h: Float) {
    val title = measurer.measure(
        "摇色子",
        style = TextStyle(
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold,
            color = Gold,
            textAlign = TextAlign.Center
        )
    )
    drawText(title, topLeft = Offset((w - title.size.width) / 2f, h * 0.055f))
    val sub = measurer.measure(
        "Yaoshaizi v0.10",
        style = TextStyle(fontSize = 15.sp, color = Color(0xFF9C8B66), textAlign = TextAlign.Center)
    )
    drawText(
        sub,
        topLeft = Offset((w - sub.size.width) / 2f, h * 0.055f + title.size.height + 2f)
    )
}

private fun DrawScope.drawTotal(measurer: TextMeasurer, game: DiceGame, w: Float, h: Float) {
    val labelText = if (game.rolling) "摇骰中…" else "总点数"
    val label = measurer.measure(
        labelText,
        style = TextStyle(fontSize = 16.sp, color = Color(0xFFB7A67E), textAlign = TextAlign.Center)
    )
    drawText(label, topLeft = Offset((w - label.size.width) / 2f, h * 0.175f))
    val num = measurer.measure(
        game.total.toString(),
        style = TextStyle(
            fontSize = 52.sp,
            fontWeight = FontWeight.Black,
            color = Gold,
            textAlign = TextAlign.Center
        )
    )
    drawText(
        num,
        topLeft = Offset((w - num.size.width) / 2f, h * 0.175f + label.size.height)
    )
}

/** 骰盅（俯视椭圆：金色盅口 → 黑色盅身 → 红色内衬 → 深色盅底）+ 5 颗骰子 */
private fun DrawScope.drawCupAndDice(game: DiceGame, w: Float, h: Float) {
    val s = min(w, h) / 1000f
    val cx = w / 2f
    val cy = h * 0.52f
    // 虚拟坐标 (500,545) 对应盅心
    val ox = cx - DiceGame.CUP_CX * s
    val oy = cy - DiceGame.CUP_CY * s

    fun ovalSize(vrx: Float, vry: Float) = Size(vrx * 2f * s, vry * 2f * s)
    fun ovalTopLeft(vrx: Float, vry: Float) = Offset(cx - vrx * s, cy - vry * s)

    // 落地阴影
    val shTL = ovalTopLeft(408f, 326f)
    drawOval(
        Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(shTL.x, shTL.y + 16f * s),
        size = ovalSize(408f, 326f)
    )
    // 金色盅口
    drawOval(
        Brush.radialGradient(listOf(Gold, GoldDark), center = Offset(cx, cy), radius = 400f * s),
        topLeft = ovalTopLeft(400f, 318f),
        size = ovalSize(400f, 318f)
    )
    // 黑色盅身
    drawOval(
        Brush.radialGradient(
            listOf(Color(0xFF2B2B30), Color(0xFF0B0B0D)),
            center = Offset(cx, cy),
            radius = 368f * s
        ),
        topLeft = ovalTopLeft(368f, 290f),
        size = ovalSize(368f, 290f)
    )
    // 红色内衬
    drawOval(
        Brush.radialGradient(
            listOf(Color(0xFF932626), Color(0xFF471010)),
            center = Offset(cx, cy),
            radius = 340f * s
        ),
        topLeft = ovalTopLeft(340f, 266f),
        size = ovalSize(340f, 266f)
    )
    // 深色盅底
    drawOval(
        Brush.radialGradient(
            listOf(Color(0xFF26190F), Color(0xFF0D0805)),
            center = Offset(cx, cy),
            radius = 318f * s
        ),
        topLeft = ovalTopLeft(318f, 246f),
        size = ovalSize(318f, 246f)
    )

    // 5 颗骰子
    for (d in game.dice) {
        drawDie(ox + d.x * s, oy + d.y * s, DiceGame.DIE_R * 2f * s, d.angle, d.face)
    }
}

/** 一颗骰子：中国传统配色（1 点、4 点为红色大点，其余黑色） */
private fun DrawScope.drawDie(px: Float, py: Float, sizePx: Float, angleRad: Float, face: Int) {
    val half = sizePx / 2f
    val deg = Math.toDegrees(angleRad.toDouble()).toFloat()
    // 阴影（不跟随旋转）
    drawRoundRect(
        Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(px - half + 5f, py - half + 9f),
        size = Size(sizePx, sizePx),
        cornerRadius = CornerRadius(sizePx * 0.18f, sizePx * 0.18f)
    )
    rotate(deg, pivot = Offset(px, py)) {
        // 白色盅体
        drawRoundRect(
            Color(0xFFF6F1E5),
            topLeft = Offset(px - half, py - half),
            size = Size(sizePx, sizePx),
            cornerRadius = CornerRadius(sizePx * 0.18f, sizePx * 0.18f)
        )
        drawRoundRect(
            Color(0xFFB7AF9E),
            topLeft = Offset(px - half, py - half),
            size = Size(sizePx, sizePx),
            cornerRadius = CornerRadius(sizePx * 0.18f, sizePx * 0.18f),
            style = Stroke(sizePx * 0.035f)
        )
        val o = sizePx * 0.27f
        val rBlack = sizePx * 0.095f
        val rRed = sizePx * 0.13f
        val black = Color(0xFF191919)
        val red = Color(0xFFC62828)
        fun pip(dx: Float, dy: Float, c: Color, r: Float) {
            drawCircle(c, radius = r, center = Offset(px + dx, py + dy))
        }
        when (face) {
            1 -> pip(0f, 0f, red, rRed * 1.3f)
            2 -> {
                pip(o, -o, black, rBlack); pip(-o, o, black, rBlack)
            }
            3 -> {
                pip(o, -o, black, rBlack); pip(0f, 0f, black, rBlack); pip(-o, o, black, rBlack)
            }
            4 -> {
                pip(o, -o, red, rRed); pip(-o, -o, red, rRed)
                pip(o, o, red, rRed); pip(-o, o, red, rRed)
            }
            5 -> {
                pip(o, -o, black, rBlack); pip(-o, -o, black, rBlack); pip(0f, 0f, black, rBlack)
                pip(o, o, black, rBlack); pip(-o, o, black, rBlack)
            }
            else -> { // 6
                pip(o, -o, black, rBlack); pip(o, 0f, black, rBlack); pip(o, o, black, rBlack)
                pip(-o, -o, black, rBlack); pip(-o, 0f, black, rBlack); pip(-o, o, black, rBlack)
            }
        }
    }
}

private fun DrawScope.drawHint(measurer: TextMeasurer, game: DiceGame, w: Float, h: Float) {
    val text = if (game.rolling) "骰子翻滚中…" else "晃动手机开始摇骰"
    val layout = measurer.measure(
        text,
        style = TextStyle(fontSize = 16.sp, color = Color(0xFF8F8265), textAlign = TextAlign.Center)
    )
    drawText(layout, topLeft = Offset((w - layout.size.width) / 2f, h * 0.845f))
}
