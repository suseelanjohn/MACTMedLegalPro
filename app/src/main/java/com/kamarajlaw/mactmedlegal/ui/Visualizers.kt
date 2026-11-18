package com.kamarajlaw.mactmedlegal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kamarajlaw.mactmedlegal.data.InjuryPinEntity
import com.kamarajlaw.mactmedlegal.data.SceneItemEntity

@Composable
fun AnatomicalBodyCanvas(
    pins: List<InjuryPinEntity>,
    isPosterior: Boolean,
    onAddPin: (normX: Float, normY: Float) -> Unit,
    onSelectPin: (InjuryPinEntity) -> Unit
) {
    val visiblePins = pins.filter { it.isPosterior == isPosterior }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
            .pointerInput(isPosterior, visiblePins) {
                detectTapGestures { offset ->
                    val nx = (offset.x / size.width).coerceIn(0f, 1f)
                    val ny = (offset.y / size.height).coerceIn(0f, 1f)
                    val hit = visiblePins.minByOrNull {
                        val dx = (it.normX - nx) * size.width
                        val dy = (it.normY - ny) * size.height
                        dx * dx + dy * dy
                    }
                    if (hit != null) {
                        val dx = (hit.normX - nx) * size.width
                        val dy = (hit.normY - ny) * size.height
                        if (dx * dx + dy * dy <= 36f * 36f) {
                            onSelectPin(hit)
                            return@detectTapGestures
                        }
                    }
                    onAddPin(nx, ny)
                }
            }
    ) {
        val cx = size.width / 2f
        val bodyColor = Color(0xFFCBD5E1)

        drawCircle(color = bodyColor, radius = size.minDimension * 0.08f, center = Offset(cx, size.height * 0.12f))
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(cx - size.width * 0.13f, size.height * 0.22f),
            size = Size(size.width * 0.26f, size.height * 0.32f),
            cornerRadius = CornerRadius(24f, 24f)
        )
        drawRoundRect(color = bodyColor, topLeft = Offset(cx - size.width * 0.22f, size.height * 0.24f), size = Size(size.width * 0.07f, size.height * 0.28f))
        drawRoundRect(color = bodyColor, topLeft = Offset(cx + size.width * 0.15f, size.height * 0.24f), size = Size(size.width * 0.07f, size.height * 0.28f))
        drawRoundRect(color = bodyColor, topLeft = Offset(cx - size.width * 0.11f, size.height * 0.56f), size = Size(size.width * 0.09f, size.height * 0.36f))
        drawRoundRect(color = bodyColor, topLeft = Offset(cx + size.width * 0.02f, size.height * 0.56f), size = Size(size.width * 0.09f, size.height * 0.36f))

        visiblePins.forEach { pin ->
            val pinColor = when (pin.severity) {
                "GRIEVOUS" -> Color(0xFFDC2626)
                "NERVE" -> Color(0xFF7C3AED)
                else -> Color(0xFFD97706)
            }
            val pos = Offset(pin.normX * size.width, pin.normY * size.height)
            drawCircle(color = pinColor.copy(alpha = 0.25f), radius = 24f, center = pos)
            drawCircle(color = pinColor, radius = 12f, center = pos)
        }
    }
}

@Composable
fun AccidentSceneCanvas(
    roadLayout: String,
    items: List<SceneItemEntity>,
    onMoveItem: (SceneItemEntity, Float, Float) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .pointerInput(items) {
                var dragged: SceneItemEntity? = null
                detectDragGestures(
                    onDragStart = { start ->
                        dragged = items.minByOrNull {
                            val dx = it.normX * size.width - start.x
                            val dy = it.normY * size.height - start.y
                            dx * dx + dy * dy
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragged?.let { item ->
                            val nx = ((item.normX * size.width + dragAmount.x) / size.width).coerceIn(0.05f, 0.95f)
                            val ny = ((item.normY * size.height + dragAmount.y) / size.height).coerceIn(0.05f, 0.95f)
                            onMoveItem(item, nx, ny)
                            dragged = item.copy(normX = nx, normY = ny)
                        }
                    }
                )
            }
    ) {
        val roadColor = Color(0xFF334155)
        val dash = PathEffect.dashPathEffect(floatArrayOf(24f, 18f), 0f)

        drawRect(
            color = roadColor,
            topLeft = Offset(0f, size.height * 0.30f),
            size = Size(size.width, size.height * 0.40f)
        )
        if (roadLayout == "CROSSROADS" || roadLayout == "T_JUNCTION") {
            val topY = if (roadLayout == "T_JUNCTION") size.height * 0.30f else 0f
            val h = if (roadLayout == "T_JUNCTION") size.height * 0.70f else size.height
            drawRect(
                color = roadColor,
                topLeft = Offset(size.width * 0.35f, topY),
                size = Size(size.width * 0.30f, h)
            )
        }
        drawLine(
            color = Color.White,
            start = Offset(0f, size.height * 0.50f),
            end = Offset(size.width, size.height * 0.50f),
            strokeWidth = 4f,
            pathEffect = dash
        )

        items.forEach { item ->
            val pos = Offset(item.normX * size.width, item.normY * size.height)
            rotate(degrees = item.rotationDeg, pivot = pos) {
                when (item.itemType) {
                    "IMPACT_X" -> drawCircle(color = Color(0xFFEF4444), radius = 16f, center = pos)
                    "LORRY" -> drawRoundRect(color = Color(0xFFF59E0B), topLeft = Offset(pos.x - 38f, pos.y - 16f), size = Size(76f, 32f))
                    "BIKE" -> drawRoundRect(color = Color(0xFF38BDF8), topLeft = Offset(pos.x - 20f, pos.y - 8f), size = Size(40f, 16f))
                    else -> drawRoundRect(color = Color(0xFF10B981), topLeft = Offset(pos.x - 28f, pos.y - 14f), size = Size(56f, 28f))
                }
            }
        }
    }
}
