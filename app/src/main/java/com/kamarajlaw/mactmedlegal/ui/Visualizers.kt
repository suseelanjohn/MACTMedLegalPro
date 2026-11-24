package com.kamarajlaw.mactmedlegal.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kamarajlaw.mactmedlegal.data.InjuryPinEntity
import com.kamarajlaw.mactmedlegal.data.SceneItemEntity
import kotlin.math.hypot

fun inferAnatomicalRegion(nx: Float, ny: Float, isPosterior: Boolean): String {
    val side = if (nx < 0.45f) "Right" else if (nx > 0.55f) "Left" else "Midline"
    return when {
        ny < 0.16f -> if (isPosterior) "Occipital Cranium" else "Cranium / Facial"
        ny < 0.21f -> if (isPosterior) "Cervical Spine (C1-C7)" else "Neck / Clavicle"
        ny < 0.38f -> when {
            nx < 0.33f || nx > 0.67f -> "$side Upper Arm / Shoulder"
            isPosterior -> "Thoracic Spine / Scapula"
            else -> "$side Thorax / Ribs"
        }
        ny < 0.52f -> when {
            nx < 0.30f || nx > 0.70f -> "$side Forearm / Wrist"
            isPosterior -> "Lumbar Spine (L1-L5)"
            else -> "Abdomen / Pelvis"
        }
        ny < 0.72f -> "$side Femur / Thigh"
        ny < 0.90f -> "$side Tibia-Fibula / Knee"
        else -> "$side Ankle / Foot"
    }
}

