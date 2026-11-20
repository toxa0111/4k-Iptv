package uz.toxa.iptv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

private data class Channel(val name: String, val url: String, val group: String = "Imported")
private val demoChannels = listOf(
    Channel("4K sinov videosi • Akamai DASH", "https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd", "Sinov"),
    Channel("HLS sinov videosi • Mux 1080p", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Sinov")
)

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
    val scope = rememberCoroutineScope()
    val channels = remember { mutableStateListOf<Channel>().apply { addAll(demoChannels) } }
    val favorites = remember { mutableStateListOf<String>() }
    var selected by remember { mutableStateOf<Channel?>(null) }
    var query by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Sinov videolari tayyor. Kanalni tanlang.") }
    var category by remember { mutableStateOf("All") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_BUFFERING) message = "Video yuklanmoqda…"
                    if (state == Player.STATE_READY) message = "Ijro qilinmoqda"
                    if (state == Player.STATE_ENDED) message = "Video tugadi"
                }
                override fun onPlayerError(error: PlaybackException) {
                    message = "Ijro xatosi: " + error.errorCodeName + ". Manbani tekshiring."
                }
            })
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val groups = listOf("All") + channels.map { it.group }.distinct()
    val visible = channels.filter {
        (category == "All" || it.group == category) &&
            (!onlyFavorites || favorites.contains(it.url)) &&
            (query.isBlank() || it.name.contains(query, true))
    }
    fun play(channel: Channel) {
        selected = channel
        message = "Ulanmoqda…"
        player.setMediaItem(MediaItem.fromUri(Uri.parse(channel.url)))
        player.prepare()
        player.play()
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF36D6B0), background = Color(0xFF080C16), surface = Color(0xFF121A2A))) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF080C16)) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val compact = maxWidth < 600.dp
                Column(Modifier.fillMaxSize().padding(if (compact) 12.dp else 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("4K", color = Color(0xFF36D6B0), fontSize = 29.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(7.dp))
                        Text("IPTV", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(if (compact) "ANDROID" else "PHONE • ANDROID TV", color = Color.LightGray, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://uzvision.tv/en/channel/sport_uz"))) }
                                .onFailure { message = "Brauzer ochilmadi" }
                        }, modifier = Modifier.weight(1f)) { Text("Sport UZ") }
                        OutlinedButton(onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.dazn.com/fifaplus"))) }
                                .onFailure { message = "Brauzer ochilmadi" }
                        }, modifier = Modifier.weight(1f)) { Text("FIFA+ / DAZN") }
                    }
                    Text("Rasmiy xizmat sahifalari ochiladi. Barcha futbol o‘yinlari bepul emas.", color = Color.Gray, fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))

                    if (compact) {
                        Column(Modifier.fillMaxSize()) {
                            PlayerPanel(player, selected, true)
                            Spacer(Modifier.height(5.dp))
                            Text((selected?.name ?: "Kanal tanlanmagan") + " • " + message, color = Color.LightGray, fontSize = 11.sp, maxLines = 2)
                            Spacer(Modifier.height(6.dp))
                            Controls(query, { query = it }, { showImport = true }, onlyFavorites, { onlyFavorites = !onlyFavorites }, loading)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                items(groups) { g -> FilterChip(selected = category == g, onClick = { category = g }, label = { Text(g) }) }
                            }
                            Text("Kanallar (" + visible.size + ")", color = Color.White, fontWeight = FontWeight.Bold)
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                items(visible, key = { it.url }) { c ->
                                    ChannelRow(c, selected == c, favorites.contains(c.url), { play(c) }) {
                                        if (favorites.contains(c.url)) favorites.remove(c.url) else favorites.add(c.url)
                                    }
                                }
                            }
                        }
                    } else {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(Modifier.weight(1.15f)) {
                                Text("Video pleer", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                PlayerPanel(player, selected, false, Modifier.weight(1f))
                                Spacer(Modifier.height(6.dp))
                                Text((selected?.name ?: "Kanal tanlanmagan") + " • " + message, color = Color.LightGray, fontSize = 12.sp, maxLines = 2)
                                Spacer(Modifier.height(8.dp))
                                Controls(query, { query = it }, { showImport = true }, onlyFavorites, { onlyFavorites = !onlyFavorites }, loading)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 5.dp)) {
                                    items(groups) { g -> FilterChip(selected = category == g, onClick = { category = g }, label = { Text(g, maxLines = 1) }) }
                                }
                            }
                            Column(Modifier.weight(0.85f)) {
                                Text("Kanallar (" + visible.size + ")", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(Modifier.height(8.dp))
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(visible, key = { it.url }) { c ->
                                        ChannelRow(c, selected == c, favorites.contains(c.url), { play(c) }) {
                                            if (favorites.contains(c.url)) favorites.remove(c.url) else favorites.add(c.url)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text("Sinov videolari jonli TV emas. Haqiqiy 4K uchun manba 2160p bo‘lishi kerak.", color = Color.Gray, fontSize = 9.sp)
                }
            }
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { if (!loading) showImport = false },
            title = { Text("M3U yoki video manzilini qo‘shish") },
            text = {
                Column {
                    Text("M3U playlist yoki bevosita .m3u8 / .mpd manzilini kiriting.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = playlistUrl, onValueChange = { playlistUrl = it }, label = { Text("https://...") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                    if (loading) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !loading, onClick = {
                    val url = playlistUrl.trim()
                    if (!url.startsWith("https://") && !url.startsWith("http://")) {
                        message = "To‘g‘ri HTTP yoki HTTPS manzil kiriting."
                    } else {
                        scope.launch {
                            loading = true
                            try {
                                val parsed = withContext(Dispatchers.IO) {
                                    val connection = URL(url).openConnection().apply {
                                        connectTimeout = 12000
                                        readTimeout = 12000
                                    }
                                    val body = connection.getInputStream().bufferedReader().use { it.readText() }
                                    parseM3u(body)
                                }
                                val result = if (parsed.isNotEmpty()) parsed else listOf(Channel("To‘g‘ridan-to‘g‘ri stream", url))
                                channels.removeAll { it.group != "Sinov" }
                                channels.addAll(result)
                                category = "All"
                                onlyFavorites = false
                                message = result.size.toString() + " ta kanal/manba qo‘shildi."
                                showImport = false
                            } catch (e: Exception) {
                                message = "Manzil yuklanmadi: " + (e.message ?: "tarmoq xatosi")
                            } finally {
                                loading = false
                            }
                        }
                    }
                }) { Text("Yuklash") }
            },
            dismissButton = { TextButton(onClick = { if (!loading) showImport = false }) { Text("Bekor") } }
        )
    }
}

