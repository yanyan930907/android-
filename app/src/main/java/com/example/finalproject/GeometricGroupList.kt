package com.example.finalproject

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class Group(
    val id: String = "",
    val name: String = "",
    val shapeType: String = "Circle", // Circle, Square, Triangle, Pentagon
    val color: Color = Color.Blue,
    val animationSpeed: Int = 2000 // duration in ms
)

@Composable
fun GeometricGroupList(groups: List<Group>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(groups) { group ->
            GeometricGroupItem(group)
        }
    }
}

@Composable
fun GeometricGroupItem(group: Group) {
    val infiniteTransition = rememberInfiniteTransition(label = "shapeAnimation")
    
    // 動態變化：縮放與旋轉
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(group.animationSpeed, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(group.animationSpeed * 2, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(150.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = rotation
                )
        ) {
            val size = this.size.minDimension
            val center = this.center

            when (group.shapeType) {
                "Circle" -> {
                    drawCircle(
                        color = group.color,
                        radius = size / 2,
                        style = Stroke(width = 8f)
                    )
                    drawCircle(
                        color = group.color.copy(alpha = 0.3f),
                        radius = size / 3
                    )
                }
                "Square" -> {
                    drawRect(
                        color = group.color,
                        style = Stroke(width = 8f)
                    )
                    drawRect(
                        color = group.color.copy(alpha = 0.3f),
                        topLeft = androidx.compose.ui.geometry.Offset(size * 0.2f, size * 0.2f),
                        size = androidx.compose.ui.geometry.Size(size * 0.6f, size * 0.6f)
                    )
                }
                "Triangle" -> {
                    val path = Path().apply {
                        moveTo(center.x, center.y - size / 2)
                        lineTo(center.x + size / 2, center.y + size / 2)
                        lineTo(center.x - size / 2, center.y + size / 2)
                        close()
                    }
                    drawPath(path, color = group.color, style = Stroke(width = 8f))
                    drawPath(path, color = group.color.copy(alpha = 0.3f))
                }
                else -> { // 多邊形 (例如五邊形)
                    val sides = 5
                    val path = Path().apply {
                        for (i in 0 until sides) {
                            val angle = 2.0 * PI * i / sides - PI / 2
                            val x = center.x + (size / 2) * cos(angle).toFloat()
                            val y = center.y + (size / 2) * sin(angle).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(path, color = group.color, style = Stroke(width = 8f))
                    drawPath(path, color = group.color.copy(alpha = 0.3f))
                }
            }
        }
        
        // 顯示群組名稱
        Text(
            text = group.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
