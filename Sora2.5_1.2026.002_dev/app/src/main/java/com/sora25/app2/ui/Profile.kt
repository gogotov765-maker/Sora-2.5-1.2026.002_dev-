package com.sora25.app2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sora25.app2.data.Auth
import com.sora25.app2.data.Repo
import com.sora25.app2.data.VideoItem
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(uid: String, name: String) {
    val items by remember(uid) { Repo.mine(uid) }.collectAsState(initial = emptyList())
    val drafts = items.filter { !it.published }
    val posted = items.filter { it.published }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, fontSize = 22.sp)
                TextButton(onClick = { Auth.signOut() }) { Text("Выйти") }
            }
        }
        item { Text("Черновики (не опубликованы)", color = Color.White.copy(0.7f), fontSize = 14.sp) }
        if (drafts.isEmpty()) item { Text("Нет черновиков", color = Color.White.copy(0.4f), fontSize = 13.sp) }
        items(drafts, key = { it.id }) { VideoCard(it) }
        item { Text("Опубликованные", color = Color.White.copy(0.7f), fontSize = 14.sp) }
        if (posted.isEmpty()) item { Text("Пока ничего", color = Color.White.copy(0.4f), fontSize = 13.sp) }
        items(posted, key = { it.id }) { VideoCard(it) }
    }
}

@Composable
private fun VideoCard(v: VideoItem) {
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(16.dp), color = Card) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(v.prompt, maxLines = 2, fontSize = 15.sp)
            when (v.status) {
                "ready" -> {
                    if (open) {
                        VideoPlayer(
                            v.videoUrl, play = true, fill = false,
                            modifier = Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(12.dp)),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { open = !open }) { Text(if (open) "Скрыть" else "Смотреть") }
                        if (v.published) {
                            OutlinedButton(onClick = { scope.launch { Repo.setPublished(v.id, false) } }) { Text("Снять") }
                        } else {
                            Button(onClick = { scope.launch { Repo.setPublished(v.id, true) } }) { Text("Опубликовать") }
                        }
                        TextButton(onClick = { scope.launch { Repo.delete(v.id) } }) { Text("Удалить") }
                    }
                }
                "failed" -> {
                    Text("Ошибка: ${v.error.ifBlank { "не удалось создать" }}", color = Color(0xFFFF8A8A), fontSize = 13.sp)
                    TextButton(onClick = { scope.launch { Repo.delete(v.id) } }) { Text("Удалить") }
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(if (v.status == "queued") "В очереди…" else "Генерируется…", fontSize = 13.sp)
                }
            }
        }
    }
}
