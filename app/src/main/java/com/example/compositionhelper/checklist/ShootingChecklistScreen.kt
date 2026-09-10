package com.example.compositionhelper.checklist

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

private data class ShootingCheckItem(val id: String, val group: String, val title: String, val detail: String)
private val checkItems = listOf(
    ShootingCheckItem("prepare_permission", "拍摄前", "相机能够取景", "允许相机权限后，确认预览正常、没有黑屏或错误提示。"),
    ShootingCheckItem("prepare_lens", "拍摄前", "镜头与画面清晰", "擦净镜头，确认主体清晰；遇到模糊时调整距离和光线。"),
    ShootingCheckItem("prepare_scene", "拍摄前", "主体与拍摄意图明确", "确定要突出的人或物，观察背景，避免杂物抢占注意力。"),
    ShootingCheckItem("prepare_composition", "拍摄前", "选择合适的构图", "切换所需构图方式，确认辅助线可见且与预期一致。"),
    ShootingCheckItem("capture_alignment", "取景拍摄", "主体与辅助线关系正确", "移动手机，对照辅助线检查主体位置，注意地平线和垂直线。"),
    ShootingCheckItem("capture_edges", "取景拍摄", "四周边缘没有意外裁切", "检查头顶、手脚和画面边缘，保留需要的主体与留白。"),
    ShootingCheckItem("capture_light", "取景拍摄", "亮部和暗部保留细节", "观察主体与背景，避免重要区域过亮、过暗或出现明显偏色。"),
    ShootingCheckItem("capture_shutter", "取景拍摄", "完成一次真实拍摄", "稳定手机后按快门，确认获得拍摄结果，而非仅停留在预览。"),
    ShootingCheckItem("review_photo", "拍摄后", "成片清晰且构图符合预期", "查看真实成片，确认主体、方向、比例和裁切正确，没有明显抖动。"),
    ShootingCheckItem("review_color", "拍摄后", "调色结果经过前后对比", "若使用调色，对照原图检查肤色、文字和细节；未调色可标记不适用。"),
    ShootingCheckItem("review_save", "拍摄后", "照片确实出现在系统相册", "完成保存后，到系统相册打开照片核对，不能只依据成功提示。"),
    ShootingCheckItem("review_record", "拍摄后", "记录问题并决定是否重拍", "对有问题的项目填写备注；必要时重拍，再重新检查。")
)
private enum class CheckStatus(val label: String) {
    pending("待检查"), confirmed("已确认"), problem("有问题"), skipped("不适用")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShootingChecklistScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("shooting_checklist", android.content.Context.MODE_PRIVATE) }
    var stored by remember { mutableStateOf(prefs.getString("shooting.checklist.v1", "") ?: "") }
    var notes by remember { mutableStateOf(prefs.getString("shooting.checklist.notes.v1", "") ?: "") }
    var reset by remember { mutableStateOf(false) }
    val values = remember(stored) {
        stored.split(';').mapNotNull { entry ->
            val parts = entry.split('=', limit = 2)
            if (parts.size != 2) null else CheckStatus.values().find { it.name == parts[1] }?.let { parts[0] to it }
        }.toMap()
    }
    val confirmed = checkItems.count { values[it.id] == CheckStatus.confirmed }
    val problems = checkItems.count { values[it.id] == CheckStatus.problem }
    val skipped = checkItems.count { values[it.id] == CheckStatus.skipped }
    val gold = Color(0xFFE3CAA1)
    fun update(item: ShootingCheckItem, status: CheckStatus) {
        stored = (values + (item.id to status)).toSortedMap().entries.joinToString(";") { "${it.key}=${it.value.name}" }
        prefs.edit().putString("shooting.checklist.v1", stored).apply()
    }
    Scaffold(
        containerColor = Color(0xFF101113),
        topBar = { TopAppBar(title = { Text("拍摄检查清单") }, actions = { TextButton(onClick = onClose) { Text("完成") } }) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("shootingChecklistList"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("已确认 $confirmed/${checkItems.size} · 有问题 $problems · 不适用 $skipped", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(progress = confirmed.toFloat() / checkItems.size, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), color = gold)
                Text("请实际操作后逐项确认。进度保存在本机，不会自动判定拍摄合格；新一轮拍摄前请重置。", style = MaterialTheme.typography.bodySmall)
            }
            listOf("拍摄前", "取景拍摄", "拍摄后").forEach { group ->
                item(key = group) { Text(group, color = gold, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp)) }
                items(checkItems.filter { it.group == group }, key = { it.id }) { item ->
                    val status = values[item.id] ?: CheckStatus.pending
                    var expanded by remember { mutableStateOf(false) }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(item.detail, style = MaterialTheme.typography.bodyMedium)
                            Box {
                                TextButton(onClick = { expanded = true }, modifier = Modifier.testTag("shootingChecklist." + item.id).semantics { contentDescription = item.title + "，" + status.label + "，更改检查结果" }) {
                                    Icon(when (status) {
                                        CheckStatus.confirmed -> Icons.Default.CheckCircle
                                        CheckStatus.problem -> Icons.Default.ErrorOutline
                                        CheckStatus.skipped -> Icons.Default.RemoveCircleOutline
                                        CheckStatus.pending -> Icons.Default.RadioButtonUnchecked
                                    }, contentDescription = null, tint = if (status == CheckStatus.problem) Color(0xFFFFB86C) else gold)
                                    Spacer(Modifier.width(8.dp)); Text(status.label, color = gold)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    CheckStatus.values().forEach { next ->
                                        DropdownMenuItem(text = { Text(next.label) }, onClick = { update(item, next); expanded = false })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(value = notes, onValueChange = {
                    notes = it; prefs.edit().putString("shooting.checklist.notes.v1", it).apply()
                }, label = { Text("问题与备注") }, placeholder = { Text("记录项目、问题现象和是否需要重拍") }, minLines = 3, modifier = Modifier.fillMaxWidth().testTag("shootingChecklistNotes"))
            }
            item {
                Button(onClick = {
                    val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    val report = (listOf("拍摄检查记录 · Android $version", DateFormat.getDateTimeInstance().format(Date()),
                        "本记录由使用者手动确认，不代表自动测试通过。") + checkItems.map {
                        "[${(values[it.id] ?: CheckStatus.pending).label}] ${it.group} · ${it.title}"
                    } + listOf("备注：", notes.ifBlank { "无" })).joinToString("\n")
                    try {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, report)
                        }, "导出检查记录"))
                    } catch (_: android.content.ActivityNotFoundException) {
                        Toast.makeText(context, "未找到可接收记录的应用", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("导出检查记录") }
                TextButton(onClick = { reset = true }, modifier = Modifier.fillMaxWidth()) { Text("开始新一轮检查") }
            }
        }
    }
    if (reset) {
        AlertDialog(onDismissRequest = { reset = false }, title = { Text("清空本轮检查结果和备注？") },
            text = { Text("如需保留，请先导出检查记录。此操作不会删除照片。") },
            confirmButton = { TextButton(onClick = {
                stored = ""; notes = ""
                prefs.edit().remove("shooting.checklist.v1").remove("shooting.checklist.notes.v1").apply()
                reset = false
            }) { Text("清空并开始新一轮") } },
            dismissButton = { TextButton(onClick = { reset = false }) { Text("取消") } })
    }
}
