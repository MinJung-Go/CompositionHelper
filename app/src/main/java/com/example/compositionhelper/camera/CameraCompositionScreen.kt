package com.example.compositionhelper.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.compositionhelper.checklist.ShootingChecklistScreen
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.viewinterop.AndroidView
import com.example.compositionhelper.model.*
import com.example.compositionhelper.overlay.CameraCompositionOverlay
import com.example.compositionhelper.ui.components.CameraBottomBar
import com.example.compositionhelper.ui.components.RecommendationChip

@Composable
fun CameraCompositionScreen(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenGallery: () -> Unit,
    onPhotoSaved: (Uri) -> Unit = {},
    onOpenSamples: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 构图状态
    var compositionType by remember { mutableStateOf(CompositionType.RULE_OF_THIRDS) }
    var selectedCategory by remember { mutableStateOf(CompositionCategory.CLASSIC) }
    var lineOpacity by remember { mutableStateOf(0.7f) }
    var lineColor by remember { mutableStateOf(Color.Yellow) }
    var spiralOrientation by remember { mutableStateOf(0) } // 0=↘ 1=↙ 2=↗ 3=↖

    // 模式与 UI 控制
    var isSmartMode by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showChecklist by remember { mutableStateOf(false) }

    if (showChecklist) {
        Dialog(onDismissRequest = { showChecklist = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            ShootingChecklistScreen(onClose = { showChecklist = false })
        }
    }

    // 分析结果状态
    var detectedSubjects by remember { mutableStateOf(emptyList<DetectedSubject>()) }
    var recommendedType by remember { mutableStateOf<CompositionType?>(null) }
    var matchScore by remember { mutableStateOf(0f) }
    var guidanceHint by remember { mutableStateOf<String?>(null) }

    // 相机管理器
    val cameraManager = remember { CameraManager(context, lifecycleOwner) }
    var cameraInitialized by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraRetry by remember { mutableStateOf(0) }

    var capturing by remember { mutableStateOf(false) }
    fun capture() {
        if (capturing || !cameraReady) return
        capturing = true
        cameraManager.capturePhoto(
            onSaved = { uri ->
                capturing = false
                Toast.makeText(context, "原片已保存", Toast.LENGTH_SHORT).show()
                onPhotoSaved(uri)
            },
            onError = { e ->
                capturing = false
                Toast.makeText(context, "拍照失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
    val savePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) capture()
        else Toast.makeText(context, "需要存储权限才能保存照片", Toast.LENGTH_SHORT).show()
    }

    // 帧分析器
    val frameAnalyzer = remember {
        FrameAnalyzer { result ->
            detectedSubjects = result.detectedSubjects
            recommendedType = result.recommendedType
            matchScore = result.confidence
            guidanceHint = result.guidanceHint
        }
    }

    LaunchedEffect(isSmartMode) { frameAnalyzer.setEnabled(isSmartMode) }

    // PreviewView 引用
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var captureAreaSize by remember { mutableStateOf(IntSize.Zero) }
    var topControlsSize by remember { mutableStateOf(IntSize.Zero) }
    var bottomControlsSize by remember { mutableStateOf(IntSize.Zero) }

    // 初始化相机
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            onRequestCameraPermission()
        }
    }

    // 初始化相机
    LaunchedEffect(hasCameraPermission, cameraRetry) {
        cameraReady = false; cameraError = null
        if (hasCameraPermission) {
            try {
                cameraManager.initialize()
                cameraInitialized = true
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { cameraInitialized = false; cameraError = "相机初始化失败，请重试" }
        } else {
            cameraInitialized = false
        }
    }


    // 清理
    DisposableEffect(Unit) {
        onDispose {
            frameAnalyzer.close()
            cameraManager.shutdown()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val density = LocalDensity.current
        val statusHeight = WindowInsets.statusBars.getTop(density)
        val captureTopReserve = with(density) {
            if (topControlsSize.height > 0) topControlsSize.height.toDp() else statusHeight.toDp() + 56.dp
        }
        val captureBottomReserve = with(density) {
            if (bottomControlsSize.height > 0) bottomControlsSize.height.toDp() else 240.dp
        }
        val targetCaptureAspectRatio = compositionType.captureAspectRatio(
            isPortrait = maxHeight >= maxWidth
        )
        val fitted = CameraGeometry.fit(maxWidth.value, maxHeight.value, captureTopReserve.value,
            captureBottomReserve.value, targetCaptureAspectRatio)
        val captureWidth = fitted.width.dp
        val captureHeight = fitted.height.dp
        val captureOffsetY = fitted.top.dp

        LaunchedEffect(
            cameraInitialized,
            isSmartMode,
            previewView,
            hasCameraPermission,
            captureAreaSize,
            targetCaptureAspectRatio, cameraRetry
        ) {
            val measuredPreviewView = previewView
            if (
                hasCameraPermission &&
                cameraInitialized &&
                measuredPreviewView != null &&
                captureAreaSize.width > 0 &&
                captureAreaSize.height > 0
            ) {
                cameraReady = cameraManager.bindPreview(
                    previewView = measuredPreviewView,
                    captureWidth = captureAreaSize.width,
                    captureHeight = captureAreaSize.height,
                    enableAnalysis = isSmartMode,
                    analyzer = if (isSmartMode) frameAnalyzer else null
                )
                cameraError = if (cameraReady) null else "相机无法启动，请检查设备相机是否可用"
            }
        }

        Box(
            modifier = Modifier
                .width(captureWidth)
                .height(captureHeight)
                .offset(y = captureOffsetY)
                .align(Alignment.TopCenter)
                .onSizeChanged { captureAreaSize = it }
                .clipToBounds()
        ) {
            // Layer 0: 相机预览
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
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
                spiralOrientation = spiralOrientation,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 点击切换控制面板
        Box(
            modifier = Modifier
                .width(captureWidth)
                .height(captureHeight)
                .offset(y = captureOffsetY)
                .align(Alignment.TopCenter)
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
                        .onSizeChanged { topControlsSize = it }
                        .background(Color.Black)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = { showChecklist = true }) {
                        Icon(Icons.Default.Checklist, "拍摄检查清单", tint = Color.White)
                    }

                    // Manual / Smart 切换
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSmartMode) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                        onClick = {
                            if (capturing) return@Surface
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

                    // 黄金螺旋方向切换按钮
                    if (compositionType == CompositionType.GOLDEN_SPIRAL) {
                        val orientationLabels = arrayOf("↘", "↙", "↗", "↖")
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.25f),
                            onClick = { spiralOrientation = (spiralOrientation + 1) % 4 }
                        ) {
                            Text(
                                orientationLabels[spiralOrientation],
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                    onCompositionChange = { if (!capturing) compositionType = it },
                    onCategoryChange = { selectedCategory = it },
                    onOpacityChange = { lineOpacity = it },
                    onColorChange = { lineColor = it },
                    onCapture = {
                        if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            savePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else capture()
                    },
                    onOpenGallery = onOpenGallery,
                    captureEnabled = cameraReady && !capturing,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { bottomControlsSize = it }
                        .navigationBarsPadding()
                )
            }
        }
        if (hasCameraPermission && cameraError != null) {
            Column(Modifier.align(Alignment.Center).background(Color.Black).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(cameraError!!, color = Color.White)
                TextButton(onClick = { cameraRetry++ }) { Text("重试相机") }
                TextButton(onClick = onOpenSamples) { Text("使用参考照片") }
                TextButton(onClick = { showChecklist = true }) { Text("拍摄检查清单") }
            }
        }
        if (!hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "需要相机权限",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "请授予 CAMERA 权限后再进入实时取景模式。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Button(onClick = onRequestCameraPermission) {
                        Text("重新请求权限")
                    }
                    TextButton(onClick = onOpenSamples) { Text("先用内置参考照片", color = Color.White) }
                    TextButton(onClick = { showChecklist = true }) { Text("拍摄检查清单", color = Color.White) }
                }
            }
        }
    }
}

private fun CompositionType.captureAspectRatio(isPortrait: Boolean): Float {
    return when (this) {
        CompositionType.GOLDEN_SPIRAL, CompositionType.GOLDEN_TRIANGLE -> {
            if (isPortrait) 8f / 13f else 13f / 8f
        }
        else -> 1f
    }
}
