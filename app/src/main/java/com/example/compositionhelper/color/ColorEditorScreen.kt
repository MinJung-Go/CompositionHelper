package com.example.compositionhelper.color

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ColorEditorScreen(uri: Uri, onBack: () -> Unit) {
    key(uri.toString()) { ColorEditorContent(uri, onBack) }
}

private data class EditSnapshot(val recipe: String, val strength: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColorEditorContent(uri: Uri, onBack: () -> Unit,
    analyzePhoto: suspend (Bitmap, String, String) -> ColorPlan = GeminiColorClient::analyze
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
    var selectedTool by rememberSaveable { mutableStateOf("曝光") }
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
                recipe = ColorPlanCodec.encode(ColorPlan(basic = suggestion.adjustment,
                    explanation = suggestion.explanation, scene = "离线基础调整"))
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
            preview = ColorPhotoStore.render(original, requestedPlan, requestedStrength)
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
                notice = "AI 方案已应用，可调节强度或继续手动微调"
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
    Scaffold(topBar = {
        TopAppBar(title = { Column {
            Text("色彩工作室", style = MaterialTheme.typography.titleLarge)
            Text("COLOR STUDIO", fontSize = 9.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }, navigationIcon = { TextButton(onClick = onBack) { Text("‹ 返回") } },
            actions = { TextButton(onClick = { settings = true }, enabled = !analyzing) { Text("AI 设置") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }, bottomBar = {
        if (source != null) Surface(color = MaterialTheme.colorScheme.background) {
            Row(Modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { analyze() }, enabled = enabled, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)) {
                    Text(if (lastAi == null) "AI 调色" else "重新 AI 分析")
                }
                OutlinedButton(onClick = {
                    if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else saveCopy()
                }, enabled = enabled && previewReady && !rendering, modifier = Modifier.height(50.dp), shape = RoundedCornerShape(14.dp)) {
                    Text(if (saving) "保存中…" else "保存副本")
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            val original = source
            if (original == null) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                            Text("正在打开照片…", style = MaterialTheme.typography.titleMedium)
                        } else {
                            Text("照片未能打开", style = MaterialTheme.typography.titleLarge)
                            Text(loadError ?: "请重新选择照片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { loadRetry++ }) { Text("重新读取") }
                                Button(onClick = onBack) { Text("重新选图") }
                            }
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(4.dp)) {
                    listOf("左右对比", "调色后", "原图").forEach { mode ->
                        TextButton(onClick = { comparison = mode }, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(9.dp), colors = ButtonDefaults.textButtonColors(
                                containerColor = if (comparison == mode) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                contentColor = if (comparison == mode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)) { Text(mode, fontSize = 12.sp) }
                    }
                }
                Box(Modifier.fillMaxWidth().aspectRatio(original.width.toFloat()/original.height)
                    .clip(RoundedCornerShape(18.dp)).background(Color.Black)) {
                    val edited = preview ?: original
                    Image((if (comparison == "调色后") edited else original).asImageBitmap(),
                        if (comparison == "调色后") "调色后" else "原图", Modifier.fillMaxSize())
                    if (comparison == "左右对比") {
                        Image(edited.asImageBitmap(), "调色后", Modifier.fillMaxSize().drawWithContent {
                            clipRect(left = size.width*divider) { this@drawWithContent.drawContent() }
                        })
                        Box(Modifier.fillMaxSize().drawWithContent {
                            drawContent()
                            drawLine(Color.White.copy(alpha=.8f), androidx.compose.ui.geometry.Offset(size.width*divider, 0f),
                                androidx.compose.ui.geometry.Offset(size.width*divider, size.height), 2.dp.toPx())
                        })
                        Text("原图", Modifier.align(Alignment.TopStart).padding(10.dp).background(Color.Black.copy(alpha=.5f), RoundedCornerShape(6.dp)).padding(7.dp), color = Color.White, fontSize = 10.sp)
                        Text("调色后", Modifier.align(Alignment.TopEnd).padding(10.dp).background(Color.Black.copy(alpha=.5f), RoundedCornerShape(6.dp)).padding(7.dp), color = Color.White, fontSize = 10.sp)
                    }
                }
                if (comparison == "左右对比") {
                    Column {
                        Text("滑动分界，对比前后", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(divider, { divider = it }, valueRange = 0f..1f)
                    }
                }
                if (rendering) LinearProgressIndicator(Modifier.fillMaxWidth())
                renderError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { renderRetry++ }) { Text("重试预览") }
                }
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (lastAi == null) "自然 · 基础调整" else "AI · 色彩方案", color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium)
                        if (plan.scene.isNotBlank()) Text(plan.scene, style = MaterialTheme.typography.titleSmall)
                        if (plan.explanation.isNotBlank()) Text(plan.explanation, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AdjustmentSlider("效果强度", strength, 0f..1f, enabled, ::checkpoint) { strength = it }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        val snapshot = undo.removeAt(undo.lastIndex)
                        recipe = snapshot.recipe; strength = snapshot.strength; notice = null
                    }, enabled = enabled && undo.isNotEmpty()) { Text("撤销") }
                    TextButton(onClick = { applyPlan(ColorPlan()) }, enabled = enabled) { Text("重置") }
                    TextButton(onClick = { lastAi?.let { applyPlan(ColorPlanCodec.decode(it)) } },
                        enabled = enabled && lastAi != null) { Text("恢复 AI 方案") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(onClick = { advanced = !advanced }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("手动精调", color = MaterialTheme.colorScheme.onSurface)
                        Text(if (advanced) "收起 −" else "展开 ＋", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (advanced) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("RGB 校色", "曝光", "对比度", "阴影", "高光", "色温", "色调", "饱和度").forEach { label ->
                            FilterChip(selectedTool == label, onClick = { selectedTool = label }, label = { Text(label) })
                        }
                    }
                    val a = plan.basic
                    fun adjust(value: ColorAdjustment) { recipe = ColorPlanCodec.encode(plan.copy(basic = value)); notice = null }
                    when (selectedTool) {
                        "RGB 校色" -> RgbCorrectionControls(plan.rgb, enabled, ::checkpoint) {
                            recipe = ColorPlanCodec.encode(plan.copy(rgb = it)); notice = null
                        }
                        "曝光" -> AdjustmentSlider("曝光", a.exposure, -1f..1f, enabled, ::checkpoint) { adjust(a.copy(exposure=it)) }
                        "对比度" -> AdjustmentSlider("对比度", a.contrast, -.5f.. .5f, enabled, ::checkpoint) { adjust(a.copy(contrast=it)) }
                        "阴影" -> AdjustmentSlider("阴影", a.shadows, -.4f.. .4f, enabled, ::checkpoint) { adjust(a.copy(shadows=it)) }
                        "高光" -> AdjustmentSlider("高光", a.highlights, -.4f.. .4f, enabled, ::checkpoint) { adjust(a.copy(highlights=it)) }
                        "色温" -> AdjustmentSlider("色温", a.temperature, -.2f.. .2f, enabled, ::checkpoint) { adjust(a.copy(temperature=it)) }
                        "色调" -> AdjustmentSlider("色调", a.tint, -.2f.. .2f, enabled, ::checkpoint) { adjust(a.copy(tint=it)) }
                        else -> AdjustmentSlider("饱和度", a.saturation, -.5f.. .5f, enabled, ::checkpoint) { adjust(a.copy(saturation=it)) }
                    }
                    TextButton(onClick = { offline?.let { applyPlan(ColorPlan(basic=it.adjustment,
                        explanation=it.explanation, scene="离线基础调整")) } }, enabled = enabled) { Text("离线基础调整") }
                }
                if (analyzing || saving) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(if (analyzing) "正在分析照片的光线与色彩…" else "正在保存原尺寸照片…", fontSize = 12.sp)
                    TextButton(onClick = { if (analyzing) aiJob?.cancel() else saveJob?.cancel() }) { Text(if (analyzing) "取消分析" else "取消保存") }
                }
                actionError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
                Text("AI 调色会发送照片缩略图；手动调整在本机完成。", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text("${kotlin.math.round(value * 100).toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = {
            if (!changing) { onStart(); changing = true }
            onChange(it)
        }, onValueChangeFinished = { changing = false }, valueRange = range, enabled = enabled)
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
