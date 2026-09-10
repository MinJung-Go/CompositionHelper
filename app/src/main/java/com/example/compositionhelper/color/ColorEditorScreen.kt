package com.example.compositionhelper.color

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalDensity
import com.example.compositionhelper.samples.PhotoSamplesScreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ColorEditorScreen(uri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
    var currentUri by rememberSaveable(uri.toString()) { mutableStateOf(uri.toString()) }
    var samples by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        if (selected != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            currentUri = selected.toString()
        }
    }
    if (samples) {
        Dialog(onDismissRequest = { samples = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            PhotoSamplesScreen(onBack = { samples = false }, onColorPhoto = { selected ->
                currentUri = selected.toString(); samples = false
            })
        }
    }
    key(currentUri) {
        ColorEditorContent(Uri.parse(currentUri), onBack,
            onChoosePhoto = { picker.launch(arrayOf("image/*")) }, onOpenSamples = { samples = true })
    }
}

private data class EditSnapshot(val recipe: String, val strength: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColorEditorContent(uri: Uri, onBack: () -> Unit,
    analyzePhoto: suspend (Bitmap, String, String) -> ColorPlan = GeminiColorClient::analyze,
    onChoosePhoto: () -> Unit = onBack, onOpenSamples: () -> Unit = onBack
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var offline by remember { mutableStateOf<ColorSuggestion?>(null) }
    var recipe by rememberSaveable { mutableStateOf(ColorPlanCodec.encode(ColorPlan())) }
    val plan = remember(recipe) { ColorPlanCodec.decode(recipe) }
    var lastAi by rememberSaveable { mutableStateOf<String?>(null) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var strength by rememberSaveable { mutableStateOf(1f) }
    var comparison by rememberSaveable { mutableStateOf("左右对比") }
    var loading by remember { mutableStateOf(true) }
    var renderedRecipe by remember { mutableStateOf<String?>(null) }
    var renderedStrength by remember { mutableStateOf(-1f) }
    var rendering by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var analyzing by remember { mutableStateOf(false) }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    var aiJob by remember { mutableStateOf<Job?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var renderError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loadRetry by remember { mutableStateOf(0) }
    var renderRetry by remember { mutableStateOf(0) }
    var settings by remember { mutableStateOf(false) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    var difference by remember { mutableStateOf("") }
    var divider by rememberSaveable { mutableStateOf(.5f) }
    val undo = remember { mutableStateListOf<EditSnapshot>() }

    fun checkpoint() {
        val snapshot = EditSnapshot(recipe, strength)
        if (undo.lastOrNull() != snapshot) undo.add(snapshot)
        if (undo.size > 20) undo.removeAt(0)
    }
    fun applyPlan(value: ColorPlan) {
        checkpoint(); recipe = ColorPlanCodec.encode(value); strength = 1f; notice = null
    }
    val enabled = source != null && !loading && !saving && !analyzing
    val previewReady = renderedRecipe == recipe && renderedStrength == strength && preview != null

    LaunchedEffect(uri, loadRetry) {
        loading = true; loadError = null
        try {
            val loaded = ColorPhotoStore.load(context, uri, preview = true)
            val suggestion = ColorPhotoStore.suggest(loaded)
            source = loaded; offline = suggestion
            if (!initialized) {
                recipe = ColorPlanCodec.encode(ColorPlan())
                initialized = true
            }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { loadError = when (e) {
            is SecurityException -> "照片访问权限已失效，请返回重新选择。"
            is java.io.FileNotFoundException -> "找不到这张照片，请返回重新选择。"
            else -> "照片暂时无法打开，请重试或换一张。"
        }
        } catch (e: OutOfMemoryError) { loadError = "图片过大，内存不足，请选择较小图片"
        } finally { loading = false }
    }
    LaunchedEffect(source, recipe, strength, renderRetry) {
        val original = source ?: return@LaunchedEffect
        val requestedRecipe = recipe
        val requestedStrength = strength
        val requestedPlan = ColorPlanCodec.decode(requestedRecipe)
        rendering = true; renderError = null
        try {
            delay(100)
            val result = ColorPhotoStore.renderPreview(original, requestedPlan, requestedStrength)
            preview = result.bitmap
            difference = String.format(java.util.Locale.ROOT, "预览变化像素 %.1f%% · 平均通道差 %.2f / 255", result.changedPercent, result.meanDifference)
            notice = if (requestedPlan == ColorPlan()) "照片已就绪" else if (result.changedPercent == 0f)
                "渲染完成，当前输出与原图相同" else "调色已渲染完成"
            renderedRecipe = requestedRecipe; renderedStrength = requestedStrength
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { renderError = "预览失败，请重试"
        } catch (e: OutOfMemoryError) { renderError = "预览内存不足，请重试"
        } finally { rendering = false }
    }
    fun analyze() {
        val original = source ?: return
        if (!enabled) return
        if (GeminiColorSession.apiKey.isBlank()) { settings = true; return }
        analyzing = true; actionError = null; notice = null
        val apiKey = GeminiColorSession.apiKey
        val model = GeminiColorSession.model
        aiJob = scope.launch {
            try {
                val result = analyzePhoto(original, apiKey, model)
                applyPlan(result)
                lastAi = recipe
                notice = "AI 方案已收到，正在渲染…"
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { actionError = e.message ?: "AI 分析失败，请重试"
            } catch (e: OutOfMemoryError) { actionError = "图片分析内存不足，请重试"
            } finally { analyzing = false }
        }
    }
    fun saveCopy() {
        if (!enabled || !previewReady) return
        val exportPlan = plan; val exportStrength = strength
        saving = true; actionError = null; notice = null
        saveJob = scope.launch {
            try {
                ColorPhotoStore.save(context, uri, exportPlan, exportStrength)
                notice = "调色副本已保存到相册，原片保留"
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { actionError = "保存失败，请检查相册权限和剩余空间后重试"
            } catch (e: OutOfMemoryError) { actionError = "原图导出内存不足，请关闭其他应用后重试"
            } finally { saving = false }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) saveCopy() else actionError = "未获得保存权限，可在系统设置中授予后重试"
    }

    if (settings) AiColorSettings(onDismiss = { settings = false })
    val busy = loading || analyzing || saving
    val status = when {
        loading -> "正在打开照片…"
        analyzing -> "AI 正在分析光线与色彩…"
        saving -> "正在导出原尺寸副本…"
        loadError != null -> "照片未能打开"
        renderError != null || actionError != null -> "操作失败，可重试"
        source != null && (rendering || !previewReady) -> "正在渲染调色…"
        else -> notice ?: "照片已就绪"
    }
    val saveAction: () -> Unit = {
        if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else saveCopy()
    }
    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("色彩工作室", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, "关闭色彩工作室") } },
            actions = { IconButton(onClick = { settings = true }) { Icon(Icons.Default.Tune, "AI 设置") } },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }, bottomBar = {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.navigationBarsPadding().fillMaxWidth()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.widthIn(max = 720.dp).align(Alignment.CenterHorizontally).padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    (actionError ?: renderError)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (busy || rendering) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(status, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (analyzing || saving) {
                            TextButton(onClick = {
                                if (analyzing) aiJob?.cancel() else saveJob?.cancel()
                                notice = "操作已取消"
                            }) { Text(if (analyzing) "取消分析" else "取消保存") }
                        } else if (renderError != null) {
                            TextButton(onClick = { renderRetry++ }) { Text("重试预览") }
                        }
                    }
                    if (source != null) {
                        StudioActions(enabled, enabled && previewReady && !rendering, { analyze() }, saveAction)
                    }
                }
            }
        }
    }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val viewportHeight = maxHeight
            val largeType = LocalDensity.current.fontScale > 1.4f
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                val original = source
                if (original == null) {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (loading) Text("正在打开照片…", style = MaterialTheme.typography.titleMedium)
                            else {
                                Text("照片未能打开", style = MaterialTheme.typography.titleLarge)
                                Text(loadError ?: "请重新选择照片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedButton(onClick = { loadRetry++ }) { Text("重新读取") }
                                Button(onClick = onChoosePhoto) { Text("重新选图") }
                            }
                        }
                    }
                } else {
                    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (!largeType) Text("你的影像", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            if (largeType) IconButton(onClick = onChoosePhoto, enabled = !busy) { Icon(Icons.Default.PhotoLibrary, "更换照片") }
                            else TextButton(onClick = onChoosePhoto, enabled = !busy) {
                                Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("换照片")
                            }
                            IconButton(onClick = onOpenSamples, enabled = !busy) { Icon(Icons.Default.GridView, "选择内置样片") }
                        }
                        StudioPhotoPreview(original, preview ?: original, comparison, divider,
                            maxHeight = (viewportHeight * .62f).coerceIn(if (largeType) 140.dp else 220.dp, 520.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(4.dp)) {
                            listOf("左右对比", "调色后", "原图").forEach { mode ->
                                TextButton(onClick = { comparison = mode }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(9.dp),
                                    colors = ButtonDefaults.textButtonColors(containerColor = if (comparison == mode) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)) {
                                    Text(if (mode == "左右对比") "对比" else mode, fontSize = 12.sp)
                                }
                            }
                        }
                        if (comparison == "左右对比") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(divider, { divider = it }, valueRange = 0f..1f,
                                    modifier = Modifier.padding(start = 12.dp).semantics { contentDescription = "前后对比分界" })
                            }
                        }
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("调色方案", style = MaterialTheme.typography.titleMedium)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(onClick = {
                                        val snapshot = undo.removeAt(undo.lastIndex)
                                        recipe = snapshot.recipe; strength = snapshot.strength; notice = null
                                    }, enabled = enabled && undo.isNotEmpty()) { Text("撤销") }
                                    TextButton(onClick = { applyPlan(ColorPlan()) }, enabled = enabled) { Text("重置") }
                                }
                                if (plan.scene.isNotBlank()) Text(plan.scene, style = MaterialTheme.typography.titleSmall)
                                if (plan.explanation.isNotBlank()) Text(plan.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                AdjustmentSlider("效果强度", strength, 0f..1f, enabled, ::checkpoint) { strength = it }
                                TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "手动精调 −" else "手动精调") }
                                if (advanced) {
                                    val a = plan.basic
                                    fun adjust(value: ColorAdjustment) { recipe = ColorPlanCodec.encode(plan.copy(basic = value)); notice = null }
                                    AdjustmentSlider("曝光", a.exposure, -1f..1f, enabled, ::checkpoint) { adjust(a.copy(exposure=it)) }
                                    AdjustmentSlider("对比度", a.contrast, -.5f.. .5f, enabled, ::checkpoint) { adjust(a.copy(contrast=it)) }
                                    AdjustmentSlider("阴影", a.shadows, -.4f.. .4f, enabled, ::checkpoint) { adjust(a.copy(shadows=it)) }
                                    AdjustmentSlider("高光", a.highlights, -.4f.. .4f, enabled, ::checkpoint) { adjust(a.copy(highlights=it)) }
                                    AdjustmentSlider("色温", a.temperature, -.2f.. .2f, enabled, ::checkpoint) { adjust(a.copy(temperature=it)) }
                                    AdjustmentSlider("色调", a.tint, -.2f.. .2f, enabled, ::checkpoint) { adjust(a.copy(tint=it)) }
                                    AdjustmentSlider("饱和度", a.saturation, -.5f.. .5f, enabled, ::checkpoint) { adjust(a.copy(saturation=it)) }
                                    RgbCorrectionControls(plan.rgb, enabled, ::checkpoint) {
                                        recipe = ColorPlanCodec.encode(plan.copy(rgb = it)); notice = null
                                    }
                                    TextButton(onClick = { offline?.let { applyPlan(ColorPlan(basic = it.adjustment, explanation = it.explanation, scene = "离线基础调整")) } }, enabled = enabled) { Text("离线基础调整") }
                                }
                                if (lastAi != null) TextButton(onClick = { lastAi?.let { applyPlan(ColorPlanCodec.decode(it)) } }, enabled = enabled) { Text("恢复 AI 方案") }
                                TextButton(onClick = { details = !details }) { Text(if (details) "调整详情 −" else "调整详情") }
                                if (details) {
                                    Text(difference, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    androidx.compose.foundation.text.selection.SelectionContainer {
                                        Text(recipe, style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun RgbCorrectionControls(value: RgbCorrection, enabled: Boolean,
    onStart: () -> Unit, onChange: (RgbCorrection) -> Unit) {
    var range by rememberSaveable { mutableStateOf("整体") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("按画面偏色调整，不以三种颜色数量相等为目标。", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("direct" to "直接调原色", "complement" to "反向调互补色").forEach { (mode, label) ->
                FilterChip(value.mode == mode, onClick = { onStart(); onChange(value.copy(mode = mode)) },
                    label = { Text(label) }, enabled = enabled)
            }
        }
        Text(if (value.mode == "direct") "例如向红色调整：提高 R，画面可能变亮。" else
            "例如向红色调整：降低 G 和 B，画面可能变暗。", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("整体", "阴影", "中间调", "高光").forEach { label ->
                FilterChip(range == label, onClick = { range = label }, label = { Text(label) }, enabled = enabled)
            }
        }
        val channels = when (range) {
            "阴影" -> value.shadows; "中间调" -> value.midtones; "高光" -> value.highlights; else -> value.global
        }
        key(range, value.mode) {
            listOf("青 ← R → 红", "品红 ← G → 绿", "黄 ← B → 蓝").forEachIndexed { index, label ->
                AdjustmentSlider(label, channels[index], -.08f.. .08f, enabled, onStart) { amount ->
                    val updated = channels.toMutableList().also { it[index] = amount }
                    onChange(when (range) {
                        "阴影" -> value.copy(shadows = updated)
                        "中间调" -> value.copy(midtones = updated)
                        "高光" -> value.copy(highlights = updated)
                        else -> value.copy(global = updated)
                    })
                }
            }
        }
        TextButton(onClick = { onStart(); onChange(RgbCorrection()) }, enabled = enabled) { Text("重置 RGB 校色") }
    }
}

@Composable
private fun AdjustmentSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>,
    enabled: Boolean, onStart: () -> Unit, onChange: (Float) -> Unit) {
    var changing by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (label == "效果强度") "${kotlin.math.round(value * 100).toInt()}%" else String.format(java.util.Locale.ROOT, "%+.4f", value), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = {
            if (!changing) { onStart(); changing = true }
            onChange(it)
        }, onValueChangeFinished = { changing = false }, valueRange = range, enabled = enabled, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun AiColorSettings(onDismiss: () -> Unit) {
    var apiKey by remember { mutableStateOf(GeminiColorSession.apiKey) }
    var model by remember { mutableStateOf(GeminiColorSession.model) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Gemini 调色设置") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("填写你自己的 API Key。密钥仅保留在本次应用运行中，关闭进程后需要重新填写。")
            OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true)
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("模型名称") }, singleLine = true)
            Text("保存设置后点击 AI 调色，才会发送当前照片的缩略图。", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = {
        TextButton(onClick = {
            GeminiColorSession.apiKey = apiKey.trim(); GeminiColorSession.model = model.trim(); onDismiss()
        }, enabled = apiKey.trim().isNotEmpty() && apiKey.trim().all { it.code in 33..126 } &&
            GeminiColorProtocol.validModel(model.trim())) { Text("保存") }
    }, dismissButton = {
        Row {
            TextButton(onClick = { GeminiColorSession.apiKey = ""; apiKey = "" }) { Text("清除密钥") }
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    })
}
