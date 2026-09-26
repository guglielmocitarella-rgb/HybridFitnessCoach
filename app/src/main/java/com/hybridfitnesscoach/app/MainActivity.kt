package com.hybridfitnesscoach.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF3568E8)
private val Bg = Color(0xFFF7F8FA)

data class EquipmentProfile(val name: String, val icon: String, val equipment: List<String>)
data class Exercise(val name: String, val group: String, val equipment: String, val level: String, val skill: String? = null)

private val profiles = listOf(
    EquipmentProfile("Palestra", "🏋️", listOf("Bilanciere", "Manubri", "Cavi", "Macchine", "Sbarra", "Parallele")),
    EquipmentProfile("Casa", "🏠", listOf("Corpo libero", "Sbarra", "Elastici")),
    EquipmentProfile("Parco", "🌳", listOf("Sbarra", "Parallele", "Corpo libero"))
)

private val exercises = listOf(
    Exercise("Trazioni prone", "Dorso", "Sbarra", "Intermedio", "Muscle-up"),
    Exercise("Lat machine", "Dorso", "Macchina", "Principiante"),
    Exercise("Panca piana", "Petto", "Bilanciere", "Intermedio"),
    Exercise("Push-up", "Petto", "Corpo libero", "Principiante", "Planche"),
    Exercise("Dip alle parallele", "Petto / Tricipiti", "Parallele", "Intermedio", "Muscle-up"),
    Exercise("Squat", "Gambe", "Bilanciere", "Intermedio"),
    Exercise("Bulgarian split squat", "Gambe", "Manubri / corpo libero", "Intermedio"),
    Exercise("Pike push-up", "Spalle", "Corpo libero", "Intermedio", "Handstand push-up"),
    Exercise("Hollow body hold", "Core", "Corpo libero", "Principiante", "Front lever")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HybridFitnessApp() }
    }
}

enum class Tab { HOME, LIBRARY, EQUIPMENT, PROGRESS }

@Composable
fun HybridFitnessApp() {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var activeProfile by remember { mutableStateOf(profiles.first()) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Bg, surface = Color.White)) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf(
                        Triple(Tab.HOME, Icons.Default.Home, "Oggi"),
                        Triple(Tab.LIBRARY, Icons.Default.Search, "Esercizi"),
                        Triple(Tab.EQUIPMENT, Icons.Default.FitnessCenter, "Attrezzatura"),
                        Triple(Tab.PROGRESS, Icons.Default.TrendingUp, "Progressi")
                    ).forEach { (item, icon, label) ->
                        NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(icon, label) }, label = { Text(label) })
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    Tab.HOME -> Home(activeProfile) { tab = Tab.EQUIPMENT }
                    Tab.LIBRARY -> Library()
                    Tab.EQUIPMENT -> Equipment(activeProfile) { activeProfile = it }
                    Tab.PROGRESS -> Progress()
                }
            }
        }
    }
}

@Composable private fun Header(title: String, subtitle: String? = null) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun Home(profile: EquipmentProfile, changeProfile: () -> Unit) {\n    var workoutOpen by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header("Buongiorno 👋", "Il tuo coach ibrido: palestra + calisthenics") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.icon, fontSize = 30.sp); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("Ti alleni in", color = Color.Gray); Text(profile.name, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = changeProfile) { Text("CAMBIA") }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    Text("Allenamento suggerito", fontWeight = FontWeight.SemiBold)
                    Text("Upper body ibrido", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    Text("55 min  •  6 esercizi  •  Forza + skill", color = Color.Gray)
                    Button(onClick = { workoutOpen = true }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp), shape = RoundedCornerShape(14.dp)) { Text("INIZIA ALLENAMENTO") }
                }
            }
        }
        item { SectionTitle("Oggi") }
        items(listOf("Trazioni prone" to "3 × 6 · Skill: Muscle-up", "Panca piana" to "3 × 8 · 70 kg", "Pike push-up" to "3 × 10 · Skill: HSPU")) { (a,b) -> ExerciseRow(a,b) }
        item { SectionTitle("Recupero") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Recovery("Dorso", "92%"); Recovery("Petto", "78%"); Recovery("Gambe", "100%")
                }
            }
        }
    }
}

@Composable private fun Recovery(name: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Accent); Text(name, color = Color.Gray) } }
@Composable private fun SectionTitle(s: String) { Text(s, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 10.dp)) }
@Composable private fun ExerciseRow(a: String, b: String) { Card(Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FitnessCenter, null, tint = Accent); Spacer(Modifier.width(14.dp)); Column { Text(a, fontWeight = FontWeight.SemiBold); Text(b, color = Color.Gray, fontSize = 13.sp) } } } }

@Composable
private fun Library() {
    var query by remember { mutableStateOf("") }
    val shown = exercises.filter { query.isBlank() || it.name.contains(query, true) || it.group.contains(query, true) }
    Column {
        Header("Libreria esercizi", "Pesi, corpo libero e progressioni")
        OutlinedTextField(query, { query = it }, leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Cerca esercizio o muscolo") }, modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        LazyColumn(contentPadding = PaddingValues(20.dp)) {
            items(shown) { ex ->
                Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { selected = ex }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(ex.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${ex.group} • ${ex.equipment} • ${ex.level}", color = Color.Gray)
                        ex.skill?.let { AssistChip(onClick = { selected = ex }, label = { Text("Progressione → $it") }, leadingIcon = { Icon(Icons.Default.AccountTree, null) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Equipment(active: EquipmentProfile, select: (EquipmentProfile) -> Unit) {
    Column {
        Header("Profili attrezzatura", "Scegli dove ti alleni oggi")
        (profiles + listOfNotNull(customProfile)).forEach { p ->
            Card(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth().clickable { select(p) }, colors = CardDefaults.cardColors(containerColor = if (p == active) Accent.copy(alpha=.10f) else Color.White)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.icon, fontSize = 32.sp); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(p.name, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(p.equipment.joinToString(" • "), color = Color.Gray, fontSize = 13.sp) }
                    if (p == active) Icon(Icons.Default.CheckCircle, "Attivo", tint = Accent)
                }
            }
        }
        OutlinedButton(onClick = { showCreator = true }, modifier = Modifier.padding(20.dp).fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("NUOVO PROFILO") }
    }
}

@Composable
private fun Progress() {
    Column {
        Header("Progressi", "Forza e skill nello stesso posto")
        SectionTitle("Skill")
        listOf("Muscle-up" to 34, "Handstand push-up" to 48, "Front lever" to 21).forEach { (name, pct) ->
            Card(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, fontWeight = FontWeight.Bold); Text("$pct%", color = Accent) }; LinearProgressIndicator(progress = { pct/100f }, modifier = Modifier.fillMaxWidth().padding(top=10.dp)) } }
        }
        SectionTitle("Forza")
        ExerciseRow("Panca piana", "70 kg × 8 · +5 kg nelle ultime 6 settimane")
        ExerciseRow("Trazioni", "8 reps · +2 reps nelle ultime 4 settimane")
    }
}
