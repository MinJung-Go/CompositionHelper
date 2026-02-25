package com.example.compositionhelper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.compositionhelper.model.CompositionCategory
import com.example.compositionhelper.model.CompositionType
import com.example.compositionhelper.model.getCategory
import com.example.compositionhelper.ui.composition.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompositionHelperApp(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenCamera: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var selectedComposition by remember { mutableStateOf(CompositionType.RULE_OF_THIRDS) }
    var lineOpacity by remember { mutableStateOf(0.7f) }
    var lineColor by remember { mutableStateOf(Color.Yellow) }
    var autoAnalyzed by remember { mutableStateOf(false) }
    var recommendedCompositions by remember { mutableStateOf(listOf<CompositionType>()) }
    var confidenceScores by remember { mutableStateOf(mapOf<CompositionType, Double>()) }
    var showAnalysisResults by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(CompositionCategory.CLASSIC) }
    var analyzeInProgress by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 相机启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        selectedImage = bitmap
        autoAnalyzed = false
        recommendedCompositions = emptyList()
    }

    // 相册启动器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                selectedImage = BitmapFactory.decodeStream(stream)
            }
            autoAnalyzed = false
            recommendedCompositions = emptyList()
        }
    }

    // 筛选当前分类的构图类型
    val filteredCompositions = CompositionType.values().filter { it.getCategory() == selectedCategory }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("构图辅助") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onOpenCamera) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "实时取景")
                    }
                    TextButton(onClick = onRequestPermissions) {
                        Text("权限")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 图片显示区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                selectedImage?.let { bitmap ->
                    ImageWithCompositionOverlay(
                        bitmap = bitmap,
                        compositionType = selectedComposition,
                        lineOpacity = lineOpacity,
                        lineColor = lineColor,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: run {
                    // 占位图
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Photo,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "选择照片或拍照",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 构图分类选择器
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompositionCategory.values().forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.displayName) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // 构图类型选择器
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
            ) {
                items(filteredCompositions) { composition ->
                    val confidence = if (recommendedCompositions.contains(composition) && autoAnalyzed) {
                        confidenceScores[composition]
                    } else {
                        null
                    }
                    CompositionTypeButton(
                        type = composition,
                        isSelected = selectedComposition == composition,
                        isRecommended = recommendedCompositions.contains(composition) && autoAnalyzed,
                        confidence = confidence,
                        onClick = { selectedComposition = composition }
                    )
                }
            }

            // 控制面板
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 透明度滑块
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("透明度", modifier = Modifier.weight(1f))
                    Slider(
                        value = lineOpacity,
                        onValueChange = { lineOpacity = it },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.weight(2f)
                    )
                    Text("${(lineOpacity * 100).toInt()}%", modifier = Modifier.width(50.dp))
                }

                // 颜色选择
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("颜色: ", modifier = Modifier.weight(1f))
                    listOf(Color.Yellow, Color.Red, Color.Blue, Color.White, Color.Green, Color(0xFFFFA500), Color(0xFF800080)).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(color, CircleShape)
                                .clickable { lineColor = color }
                                .then(
                                    if (lineColor == color) {
                                        Modifier.border(
                                            2.dp, Color.White, CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Camera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("拍照")
                    }

                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Photo, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("相册")
                    }
                }

                Button(
                    onClick = {
                        selectedImage?.let { bitmap ->
                            analyzeInProgress = true
                            scope.launch {
                                val analysis = ImageAnalyzer.analyze(bitmap)
                                recommendedCompositions = analysis.recommendedCompositions
                                confidenceScores = analysis.confidenceScores
                                autoAnalyzed = true
                                showAnalysisResults = true
                                analyzeInProgress = false
                                if (analysis.recommendedCompositions.isNotEmpty()) {
                                    selectedComposition = analysis.recommendedCompositions.first()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedImage != null && !analyzeInProgress,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (autoAnalyzed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (analyzeInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("分析中...")
                    } else {
                        Text("自动分析构图")
                    }
                }

                // 分析结果显示
                if (showAnalysisResults && autoAnalyzed) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "分析完成",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Green
                                )
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            Divider()

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "推荐构图:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                recommendedCompositions.take(3).forEach { composition ->
                                    val confidence = confidenceScores[composition] ?: 0.0
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            composition.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "${(confidence * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 构图类型按钮
@Composable
fun CompositionTypeButton(
    type: CompositionType,
    isSelected: Boolean,
    isRecommended: Boolean,
    confidence: Double?,
    onClick: () -> Unit
) {
    val buttonHeight = if (isRecommended && confidence != null) 100.dp else 80.dp

    Box(
        modifier = Modifier
            .size(72.dp, buttonHeight)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isRecommended) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    tint = Color.Yellow
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    type.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )

                if (isRecommended && confidence != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${(confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
