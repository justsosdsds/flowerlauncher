package com.flower.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flower.launcher.apps.InstalledApp
import com.flower.launcher.home.HomePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopSettingsSheet(
    preferences: HomePreferences,
    onDismiss: () -> Unit,
    onToggleLock: () -> Unit,
    onWallpaper: () -> Unit,
    onAppearance: () -> Unit,
    onHomeSettings: () -> Unit,
    onSwipeUpToSettings: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .swipeUpToSettings(onSwipeUpToSettings)
                .padding(horizontal = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "主页",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Surface(shape = CircleShape, tonalElevation = 2.dp) {
                    IconButton(onClick = onToggleLock) {
                        Icon(
                            imageVector = if (preferences.layoutLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            contentDescription = if (preferences.layoutLocked) "解锁布局" else "锁定布局",
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            SettingsRow(Icons.Outlined.Palette, "全局外观", onAppearance)
            SettingsRow(Icons.Outlined.Home, "主页设置", onHomeSettings)
            SettingsRow(Icons.Outlined.Wallpaper, "壁纸", onWallpaper)
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun Modifier.swipeUpToSettings(onSwipeUp: () -> Unit): Modifier = pointerInput(onSwipeUp) {
    val thresholdPx = 96.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var totalY = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            totalY += change.positionChange().y
        }
        if (totalY < -thresholdPx) onSwipeUp()
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(width = 78.dp, height = 88.dp)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(30.dp),
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 22.dp),
        )
    }
}

@Composable
fun AddAppDialog(
    apps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onSelected: (InstalledApp) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加应用") },
        text = {
            LazyColumn(modifier = Modifier.height(420.dp)) {
                items(apps, key = { it.componentKey }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(app) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        app.icon?.let {
                            Image(bitmap = it, contentDescription = null, modifier = Modifier.size(42.dp))
                        }
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun AddFolderDialog(
    apps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onCreate: (String, List<String>) -> Unit,
) {
    var name by remember { mutableStateOf("文件夹") }
    val selected = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加文件夹") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(330.dp)) {
                    items(apps, key = { it.componentKey }) { app ->
                        val checked = app.componentKey in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selected.remove(app.componentKey) else selected.add(app.componentKey)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { value ->
                                    if (value) {
                                        if (app.componentKey !in selected) selected.add(app.componentKey)
                                    } else {
                                        selected.remove(app.componentKey)
                                    }
                                },
                            )
                            Text(app.label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && selected.isNotEmpty(),
                onClick = { onCreate(name.trim(), selected.toList()) },
            ) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun AppearanceDialog(
    preferences: HomePreferences,
    onDismiss: () -> Unit,
    onUpdate: (HomePreferences) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("全局外观") },
        text = {
            Column {
                ToggleRow("显示时钟", preferences.showClock) { onUpdate(preferences.copy(showClock = it)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
fun HomeSettingsDialog(
    preferences: HomePreferences,
    onDismiss: () -> Unit,
    onUpdate: (HomePreferences) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主页设置") },
        text = {
            Column {
                Text("图标列数", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    (3..5).forEach { columns ->
                        if (preferences.gridColumns == columns) {
                            Button(onClick = { onUpdate(preferences.copy(gridColumns = columns)) }) { Text("$columns") }
                        } else {
                            TextButton(onClick = { onUpdate(preferences.copy(gridColumns = columns)) }) { Text("$columns") }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                ToggleRow("锁定桌面布局", preferences.layoutLocked) { onUpdate(preferences.copy(layoutLocked = it)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
