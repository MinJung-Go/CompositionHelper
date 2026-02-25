package com.example.compositionhelper.camera

import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.compositionhelper.model.*
import com.example.compositionhelper.overlay.CameraCompositionOverlay
import com.example.compositionhelper.ui.components.CameraBottomBar
import com.example.compositionhelper.ui.components.RecommendationChip
import kotlinx.coroutines.launch

@Composable
fun CameraCompositionScreen(
    onNavigateBack: () -> Unit,
    onOpenGallery: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // 构图状态
    var compositionType by remember { mutableStateOf(CompositionType.RULE_OF_THIRDS) }
    var selectedCategory by remember { mutableStateOf(CompositionCategory.CLASSIC) }
    var lineOpacity by remember { mutableStateOf(0.7f) }
    var lineColor by remember { mutableStateOf(Color.Yellow) }

    // 模式与 UI 控制
    var isSmartMode by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    // 分析结果状态
    var detectedSubjects by remember { mutableStateOf(emptyList<DetectedSubject>()) }
    var recommendedType by remember { mutableStateOf<CompositionType?>(null) }
    var matchScore by remember { mutableStateOf(0f) }
    var guidanceHint by remember { mutableStateOf<String?>(null) }

    // 相机管理器
    val cameraManager = remember { CameraManager(context, lifecycleOwner) }
    var cameraInitialized by remember { mutableStateOf(false) }

    // 帧分析器
    val frameAnalyzer = remember {
        FrameAnalyzer { result ->
            detectedSubjects = result.detectedSubjects
            recommendedType = result.recommendedType
            matchScore = result.confidence
            guidanceHint = result.guidanceHint
        }
    }

    // PreviewView 引用
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // 初始化相机
    LaunchedEffect(Unit) {
        cameraManager.initialize()
        cameraInitialized = true
    }

    // 绑定相机（响应 Smart 模式切换）
    LaunchedEffect(cameraInitialized, isSmartMode, previewView) {
        if (cameraInitialized && previewView != null) {
            cameraManager.bindPreview(
                previewView = previewView!!,
                enableAnalysis = isSmartMode,
                analyzer = if (isSmartMode) frameAnalyzer else null
            )
        }
    }

    // 清理
    DisposableEffect(Unit) {
        onDispose {
            frameAnalyzer.close()
            cameraManager.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Layer 0: 相机预览
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { previewView = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Layer 1 + 3: 构图叠加层（引导线 + 主体追踪）
        CameraCompositionOverlay(
            compositionType = compositionType,
            lineOpacity = lineOpacity,
            lineColor = lineColor,
            detectedSubjects = if (isSmartMode) detectedSubjects else emptyList(),
            modifier = Modifier.fillMaxSize()
        )

        // 点击切换控制面板
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls }
                    )
                }
        )

        // UI 控制层
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 顶部栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Manual / Smart 切换
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSmartMode) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                        onClick = {
                            isSmartMode = !isSmartMode
                            if (!isSmartMode) {
                                detectedSubjects = emptyList()
                                recommendedType = null
                                guidanceHint = null
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (isSmartMode) Icons.Default.AutoAwesome else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                if (isSmartMode) "智能" else "手动",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                // AI 推荐浮层（Smart 模式）
                if (isSmartMode && recommendedType != null) {
                    RecommendationChip(
                        compositionType = recommendedType!!,
                        score = matchScore,
                        guidanceHint = guidanceHint,
                        onAccept = { compositionType = recommendedType!! },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 220.dp)
                    )
                }

                // 底部控制栏
                CameraBottomBar(
                    selectedComposition = compositionType,
                    selectedCategory = selectedCategory,
                    lineOpacity = lineOpacity,
                    lineColor = lineColor,
                    onCompositionChange = { compositionType = it },
                    onCategoryChange = { selectedCategory = it },
                    onOpacityChange = { lineOpacity = it },
                    onColorChange = { lineColor = it },
                    onCapture = {
                        cameraManager.capturePhoto(
                            onSaved = { uri ->
                                Toast.makeText(context, "照片已保存", Toast.LENGTH_SHORT).show()
                            },
                            onError = { e ->
                                Toast.makeText(context, "拍照失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onOpenGallery = onOpenGallery,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
