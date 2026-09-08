package com.example.compositionhelper.samples

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.compositionhelper.R

/** Packaged JPEG resources: browsing and opening the editor need no network or media permission. */
data class PhotoSample(@DrawableRes val resource: Int, val name: String, val title: String,
    val hint: String, val author: String, val source: String)

object PhotoSamples {
    val all = listOf(
        PhotoSample(R.drawable.sample_lake, "sample_lake", "雪山与湖泊", "观察天空、水面与积雪的冷色和高光。",
            "Kata", "https://www.pexels.com/photo/mountain-landscape-with-lake-14958494/"),
        PhotoSample(R.drawable.sample_forest, "sample_forest", "森林与苔藓", "观察绿色层次，以及提亮暗部后的细节。",
            "Aysegul Aytoren", "https://www.pexels.com/photo/a-forest-with-mossy-rocks-14755971/"),
        PhotoSample(R.drawable.sample_coffee, "sample_coffee", "咖啡静物", "观察暖棕色、杯沿高光和背景虚化。",
            "Negative Space", "https://www.pexels.com/photo/caffeine-coffee-cup-mug-134577/"),
        PhotoSample(R.drawable.sample_city, "sample_city", "城市远景", "观察建筑中性色、远景对比度与天空过渡。",
            "Mo Eid", "https://www.pexels.com/photo/drone-shot-of-city-with-skyscrapers-17910086/")
    )
    fun uri(packageName: String, photo: PhotoSample): Uri =
        Uri.parse("android.resource://$packageName/drawable/${photo.name}")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSamplesScreen(onBack: () -> Unit, onColorPhoto: (Uri) -> Unit) {
    val context = LocalContext.current
    val links = LocalUriHandler.current
    Scaffold(topBar = {
        TopAppBar(title = { Text("参考照片") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("‹ 返回") }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                Text("四种风景，\n四种色彩表达。", fontSize = 28.sp, lineHeight = 38.sp)
                Spacer(Modifier.height(10.dp))
                Text("精选照片已内置，从喜欢的一张开始。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(PhotoSamples.all, key = { it.name }) { photo ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(onClick = { onColorPhoto(PhotoSamples.uri(context.packageName, photo)) }, shape = RoundedCornerShape(20.dp)) {
                        Box(Modifier.fillMaxWidth().height(280.dp)) {
                            AsyncImage(model = photo.resource, contentDescription = photo.title,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha=.8f)))))
                            Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("0${PhotoSamples.all.indexOf(photo)+1}  /  色彩练习", color = Color.White.copy(alpha=.7f), fontSize = 10.sp)
                                Text(photo.title, color = Color.White, fontSize = 23.sp)
                                Text(photo.hint, color = Color.White.copy(alpha=.8f), fontSize = 12.sp)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { runCatching { links.openUri(photo.source) } }) {
                            Text("${photo.author} · Pexels ↗", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onColorPhoto(PhotoSamples.uri(context.packageName, photo)) }) { Text("用这张调色 →", fontSize = 12.sp) }
                    }
                }
            }
            item {
                Text("浏览与手动调色无需联网。AI 分析需要配置密钥。", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { runCatching { links.openUri("https://www.pexels.com/license/") } }) { Text("照片使用许可", fontSize = 11.sp) }
            }
        }
    }
}
