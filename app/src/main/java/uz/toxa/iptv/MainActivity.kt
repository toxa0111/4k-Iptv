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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Sport(val name: String, val emoji: String, val providers: List<Pair<String, String>>)
private val sports = listOf(
    Sport("Futbol", "⚽", listOf("ZO‘R TV" to "https://zortv.uz/", "Setanta Sports" to "https://setantasports.com/", "FIFA+" to "https://www.plus.fifa.com/")),
    Sport("Basketbol", "🏀", listOf("NBA" to "https://www.nba.com/watch", "FIBA" to "https://www.fiba.basketball/")),
    Sport("Tennis", "🎾", listOf("Tennis TV" to "https://www.tennistv.com/", "WTA" to "https://www.wtatennis.com/")),
    Sport("Boks", "🥊", listOf("DAZN" to "https://www.dazn.com/", "World Boxing" to "https://worldboxing.org/")),
    Sport("MMA", "🥋", listOf("UFC" to "https://www.ufc.com/watch", "UFC Fight Pass" to "https://www.ufcfightpass.com/")),
    Sport("Motorsport", "🏎️", listOf("F1 TV" to "https://www.formula1.com/en/subscribe-to-f1-tv", "MotoGP" to "https://www.motogp.com/")),
    Sport("Voleybol", "🏐", listOf("Volleyball World" to "https://www.volleyballworld.com/")),
    Sport("Xokkey", "🏒", listOf("NHL" to "https://www.nhl.com/subscribe"))
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
    val context = LocalContext.current
    var selectedSport by remember { mutableStateOf("Barchasi") }
    var selectedSection by remember { mutableStateOf("Jonli") }
    var selectedMatch by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    val filteredSports = sports.filter {
        (selectedSport == "Barchasi" || it.name == selectedSport) &&
            (search.isBlank() || it.name.contains(search, true))
    }

    fun openOfficial(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

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
                        Text("LIVE • HD • 4K", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Barcha sport musobaqalari — bitta platformada", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Jonli", "Bugun", "Yaqin", "Yakunlangan")) { section ->
                            FilterChip(
                                selected = selectedSection == section,
                                onClick = { selectedSection = section; selectedMatch = null },
                                label = { Text(section) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Sport turini qidirish") }
                    )
                    Spacer(Modifier.height(10.dp))

                    if (compact) {
                        SportsPicker(selectedSport) { selectedSport = it; selectedMatch = null }
                        Spacer(Modifier.height(10.dp))
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item { EventStatusCard(selectedSection) }
                            if (selectedMatch != null) {
                                item { MatchDetail(selectedMatch!!, filteredSports.firstOrNull(), ::openOfficial) }
                            } else {
                                items(filteredSports) { sport ->
                                    SportCard(sport, ::openOfficial) { selectedMatch = sport.name }
                                }
                            }
                        }
                    } else {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(0.34f)) {
                                Text("SPORT TURLARI", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                SportsPicker(selectedSport) { selectedSport = it; selectedMatch = null }
                            }
                            Column(Modifier.weight(0.66f)) {
                                EventStatusCard(selectedSection)
                                Spacer(Modifier.height(10.dp))
                                if (selectedMatch != null) {
                                    MatchDetail(selectedMatch!!, sports.firstOrNull { it.name == selectedMatch }, ::openOfficial)
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(filteredSports) { sport ->
                                            SportCard(sport, ::openOfficial) { selectedMatch = sport.name }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Eslatma: jonli hisoblar va translyatsiyalar faqat rasmiy ma’lumot/manbalar ulangach ko‘rsatiladi.",
                        color = Color.Gray, fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SportsPicker(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val all = listOf("Barchasi" to "🏆") + sports.map { it.name to it.emoji }
        all.forEach { (name, emoji) ->
            Row(
                Modifier.fillMaxWidth()
                    .background(if (selected == name) Color(0xFF20384A) else panel, RoundedCornerShape(9.dp))
                    .clickable { onSelect(name) }.focusable().padding(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(name, color = if (selected == name) accent else Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EventStatusCard(section: String) {
    Column(
        Modifier.fillMaxWidth().background(panel, RoundedCornerShape(13.dp)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(section.uppercase(), color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text("SPORTS CENTER", color = Color.Gray, fontSize = 9.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when (section) {
                "Jonli" -> "Hozircha jonli uchrashuvlar lentasi ulanmagan"
                "Bugun" -> "Bugungi musobaqalar taqvimi ulanmoqda"
                "Yaqin" -> "Kelgusi uchrashuvlar taqvimi ulanmoqda"
                else -> "Yakunlangan o‘yinlar natijalari ulanmoqda"
            },
            color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Soxta o‘yinlar yoki hisoblar ko‘rsatilmaydi. Rasmiy sport taqvimi va natijalar API manbasi ulanmaguncha ro‘yxat bo‘sh turadi.",
            color = Color.LightGray, fontSize = 12.sp, lineHeight = 17.sp
        )
    }
}

@Composable
private fun SportCard(sport: Sport, openOfficial: (String) -> Unit, onSelect: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(12.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onSelect)) {
            Text(sport.emoji, fontSize = 25.sp)
            Spacer(Modifier.width(10.dp))
            Text(sport.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("Manbalar  ›", color = accent, fontSize = 11.sp)
        }
        Spacer(Modifier.height(9.dp))
        Text("Rasmiy translyatsiya platformalari", color = Color.Gray, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(sport.providers) { (name, url) ->
                OutlinedButton(onClick = { openOfficial(url) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)) {
                    Text(name, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MatchDetail(name: String, sport: Sport?, openOfficial: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(13.dp)).padding(16.dp)) {
        Text("$name — translyatsiya", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Hozircha tasdiqlangan jonli o‘yin yoki litsenziyalangan video manbasi mavjud emas.", color = Color.LightGray, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Text("Rasmiy tomosha qilish xizmatlari", color = accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        (sport?.providers ?: emptyList()).forEach { (name, url) ->
            TextButton(onClick = { openOfficial(url) }) { Text("▶  $name", color = Color.White) }
        }
    }
}
