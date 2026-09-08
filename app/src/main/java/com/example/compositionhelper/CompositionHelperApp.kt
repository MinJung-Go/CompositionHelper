package com.example.compositionhelper

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.compositionhelper.color.ColorPhotoStore
import com.example.compositionhelper.model.*
import com.example.compositionhelper.ui.composition.ImageWithCompositionOverlay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompositionHelperApp(hasPermissions: Boolean, onRequestPermissions: () -> Unit,
    onOpenCamera: () -> Unit = {}, onColorPhoto: (Uri) -> Unit = {}, onOpenSamples: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableStateOf(CompositionType.RULE_OF_THIRDS) }
    var category by remember { mutableStateOf(CompositionCategory.CLASSIC) }
    var opacity by remember { mutableStateOf(.7f) }
    var lineColor by remember { mutableStateOf(Color.White) }
    var controls by remember { mutableStateOf(false) }
    var analyzing by remember { mutableStateOf(false) }
    var analysisJob by remember { mutableStateOf<Job?>(null) }
    var recommendations by remember { mutableStateOf<List<CompositionType>>(emptyList()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            analysisJob?.cancel(); photoUri = uri.toString(); recommendations = emptyList()
        }
    }
    LaunchedEffect(photoUri) {
        photo = null; error = null
        val uri = photoUri ?: return@LaunchedEffect
        loading = true
        try { photo = ColorPhotoStore.load(context, Uri.parse(uri), true) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = "照片暂时无法打开，请重新选择。" }
        catch (e: OutOfMemoryError) { error = "照片过大，暂时无法预览。" }
        finally { loading = false }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Column {
            Text("构图工作室", style = MaterialTheme.typography.titleLarge)
            Text("COMPOSITION STUDIO", fontSize = 9.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }, actions = { IconButton(onClick = onOpenCamera) { Icon(Icons.Default.CameraAlt, "打开相机") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (photo == null) {
                Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp))) {
                    AsyncImage(R.drawable.sample_lake, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha=.88f)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("把眼前的风景\n变成你的作品", fontSize = 28.sp, lineHeight = 36.sp, color = Color.White)
                        Text("从一张照片开始，探索构图与色彩。", color = Color.White.copy(alpha=.7f), fontSize = 12.sp)
                        Button(onClick = { picker.launch(arrayOf("image/*")) }) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("选择照片")
                        }
                    }
                    if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("灵感，从这里开始", style = MaterialTheme.typography.titleMedium)
                Card(onClick = onOpenSamples, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AsyncImage(R.drawable.sample_coffee, null, Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("参考照片", style = MaterialTheme.typography.titleMedium)
                            Text("4 个场景 · 离线探索与调色", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Text("↗", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp)
                    }
                }
                Text("让主体更突出，让色彩更自然。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(Color.Black)) {
                    ImageWithCompositionOverlay(photo!!, type, opacity, lineColor, Modifier.fillMaxSize())
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { photoUri?.let { onColorPhoto(Uri.parse(it)) } }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("智能调色")
                    }
                    OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }) { Text("换一张") }
                }
                Text("构图方式", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CompositionCategory.values().toList()) { c ->
                        FilterChip(category == c, onClick = { category = c }, label = { Text(c.displayName) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CompositionType.values().filter { it.getCategory() == category }) { t ->
                        FilterChip(type == t, onClick = { type = t }, label = { Text(t.displayName) })
                    }
                }
                OutlinedButton(onClick = {
                    val bitmap = photo ?: return@OutlinedButton
                    analyzing = true; error = null
                    analysisJob = scope.launch {
                        try {
                            val result = ImageAnalyzer.analyze(bitmap)
                            recommendations = result.recommendedCompositions
                            recommendations.firstOrNull()?.let { type = it }
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { error = "构图分析失败，请重试。" }
                        finally { analyzing = false }
                    }
                }, enabled = !analyzing, modifier = Modifier.fillMaxWidth()) { Text(if (analyzing) "正在寻找合适的构图…" else "分析这张照片的构图") }
                if (recommendations.isNotEmpty()) Text("推荐 · ${recommendations.take(3).joinToString(" / ") { it.displayName }}",
                    color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = { controls = !controls }) { Text(if (controls) "收起辅助线设置 −" else "辅助线设置 ＋") }
                if (controls) {
                    Text("辅助线强度  ${(opacity*100).toInt()}%", fontSize = 12.sp)
                    Slider(opacity, { opacity = it }, valueRange = .1f..1f)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(Color.White, Color(0xFFE3CAA1), Color.Cyan, Color.Red).forEach { c ->
                            Surface(onClick = { lineColor = c }, color = c, shape = RoundedCornerShape(24.dp), modifier = Modifier.size(44.dp)) {
                                if (lineColor == c) Box(contentAlignment = Alignment.Center) { Text("✓", color = Color.Black) }
                            }
                        }
                    }
                }
                TextButton(onClick = onOpenSamples) { Text("浏览参考照片 ↗") }
            }
        }
    }
}
