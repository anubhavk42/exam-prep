package com.anubhav.diprep.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = listOf(
    Color(0xFFC9622F),
    Color(0xFF1F9E5E),
    Color(0xFFD4860A),
    Color(0xFF3B82F6),
    Color(0xFFEC4899)
)

private data class Particle(
    val xRatio: Float,
    val speed: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val initialAngle: Float,
    val rotationSpeed: Float,
    val swayAmplitude: Float,
    val isCircle: Boolean
)

@Composable
fun ConfettiEffect(onDismiss: () -> Unit) {
    val progress = remember { Animatable(0f) }

    val particles = remember {
        List(60) {
            Particle(
                xRatio = Random.nextFloat(),
                speed = 0.7f + Random.nextFloat() * 0.6f,
                width = 8f + Random.nextFloat() * 12f,
                height = 8f + Random.nextFloat() * 14f,
                color = ConfettiColors[Random.nextInt(ConfettiColors.size)],
                initialAngle = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                swayAmplitude = 15f + Random.nextFloat() * 25f,
                isCircle = Random.nextBoolean()
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2500, easing = LinearEasing)
        )
        onDismiss()
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("confetti_canvas")
    ) {
        val currentProgress = progress.value
        val screenWidth = size.width
        val screenHeight = size.height

        particles.forEach { p ->
            val yPos = currentProgress * (screenHeight + 100f) * p.speed - 50f
            val sway = sin((currentProgress * 4.0 * Math.PI + p.initialAngle).toFloat()) * p.swayAmplitude
            val xPos = (p.xRatio * screenWidth) + sway
            val rotation = p.initialAngle + (currentProgress * p.rotationSpeed)

            if (yPos in -50f..screenHeight + 50f) {
                rotate(degrees = rotation, pivot = Offset(xPos, yPos)) {
                    if (p.isCircle) {
                        drawCircle(
                            color = p.color,
                            radius = p.width / 2,
                            center = Offset(xPos, yPos)
                        )
                    } else {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(xPos - p.width / 2, yPos - p.height / 2),
                            size = Size(p.width, p.height)
                        )
                    }
                }
            }
        }
    }
}
