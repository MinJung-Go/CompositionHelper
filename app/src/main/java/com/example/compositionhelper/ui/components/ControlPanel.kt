package com.example.compositionhelper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compositionhelper.model.CompositionCategory
import com.example.compositionhelper.model.CompositionType
import com.example.compositionhelper.model.getCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraBottomBar(
    selectedComposition: CompositionType,
    selectedCategory: CompositionCategory,
    lineOpacity: Float,
    lineColor: Color,
    onCompositionChange: (CompositionType) -> Unit,
    onCategoryChange: (CompositionCategory) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onColorChange: (Color) -> Unit,
    onCapture: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // 分类选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionCategory.values().forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(category.displayName, fontSize = 11.sp) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = Color.White.copy(alpha = 0.7f),
                        selectedContainerColor = Color.White.copy(alpha = 0.2f),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // 构图类型横向滚动选择器
        val filteredTypes = CompositionType.values().filter { it.getCategory() == selectedCategory }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(vertical = 6.dp)
        ) {
            items(filteredTypes) { type ->
                FilterChip(
                    selected = selectedComposition == type,
                    onClick = { onCompositionChange(type) },
                    label = { Text(type.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = Color.White.copy(alpha = 0.7f),
                        selectedContainerColor = Color.White.copy(alpha = 0.3f),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // 主控制行：设置、快门、相册
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Tune, "设置", tint = Color.White)
            }

            ShutterButton(onClick = onCapture)

            IconButton(onClick = onOpenGallery) {
                Icon(Icons.Default.PhotoLibrary, "相册", tint = Color.White)
            }
        }
    }

    // 设置底部弹出面板
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = Color.Black.copy(alpha = 0.9f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("设置", style = MaterialTheme.typography.titleMedium, color = Color.White)

                // 透明度滑块
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("透明度", color = Color.White, modifier = Modifier.weight(1f))
                    Slider(
                        value = lineOpacity,
                        onValueChange = onOpacityChange,
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.weight(2f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White
                        )
                    )
                    Text(
                        "${(lineOpacity * 100).toInt()}%",
                        color = Color.White,
                        modifier = Modifier.width(50.dp)
                    )
                }

                // 颜色选择
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("颜色", color = Color.White, modifier = Modifier.weight(1f))
                    listOf(
                        Color.Yellow, Color.Red, Color.Blue, Color.White,
                        Color.Green, Color(0xFFFFA500), Color(0xFF800080)
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(color, CircleShape)
                                .clickable { onColorChange(color) }
                                .then(
                                    if (lineColor == color) {
                                        Modifier.border(2.dp, Color.White, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
