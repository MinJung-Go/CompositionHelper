package com.example.compositionhelper.color

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun StudioActions(canAnalyze: Boolean, canSave: Boolean, onAnalyze: () -> Unit, onSave: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = LocalDensity.current.fontScale > 1.4f || maxWidth < 280.dp
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAnalyze, enabled = canAnalyze, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("AI 调色") }
                OutlinedButton(onClick = onSave, enabled = canSave, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("保存副本") }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onAnalyze, enabled = canAnalyze, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("AI 调色") }
                OutlinedButton(onClick = onSave, enabled = canSave, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("保存副本") }
            }
        }
    }
}

@Composable
internal fun StudioPhotoPreview(original: Bitmap, edited: Bitmap, mode: String, divider: Float, maxHeight: Dp) {
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
        val aspect = original.width.toFloat() / original.height
        val photoWidth = minOf(maxWidth, maxHeight * aspect)
        val photoHeight = photoWidth / aspect
        Box(Modifier.size(photoWidth, photoHeight)) {
            Image((if (mode == "调色后") edited else original).asImageBitmap(),
                if (mode == "调色后") "调色后" else "原图", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            if (mode == "左右对比") {
                Image(edited.asImageBitmap(), "调色后", Modifier.fillMaxSize().drawWithContent {
                    clipRect(left = size.width * divider.coerceIn(0f, 1f)) { this@drawWithContent.drawContent() }
                }, contentScale = ContentScale.Fit)
                Box(Modifier.offset(x = (photoWidth - 2.dp) * divider.coerceIn(0f, 1f)).width(2.dp).fillMaxHeight().background(Color.White))
            }
            if (mode != "调色后") PreviewBadge("原图", Modifier.align(Alignment.TopStart).padding(10.dp))
            if (mode != "原图") PreviewBadge("调色后", Modifier.align(Alignment.TopEnd).padding(10.dp))
        }
    }
}

@Composable
private fun PreviewBadge(text: String, modifier: Modifier) {
    Text(text, modifier.background(Color.Black.copy(alpha = .6f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, fontSize = 12.sp)
}
