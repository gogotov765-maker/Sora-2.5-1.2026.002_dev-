package com.sora25.app2.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sora25.app2.data.Backend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private val MODES = listOf("text" to "Текст", "character" to "Персонаж", "image" to "Картинка")

/** Читает картинку, уменьшает до 1024 px и сжимает в JPEG (чтобы запрос был лёгким). */
private suspend fun loadJpeg(ctx: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
    val bmp = ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
    val k = 1024f / maxOf(bmp.width, bmp.height)
    val scaled = if (k < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt(), (bmp.height * k).toInt(), true) else bmp
    ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
}

@Composable
fun CreateScreen(onStarted: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var prompt by remember { mutableStateOf("") }
    var character by remember { mutableStateOf("") }
    var duration by remember { mutableIntStateOf(5) }
    var aspect by remember { mutableStateOf("9:16") }
    var image by remember { mutableStateOf<ByteArray?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { image = runCatching { loadJpeg(ctx, uri) }.getOrNull() }
    }
    val mode = MODES[tab].first
    val needsImage = mode != "text"

    Column(Modifier.fillMaxSize()) {
        TabRow(tab, containerColor = Bg) {
            MODES.forEachIndexed { i, (_, title) ->
                Tab(tab == i, { tab = i; error = null }, text = { Text(title) })
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (mode == "character") {
                OutlinedTextField(character, { character = it }, label = { Text("Имя персонажа") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (needsImage) {
                OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (image == null) "Выбрать фото" else "Фото выбрано ✓ (сменить)")
                }
            }
            OutlinedTextField(
                prompt, { prompt = it }, label = { Text("Опиши видео") },
                minLines = 4, modifier = Modifier.fillMaxWidth(),
            )
            Text("Длительность", color = Color.White.copy(0.7f), fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10).forEach { d ->
                    FilterChip(duration == d, { duration = d }, label = { Text("$d с") })
                }
            }
            Text("Формат", color = Color.White.copy(0.7f), fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("9:16", "16:9").forEach { a ->
                    FilterChip(aspect == a, { aspect = a }, label = { Text(a) })
                }
            }
            error?.let { Text(it, color = Color(0xFFFF8A8A), fontSize = 13.sp) }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    error = null
                    when {
                        prompt.isBlank() -> error = "Напиши описание"
                        needsImage && image == null -> error = "Выбери фото"
                        mode == "character" && character.isBlank() -> error = "Укажи имя персонажа"
                        else -> {
                            busy = true
                            scope.launch {
                                Backend.generate(mode, prompt.trim(), duration, aspect, image, character.trim())
                                    .onSuccess { onStarted() }
                                    .onFailure { error = it.message ?: "Ошибка запуска" }
                                busy = false
                            }
                        }
                    }
                },
                enabled = !busy,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Создать", fontSize = 16.sp)
            }
        }
    }
}