@Composable
private fun PlayerPanel(player: ExoPlayer, selected: Channel?, compact: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth()
            .then(if (compact) Modifier.aspectRatio(16f / 9f) else Modifier.heightIn(min = 220.dp))
            .background(Color.Black, RoundedCornerShape(12.dp))
    ) {
        if (selected != null) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            }, update = { it.player = player })
        } else {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▶", color = Color(0xFF36D6B0), fontSize = 38.sp)
                Text("Sinov kanalini tanlang", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Controls(query: String, onQuery: (String) -> Unit, onImport: () -> Unit,
                     favorites: Boolean, onFavorites: () -> Unit, loading: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onImport, enabled = !loading, modifier = Modifier.weight(1f)) { Text("M3U qo‘shish", maxLines = 1) }
        OutlinedButton(onClick = onFavorites, modifier = Modifier.weight(1f)) { Text(if (favorites) "Barchasi" else "★ Sevimli", maxLines = 1) }
    }
    Spacer(Modifier.height(5.dp))
    OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Kanal qidirish") })
}

@Composable
private fun ChannelRow(channel: Channel, selected: Boolean, favorite: Boolean, onPlay: () -> Unit, onFavorite: () -> Unit) {
    Row(Modifier.fillMaxWidth()
        .background(if (selected) Color(0xFF20384A) else Color(0xFF121A2A), RoundedCornerShape(9.dp))
        .clickable(onClick = onPlay).focusable().padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(channel.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(channel.group, color = Color.Gray, fontSize = 10.sp)
        }
        Text(if (favorite) "★" else "☆", color = Color(0xFFFFD166), fontSize = 21.sp,
            modifier = Modifier.clickable(onClick = onFavorite).padding(horizontal = 5.dp))
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
            line.isNotBlank() && !line.startsWith("#") &&
                (line.startsWith("http://") || line.startsWith("https://")) -> {
                result.add(Channel(name, line, group))
                name = "Nomsiz kanal"
            }
        }
    }
    return result
}
