package com.example.compositionhelper.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.compositionhelper.model.CompositionType

@Composable
fun RecommendationChip(
    compositionType: CompositionType,
    score: Float,
    guidanceHint: String?,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        score > 0.8f -> Color.Green
        score > 0.5f -> Color.Yellow
        else -> Color.Red.copy(alpha = 0.6f)
    }

    Surface(
        modifier = modifier.clickable(onClick = onAccept),
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, scoreColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 分数环形指示器
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = score,
                    modifier = Modifier.size(36.dp),
                    color = scoreColor,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeWidth = 3.dp
                )
                Text(
                    "${(score * 100).toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            Column {
                Text(
                    compositionType.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                guidanceHint?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
