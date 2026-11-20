package uz.toxa.iptv

import android.net.Uri
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

private data class Channel(val name: String, val url: String, val group: String = "Imported")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 12, 22)
        window.navigationBarColor = android.graphics.Color.rgb(8, 12, 22)
        setContent { IptvApp() }
    }
}

@Composable
private fun IptvApp() {
    val context = LocalContext.current
    val channels = remember { mutableStateListOf<Channel>() }
    val favorites = remember { mutableStateListOf<String>() }
    var selected by remember { mutableStateOf<Channel?>(null) }
    var query by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("M3U playlist manzilini qo‘shing.") }
    var activeCategory by remember { mutableStateOf("All") }
    var showFavorites by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    message = "Ijro xatosi: ${error.errorCodeName}. Stream manzilini tekshiring."
                }
            })
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val groups = remember(channels.toList()) { listOf("All") + channels.map { it.group }.distinct() }
    val visible = channels.filter {
        (activeCategory == "All" || it.group == activeCategory) &&
            (!showFavorites || favorites.contains(it.url)) &&
            (query.isBlank() || it.name.contains(query, true))
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF36D6B0), background = Color(0xFF080C16), surface = Color(0xFF121A2A))) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF080C16)) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("4K", color = Color(0xFF36D6B0), fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("IPTV", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("PHONE • ANDROID TV", color = Color.LightGray, fontSize = 10.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text("Rasmiy futbol va sport efirlari", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://uzvision.tv/en/channel/sport_uz"))
                        context.startActivity(intent)
                    }) { Text("Sport UZ • Jonli") }
                    OutlinedButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.fifa.com/fifaplus"))
                        context.startActivity(intent)
                    }) { Text("FIFA+") }
                }
                Text("Tanlangan musobaqalar bepul. Barcha liga va o‘yinlar bepul bo‘lmasligi mumkin.", color = Color.Gray, fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1.15f)) {
                        Text("Jonli efir", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().weight(0.78f).background(Color.Black, RoundedCornerShape(14.dp))) {
                            if (selected != null) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx -> PlayerView(ctx).apply {
                                        player = player
                                        useController = true
                                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                                    } },
                                    update = { it.player = player }
                                )
                            } else {
                                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("▶", color = Color(0xFF36D6B0), fontSize = 42.sp)
                                    Text("Kanal tanlang", color = Color.White)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(selected?.name ?: message, color = Color.LightGray, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showImport = true }) { Text("M3U qo‘shish") }
                            OutlinedButton(onClick = { showFavorites = !showFavorites }) {
                                Text(if (showFavorites) "Barchasi" else "Sevimlilar")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Kanal qidirish") })
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            groups.take(5).forEach { group ->
                                FilterChip(selected = activeCategory == group, onClick = { activeCategory = group }, label = { Text(group, maxLines = 1) })
                            }
                        }
                    }
                    Column(Modifier.weight(0.85f)) {
                        Text("Kanallar (${visible.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(visible) { channel ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .background(if (selected == channel) Color(0xFF20384A) else Color(0xFF121A2A), RoundedCornerShape(10.dp))
                                        .clickable {
                                            selected = channel
                                            message = "Ulanmoqda: ${channel.name}"
                                            player.setMediaItem(MediaItem.fromUri(Uri.parse(channel.url)))
                                            player.prepare()
                                            player.play()
                                        }.focusable().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(channel.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text(channel.group, color = Color.Gray, fontSize = 10.sp)
                                    }
                                    Text(if (favorites.contains(channel.url)) "★" else "☆", color = Color(0xFFFFD166), fontSize = 22.sp,
                                        modifier = Modifier.clickable {
                                            if (favorites.contains(channel.url)) favorites.remove(channel.url) else favorites.add(channel.url)
                                        }.padding(horizontal = 5.dp))
                                }
                            }
                        }
                    }
                }
                Text("Rasmiy Sport UZ va FIFA+ sahifalari tashqi ilova/brauzerda ochiladi. Haqiqiy 4K faqat manba 2160p uzatsa ishlaydi.", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("M3U playlist qo‘shish") },
            text = {
                Column {
                    Text("M3U/M3U8 playlist URL manzilini kiriting.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = playlistUrl, onValueChange = { playlistUrl = it }, label = { Text("https://.../playlist.m3u") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = playlistUrl.trim()
                    if (url.startsWith("https://") || url.startsWith("http://")) {
                        kotlinx.coroutines.MainScope().launch {
                            try {
                                val content = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    java.net.URL(url).openStream().bufferedReader().use { it.readText() }
                                }
                                val parsed = parseM3u(content)
                                channels.clear()
                                channels.addAll(parsed)
                                message = if (parsed.isEmpty()) "Playlist ichida kanal topilmadi." else "${parsed.size} ta kanal yuklandi."
                            } catch (e: Exception) {
                                message = "Playlist yuklanmadi: ${e.message ?: "tarmoq xatosi"}"
                            }
                            showImport = false
                        }
                    } else {
                        message = "To‘g‘ri HTTP yoki HTTPS manzil kiriting."
                    }
                }) { Text("Yuklash") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("Bekor") } }
        )
    }
}

private fun parseM3u(content: String): List<Channel> {
    val result = mutableListOf<Channel>()
    var name = "Nomsiz kanal"
    var group = "Imported"
    content.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("#EXTINF", true) -> {
                name = line.substringAfterLast(",").trim().ifBlank { "Nomsiz kanal" }
                group = Regex("group-title=\"([^\"]*)\"").find(line)?.groupValues?.getOrNull(1) ?: "Imported"
            }
            line.isNotBlank() && !line.startsWith("#") && (line.startsWith("http://") || line.startsWith("https://")) -> {
                result.add(Channel(name, line, group))
                name = "Nomsiz kanal"
            }
        }
    }
    return result
}