@Composable
fun AnatomicalBodyCanvas(
    pins: List<InjuryPinEntity>,
    isPosterior: Boolean,
    selectedPinId: Long?,
    onAddPin: (normX: Float, normY: Float, regionName: String) -> Unit,
    onSelectPin: (InjuryPinEntity) -> Unit
) {
    val visiblePins = remember(pins, isPosterior) { pins.filter { it.isPosterior == isPosterior } }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(20.dp))
            .pointerInput(isPosterior, visiblePins) {
                detectTapGestures { offset ->
                    val nx = (offset.x / size.width).coerceIn(0.15f, 0.85f)
                    val ny = (offset.y / size.height).coerceIn(0.05f, 0.95f)
                    val hit = visiblePins.minByOrNull {
                        hypot((it.normX - nx) * size.width, (it.normY - ny) * size.height)
                    }
                    if (hit != null && hypot((hit.normX - nx) * size.width, (hit.normY - ny) * size.height) <= 44f) {
                        onSelectPin(hit)
                    } else {
                        onAddPin(nx, ny, inferAnatomicalRegion(nx, ny, isPosterior))
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        val gridPaint = Color(0xFFE2E8F0)
        for (i in 1..9) {
            drawLine(gridPaint, Offset(0f, h * i / 10f), Offset(w, h * i / 10f), strokeWidth = 1f)
            drawLine(gridPaint, Offset(w * i / 10f, 0f), Offset(w * i / 10f, h), strokeWidth = 1f)
        }
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(cx, h * 0.04f),
            end = Offset(cx, h * 0.96f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        val skinFill = Color(0xFFE2E8F0)
        val skinStroke = Color(0xFF64748B)

        drawCircle(color = skinFill, radius = h * 0.062f, center = Offset(cx, h * 0.105f))
        drawCircle(color = skinStroke, radius = h * 0.062f, center = Offset(cx, h * 0.105f), style = Stroke(2.5f))
        drawRect(color = skinFill, topLeft = Offset(cx - w * 0.032f, h * 0.162f), size = Size(w * 0.064f, h * 0.042f))

        val torso = Path().apply {
            moveTo(cx - w * 0.135f, h * 0.205f)
            lineTo(cx + w * 0.135f, h * 0.205f)
            lineTo(cx + w * 0.105f, h * 0.37f)
            lineTo(cx + w * 0.12f, h * 0.49f)
            lineTo(cx - w * 0.12f, h * 0.49f)
            lineTo(cx - w * 0.105f, h * 0.37f)
            close()
        }
        drawPath(torso, color = skinFill)
        drawPath(torso, color = skinStroke, style = Stroke(2.5f))

        if (isPosterior) {
            for (v in 0..11) {
                val vy = h * (0.21f + v * 0.022f)
                drawRoundRect(
                    color = Color(0xFF94A3B8),
                    topLeft = Offset(cx - 7f, vy),
                    size = Size(14f, 6f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
        } else {
            for (r in 0..4) {
                val ry = h * (0.24f + r * 0.024f)
                drawLine(Color(0xFF94A3B8), Offset(cx - w * 0.08f, ry), Offset(cx - 6f, ry + 5f), strokeWidth = 2f)
                drawLine(Color(0xFF94A3B8), Offset(cx + w * 0.08f, ry), Offset(cx + 6f, ry + 5f), strokeWidth = 2f)
            }
        }

        val leftArm = Path().apply {
            moveTo(cx - w * 0.135f, h * 0.21f); lineTo(cx - w * 0.22f, h * 0.36f); lineTo(cx - w * 0.25f, h * 0.51f)
            lineTo(cx - w * 0.20f, h * 0.52f); lineTo(cx - w * 0.165f, h * 0.37f); lineTo(cx - w * 0.125f, h * 0.27f); close()
        }
        val rightArm = Path().apply {
            moveTo(cx + w * 0.135f, h * 0.21f); lineTo(cx + w * 0.22f, h * 0.36f); lineTo(cx + w * 0.25f, h * 0.51f)
            lineTo(cx + w * 0.20f, h * 0.52f); lineTo(cx + w * 0.165f, h * 0.37f); lineTo(cx + w * 0.125f, h * 0.27f); close()
        }
        drawPath(leftArm, skinFill); drawPath(leftArm, skinStroke, style = Stroke(2.5f))
        drawPath(rightArm, skinFill); drawPath(rightArm, skinStroke, style = Stroke(2.5f))

        val leftLeg = Path().apply {
            moveTo(cx - w * 0.115f, h * 0.49f); lineTo(cx - w * 0.015f, h * 0.49f); lineTo(cx - w * 0.03f, h * 0.72f)
            lineTo(cx - w * 0.04f, h * 0.93f); lineTo(cx - w * 0.10f, h * 0.93f); lineTo(cx - w * 0.095f, h * 0.72f); close()
        }
        val rightLeg = Path().apply {
            moveTo(cx + w * 0.015f, h * 0.49f); lineTo(cx + w * 0.115f, h * 0.49f); lineTo(cx + w * 0.095f, h * 0.72f)
            lineTo(cx + w * 0.10f, h * 0.93f); lineTo(cx + w * 0.04f, h * 0.93f); lineTo(cx + w * 0.03f, h * 0.72f); close()
        }
        drawPath(leftLeg, skinFill); drawPath(leftLeg, skinStroke, style = Stroke(2.5f))
        drawPath(rightLeg, skinFill); drawPath(rightLeg, skinStroke, style = Stroke(2.5f))

        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val sidePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#64748B")
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        drawIntoCanvas { c ->
            c.nativeCanvas.drawText(if (isPosterior) "LEFT" else "RIGHT", 24f, 36f, sidePaint)
            c.nativeCanvas.drawText(if (isPosterior) "RIGHT" else "LEFT", w - 95f, 36f, sidePaint)
        }

        visiblePins.forEachIndexed { idx, pin ->
            val pinColor = when (pin.severity) {
                "GRIEVOUS" -> Color(0xFFDC2626)
                "NERVE" -> Color(0xFF7C3AED)
                else -> Color(0xFFD97706)
            }
            val pos = Offset(pin.normX * w, pin.normY * h)
            val isSelected = pin.id == selectedPinId
            drawCircle(color = pinColor.copy(alpha = if (isSelected) 0.35f else 0.20f), radius = if (isSelected) 34f else 24f, center = pos)
            if (isSelected) {
                drawCircle(color = pinColor, radius = 24f, center = pos, style = Stroke(3f))
            }
            drawCircle(color = pinColor, radius = 16f, center = pos)
            drawIntoCanvas { c ->
                c.nativeCanvas.drawText("${idx + 1}", pos.x, pos.y + 9f, textPaint)
            }
        }
    }
}

@Composable
fun AccidentSceneCanvas(
    roadLayout: String,
    items: List<SceneItemEntity>,
    selectedItemId: Long?,
    onSelectItem: (SceneItemEntity) -> Unit,
    onMoveItem: (SceneItemEntity, Float, Float) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFDCFCE7))
            .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(20.dp))
            .pointerInput(items) {
                detectTapGestures { tap ->
                    val hit = items.minByOrNull { hypot(it.normX * size.width - tap.x, it.normY * size.height - tap.y) }
                    if (hit != null && hypot(hit.normX * size.width - tap.x, hit.normY * size.height - tap.y) <= 58f) {
                        onSelectItem(hit)
                    }
                }
            }
            .pointerInput(items) {
                var dragged: SceneItemEntity? = null
                detectDragGestures(
                    onDragStart = { start ->
                        dragged = items.minByOrNull { hypot(it.normX * size.width - start.x, it.normY * size.height - start.y) }
                        dragged?.let(onSelectItem)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragged?.let { item ->
                            val nx = ((item.normX * size.width + dragAmount.x) / size.width).coerceIn(0.06f, 0.94f)
                            val ny = ((item.normY * size.height + dragAmount.y) / size.height).coerceIn(0.08f, 0.92f)
                            onMoveItem(item, nx, ny)
                            dragged = item.copy(normX = nx, normY = ny)
                        }
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val asphalt = Color(0xFF334155)
        val dash = PathEffect.dashPathEffect(floatArrayOf(22f, 16f), 0f)

        // Roadside Shoulder / Mud Verge
        drawRect(color = Color(0xFFE7E5E4), topLeft = Offset(0f, h * 0.24f), size = Size(w, h * 0.52f))

        // Main Horizontal Highway
        drawRect(color = asphalt, topLeft = Offset(0f, h * 0.28f), size = Size(w, h * 0.44f))
        drawLine(Color(0xFFFACC15), Offset(0f, h * 0.29f), Offset(w, h * 0.29f), strokeWidth = 3f)
        drawLine(Color(0xFFFACC15), Offset(0f, h * 0.71f), Offset(w, h * 0.71f), strokeWidth = 3f)

        if (roadLayout == "FOUR_LANE") {
            drawRect(color = Color(0xFF16A34A), topLeft = Offset(0f, h * 0.485f), size = Size(w, h * 0.03f))
            drawLine(Color.White, Offset(0f, h * 0.39f), Offset(w, h * 0.39f), strokeWidth = 2.5f, pathEffect = dash)
            drawLine(Color.White, Offset(0f, h * 0.61f), Offset(w, h * 0.61f), strokeWidth = 2.5f, pathEffect = dash)
        } else {
            drawLine(Color.White, Offset(0f, h * 0.50f), Offset(w, h * 0.50f), strokeWidth = 3.5f, pathEffect = dash)
        }

        if (roadLayout == "CROSSROADS" || roadLayout == "T_JUNCTION") {
            val topY = if (roadLayout == "T_JUNCTION") h * 0.50f else 0f
            val roadH = if (roadLayout == "T_JUNCTION") h * 0.50f else h
            drawRect(color = asphalt, topLeft = Offset(w * 0.36f, topY), size = Size(w * 0.28f, roadH))
            drawLine(Color.White, Offset(w * 0.50f, topY), Offset(w * 0.50f, topY + roadH), strokeWidth = 3f, pathEffect = dash)
        }

        // Compass Rose (North Indicator) & Lane Direction Hints
        val compassPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0F2942")
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val laneHintPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 18f
            isAntiAlias = true
        }
        drawIntoCanvas { c ->
            c.nativeCanvas.drawText("N ↑", w - 58f, 32f, compassPaint)
            c.nativeCanvas.drawText("← WESTBOUND LANE", 18f, h * 0.41f, laneHintPaint)
            c.nativeCanvas.drawText("EASTBOUND LANE →", w - 195f, h * 0.65f, laneHintPaint)
        }

        val labelPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 21f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        items.forEach { item ->
            val pos = Offset(item.normX * w, item.normY * h)
            val isSel = item.id == selectedItemId
            rotate(degrees = item.rotationDeg, pivot = pos) {
                when (item.itemType) {
                    "IMPACT_X" -> {
                        drawCircle(Color(0xFFEF4444).copy(alpha = 0.28f), 30f, pos)
                        drawCircle(Color(0xFFDC2626), 18f, pos)
                        drawLine(Color.White, Offset(pos.x - 8f, pos.y - 8f), Offset(pos.x + 8f, pos.y + 8f), strokeWidth = 3.5f)
                        drawLine(Color.White, Offset(pos.x + 8f, pos.y - 8f), Offset(pos.x - 8f, pos.y + 8f), strokeWidth = 3.5f)
                    }
                    "SKID" -> {
                        drawLine(Color(0xFF0F172A), Offset(pos.x - 42f, pos.y - 6f), Offset(pos.x + 42f, pos.y - 6f), strokeWidth = 4.5f, pathEffect = dash)
                        drawLine(Color(0xFF0F172A), Offset(pos.x - 42f, pos.y + 6f), Offset(pos.x + 42f, pos.y + 6f), strokeWidth = 4.5f, pathEffect = dash)
                    }
                    "ARROW_R" -> {
                        drawLine(Color(0xFF38BDF8), Offset(pos.x - 32f, pos.y), Offset(pos.x + 28f, pos.y), strokeWidth = 5f)
                        drawLine(Color(0xFF38BDF8), Offset(pos.x + 14f, pos.y - 12f), Offset(pos.x + 28f, pos.y), strokeWidth = 5f)
                        drawLine(Color(0xFF38BDF8), Offset(pos.x + 14f, pos.y + 12f), Offset(pos.x + 28f, pos.y), strokeWidth = 5f)
                    }
                    "ZEBRA" -> {
                        for (z in -2..2) {
                            drawRect(Color.White, Offset(pos.x - 16f, pos.y + z * 11f - 3f), Size(32f, 6f))
                        }
                    }
                    "LORRY" -> {
                        drawRoundRect(Color(0xFFF59E0B), Offset(pos.x - 44f, pos.y - 18f), Size(88f, 36f), CornerRadius(6f, 6f))
                        drawRect(Color(0xFFB45309), Offset(pos.x + 22f, pos.y - 16f), Size(20f, 32f))
                        // Headlights
                        drawCircle(Color(0xFFFEF08A), 3.5f, Offset(pos.x + 42f, pos.y - 11f))
                        drawCircle(Color(0xFFFEF08A), 3.5f, Offset(pos.x + 42f, pos.y + 11f))
                    }
                    "BIKE" -> {
                        drawRoundRect(Color(0xFF0284C7), Offset(pos.x - 24f, pos.y - 9f), Size(48f, 18f), CornerRadius(8f, 8f))
                        drawCircle(Color(0xFFFEF08A), 3f, Offset(pos.x + 22f, pos.y))
                    }
                    "PEDESTRIAN" -> {
                        drawCircle(Color(0xFFEC4899), 12f, pos)
                        drawCircle(Color.White, 12f, pos, style = Stroke(2f))
                    }
                    else -> {
                        drawRoundRect(Color(0xFF10B981), Offset(pos.x - 34f, pos.y - 15f), Size(68f, 30f), CornerRadius(8f, 8f))
                        drawRect(Color(0xFF047857), Offset(pos.x - 12f, pos.y - 12f), Size(28f, 24f))
                    }
                }
                if (isSel) {
                    drawCircle(Color.White, 28f, pos, style = Stroke(2.5f))
                }
            }
            drawIntoCanvas { c ->
                c.nativeCanvas.drawText(item.label, pos.x, pos.y - 25f, labelPaint)
            }
        }
    }
}
