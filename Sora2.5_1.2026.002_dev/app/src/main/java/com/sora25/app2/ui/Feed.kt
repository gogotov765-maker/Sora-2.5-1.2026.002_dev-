package com.sora25.app2.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sora25.app2.data.Repo

/** Вертикальная лента на весь экран, как в Sora. Видно все опубликованные видео. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(active: Boolean) {
    val items by remember { Repo.feed() }.collectAsState(initial = emptyList())
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Пока пусто. Создай первое видео!", color = Color.White.copy(0.7f))
        }
        return
    }
    val pager = rememberPagerState(pageCount = { items.size })
    VerticalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 0) { page ->
        val item = items[page]
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoPlayer(item.videoUrl, play = active && pager.currentPage == page, modifier = Modifier.fillMaxSize())
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(220.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
            )
            Text(
                item.prompt, color = Color.White, fontSize = 15.sp, maxLines = 3,
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            )
        }
    }
}
