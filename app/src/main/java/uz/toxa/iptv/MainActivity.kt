package uz.toxa.iptv

import android.net.Uri
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

private data class Sport(val name: String, val emoji: String, val leagues: List<String>)
private val sports = listOf(
    Sport("Futbol", "⚽", listOf("UEFA Champions League", "Premier League", "La Liga", "Serie A", "Bundesliga", "O‘zbekiston Superligasi")),
    Sport("Basketbol", "🏀", listOf("NBA", "EuroLeague", "FIBA")),
    Sport("Tennis", "🎾", listOf("ATP Tour", "WTA Tour", "Grand Slam")),
    Sport("Boks", "🥊", listOf("Professional boks", "World Boxing")),
    Sport("MMA", "🥋", listOf("UFC", "PFL", "ONE Championship")),
    Sport("Motorsport", "🏎️", listOf("Formula 1", "MotoGP", "WRC")),
    Sport("Voleybol", "🏐", listOf("Volleyball Nations League", "CEV")),
    Sport("Xokkey", "🏒", listOf("NHL", "IIHF"))
)

private val bg = Color(0xFF080C16)
private val panel = Color(0xFF121A2A)
private val accent = Color(0xFF36D6B0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 12, 22)
        window.navigationBarColor = android.graphics.Color.rgb(8, 12, 22)
        setContent { SportsApp() }
    }
}

@Composable
private fun SportsApp() {
    var selectedSport by remember { mutableStateOf("Futbol") }
    var selectedSection by remember { mutableStateOf("Jonli") }
    var search by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }
    var playRequest by remember { mutableIntStateOf(0) }
    var playerMessage by remember { mutableStateOf("Translyatsiya manbasi tanlanmagan") }

    val selected = sports.firstOrNull { it.name == selectedSport } ?: sports.first()
    val filtered = sports.filter { search.isBlank() || it.name.contains(search, true) }

    MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = bg, surface = panel)) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val compact = maxWidth < 650.dp
                Column(Modifier.fillMaxSize().padding(if (compact) 12.dp else 22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TOXA", color = accent, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("SPORTS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("ICHKI LIVE PLAYER", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Sportni tanlang — translyatsiya shu ilovaning ichida ochiladi", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Jonli", "Bugun", "Yaqin", "Yakunlangan").forEach { section ->
                            FilterChip(selected = selectedSection == section, onClick = { selectedSection = section }, label = { Text(section, fontSize = 11.sp) })
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Sport turini qidirish") }
                    )
                    Spacer(Modifier.height(10.dp))

                    if (compact) {
                        Column(Modifier.weight(1f)) {
                            LazyColumn(modifier = Modifier.weight(0.43f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(filtered) { sport ->
                                    SportChoice(sport, selectedSport == sport.name) {
                                        selectedSport = sport.name
                                        streamUrl = ""
                                        playerMessage = "Translyatsiya manbasi tanlanmagan"
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            SportDetail(selected, selectedSection, streamUrl, { streamUrl = it }, {
                                if (streamUrl.startsWith("https://") || streamUrl.startsWith("http://")) {
                                    playRequest++
                                    playerMessage = "Manba pleerga yuklanmoqda…"
                                } else playerMessage = "HLS (.m3u8) yoki DASH (.mpd) manzilini kiriting"
                            }, playRequest, playerMessage, { playerMessage = it })
                        }
                    } else {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            LazyColumn(Modifier.weight(0.30f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(filtered) { sport ->
                                    SportChoice(sport, selectedSport == sport.name) {
                                        selectedSport = sport.name
                                        streamUrl = ""
                                        playerMessage = "Translyatsiya manbasi tanlanmagan"
                                    }
                                }
                            }
                            SportDetail(
                                selected, selectedSection, streamUrl, { streamUrl = it },
                                {
                                    if (streamUrl.startsWith("https://") || streamUrl.startsWith("http://")) {
                                        playRequest++
                                        playerMessage = "Manba pleerga yuklanmoqda…"
                                    } else playerMessage = "HLS (.m3u8) yoki DASH (.mpd) manzilini kiriting"
                                },
                                playRequest, playerMessage, { playerMessage = it }, Modifier.weight(0.70f)
                            )
                        }
                    }
                    Text("Eslatma: haqiqiy musobaqa translyatsiyasi uchun huquq egasi ruxsat bergan video manba kerak.", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SportChoice(sport: Sport, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (selected) Color(0xFF20384A) else panel, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick).focusable().padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(sport.emoji, fontSize = 19.sp)
        Spacer(Modifier.width(9.dp))
        Text(sport.name, color = if (selected) accent else Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun SportDetail(
    sport: Sport,
    section: String,
    streamUrl: String,
    onUrlChange: (String) -> Unit,
    onPlay: () -> Unit,
    playRequest: Int,
    playerMessage: String,
    onPlayerMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().background(panel, RoundedCornerShape(13.dp)).padding(12.dp)) {
        Text("${sport.emoji}  ${sport.name}", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(
            when (section) {
                "Jonli" -> "Jonli uchrashuvlar"
                "Bugun" -> "Bugungi uchrashuvlar"
                "Yaqin" -> "Yaqinlashayotgan uchrashuvlar"
                else -> "Yakunlangan uchrashuvlar"
            },
            color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp
        )
        Spacer(Modifier.height(7.dp))
        Text("Turnirlar", color = Color.LightGray, fontSize = 11.sp)
        sport.leagues.forEach { league ->
            Text("•  $league", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text("TRANSLYATSIYA MANBASI", color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        OutlinedTextField(
            value = streamUrl, onValueChange = onUrlChange, modifier = Modifier.fillMaxWidth(),
            singleLine = true, label = { Text("Ruxsatli HLS (.m3u8) yoki DASH (.mpd) manzili") }
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
            Text("▶  ILOVA ICHIDA TOMOSHA QILISH")
        }
        Text(playerMessage, color = Color.LightGray, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        InAppPlayer(streamUrl, playRequest, onPlayerMessage)
    }
}

@Composable
private fun InAppPlayer(url: String, playRequest: Int, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build().apply { playWhenReady = true } }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onMessage("Ijro xatosi: ${error.errorCodeName}")
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) onMessage("Video tayyor — ijro etilmoqda")
                if (playbackState == Player.STATE_BUFFERING) onMessage("Video yuklanmoqda…")
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(playRequest) {
        if (playRequest > 0 && url.isNotBlank()) {
            player.setMediaItem(MediaItem.fromUri(Uri.parse(url.trim())))
            player.prepare()
            player.play()
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 340.dp).background(Color.Black, RoundedCornerShape(10.dp)),
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                player = this@InAppPlayer.player
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            }
        },
        update = { it.player = player }
    )
}
