package com.example.compositionhelper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.compositionhelper.ui.composition.*
import kotlinx.coroutines.launch
import java.io.InputStream

// 构图类型枚举
enum class CompositionType(val displayName: String, val icon: String) {
    RULE_OF_THIRDS("三分法", "grid_3x3"),
    CENTER("中心构图", "center_focus_strong"),
    DIAGONAL("对角线", "line_diagonal"),
    FRAME("框架构图", "crop_square"),
    LEADING_LINES("引导线", "show_chart"),
    S_CURVE("S形曲线", "wave"),
    GOLDEN_SPIRAL("黄金螺旋", "all_inclusive"),
    GOLDEN_TRIANGLE("黄金三角", "change_history"),
    SYMMETRY("对称构图", "sync"),
    NEGATIVE_SPACE("负空间", "crop_square"),
    PATTERN_REPEAT("模式重复", "grid_on"),
    TUNNEL("隧道式", "exit_to_app"),
    SPLIT("分割构图", "view_week"),
    PERSPECTIVE("透视焦点", "visibility"),
    INVISIBLE_LINE("隐形线", "arrow_forward"),
    FILL_FRAME("充满画面", "panorama"),
    LOW_ANGLE("低角度", "arrow_upward"),
    HIGH_ANGLE("高角度", "arrow_downward"),
    DEPTH_LAYER("深度层次", "layers")
}

// 构图分类
enum class CompositionCategory(val displayName: String) {
    CLASSIC("经典"),
    MODERN("现代"),
    PERSPECTIVE("视角")
}

// 获取构图类型属于哪个分类
fun CompositionType.getCategory(): CompositionCategory {
    return when (this) {
        RULE_OF_THIRDS, CENTER, DIAGONAL, FRAME, LEADING_LINES, S_CURVE, GOLDEN_SPIRAL -> CompositionCategory.CLASSIC
        GOLDEN_TRIANGLE, SYMMETRY, NEGATIVE_SPACE, PATTERN_REPEAT, TUNNEL, SPLIT, PERSPECTIVE -> CompositionCategory.MODERN
        INVISIBLE_LINE, FILL_FRAME, LOW_ANGLE, HIGH_ANGLE, DEPTH_LAYER -> CompositionCategory.PERSPECTIVE
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompositionHelperApp(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var selectedComposition by remember { mutableStateOf(CompositionType.RULE_OF_THIRDS) }
    var lineOpacity by remember { mutableStateOf(0.7f) }
    var lineColor by remember { mutableStateOf(Color.Yellow) }
    var autoAnalyzed by remember { mutableStateOf(false) }
    var recommendedCompositions by remember { mutableStateOf(listOf<CompositionType>()) }
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
                                imageVector = Icons.Default.Image,
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
                    CompositionTypeButton(
                        type = composition,
                        isSelected = selectedComposition == composition,
                        isRecommended = recommendedCompositions.contains(composition) && autoAnalyzed,
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
                    listOf(Color.Yellow, Color.Red, Color.Blue, Color.White, Color.Green, Color.Orange, Color.Purple).forEach { color ->
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
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("拍照")
                    }

                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("相册")
                    }
                }

                Button(
                    onClick = {
                        selectedImage?.let { bitmap ->
                            analyzeInProgress = true
                            scope.launch {
                                // 这里调用自动分析
                                recommendedCompositions = analyzeComposition(
                                    context = context,
                                    bitmap = bitmap
                                )
                                autoAnalyzed = true
                                analyzeInProgress = false
                                if (recommendedCompositions.isNotEmpty()) {
                                    selectedComposition = recommendedCompositions.first()
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp, 80.dp)
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
            // 推荐标记
            if (isRecommended) {
                Icon(
                    imageVector = Icons.Default.Star,
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
            }
        }
    }
}

// 自动分析函数（简化版）
suspend fun analyzeComposition(
    context: Context,
    bitmap: Bitmap
): List<CompositionType> {
    // 这里应该使用 ML Kit 进行真实分析
    // 暂时返回推荐结果
    return listOf(
        CompositionType.RULE_OF_THIRDS,
        CompositionType.LEADING_LINES,
        CompositionType.CENTER
    )
}
