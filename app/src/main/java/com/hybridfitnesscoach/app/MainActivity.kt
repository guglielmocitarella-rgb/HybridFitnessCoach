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
private fun Home(profile: EquipmentProfile, changeProfile: () -> Unit) {
    var workoutOpen by remember { mutableStateOf(false) }
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
    if (workoutOpen) { WorkoutDialog(profile = profile, onDismiss = { workoutOpen = false }) }
}

@Composable private fun Recovery(name: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Accent); Text(name, color = Color.Gray) } }
@Composable private fun SectionTitle(s: String) { Text(s, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 10.dp)) }
@Composable private fun ExerciseRow(a: String, b: String) { Card(Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FitnessCenter, null, tint = Accent); Spacer(Modifier.width(14.dp)); Column { Text(a, fontWeight = FontWeight.SemiBold); Text(b, color = Color.Gray, fontSize = 13.sp) } } } }

@Composable
private fun Library() {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Exercise?>(null) }
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
    selected?.let { ex -> ExerciseDetailDialog(ex = ex, onDismiss = { selected = null }) }
}

@Composable
private fun Equipment(active: EquipmentProfile, select: (EquipmentProfile) -> Unit) {
    var showCreator by remember { mutableStateOf(false) }
    var customProfile by remember { mutableStateOf<EquipmentProfile?>(null) }
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
    if (showCreator) {
        EquipmentProfileDialog(
            onDismiss = { showCreator = false },
            onCreate = { created -> customProfile = created; select(created); showCreator = false }
        )
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


@Composable
private fun WorkoutDialog(profile: EquipmentProfile, onDismiss: () -> Unit) {
    val compatible = remember(profile) {
        exercises.filter { ex ->
            profile.equipment.any { eq ->
                ex.equipment.contains(eq, ignoreCase = true) ||
                    (eq == "Corpo libero" && ex.equipment.contains("corpo libero", ignoreCase = true)) ||
                    (eq == "Macchine" && ex.equipment.contains("Macchina", ignoreCase = true))
            }
        }.take(6)
    }
    val completedSets = remember { mutableStateMapOf<String, Int>() }
    val totalSets = compatible.size * 3
    val doneSets = completedSets.values.sum()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allenamento • " + profile.name) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                item {
                    Text("Sessione generata usando l'attrezzatura disponibile.", color = Color.Gray)
                    LinearProgressIndicator(
                        progress = { if (totalSets == 0) 0f else doneSets.toFloat() / totalSets },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }
                if (compatible.isEmpty()) {
                    item { Text("Nessun esercizio compatibile con questo profilo.") }
                }
                items(compatible) { ex ->
                    val count = completedSets[ex.name] ?: 0
                    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(ex.name, fontWeight = FontWeight.Bold)
                            Text("3 serie • " + ex.equipment, color = Color.Gray, fontSize = 12.sp)
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(count.toString() + "/3", color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                OutlinedButton(onClick = { if (count > 0) completedSets[ex.name] = count - 1 }) { Text("−") }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { if (count < 3) completedSets[ex.name] = count + 1 }) { Text("+ SERIE") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(if (totalSets > 0 && doneSets == totalSets) "TERMINA" else "SALVA E CHIUDI") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANNULLA") } }
    )
}

@Composable
private fun ExerciseDetailDialog(ex: Exercise, onDismiss: () -> Unit) {
    val instructions = when (ex.name) {
        "Trazioni prone" -> "Parti in sospensione completa, mantieni il tronco stabile e porta il petto verso la sbarra senza slancio. Torna in basso in controllo."
        "Lat machine" -> "Mantieni il torace alto e tira la barra verso la parte alta del petto. Evita di trasformare il movimento in uno slancio del busto."
        "Panca piana" -> "Mantieni scapole addotte e piedi stabili. Scendi in controllo verso il petto e spingi mantenendo polsi e gomiti stabili."
        "Push-up" -> "Mantieni testa, tronco e bacino allineati. Scendi in controllo e spingi senza perdere la tensione del core."
        "Dip alle parallele" -> "Mantieni le spalle stabili, scendi in controllo e risali senza rimbalzo. Interrompi se avverti dolore alla spalla."
        "Squat" -> "Mantieni il piede stabile e il tronco compatto. Scendi in controllo e risali coordinando ginocchia e anche."
        "Bulgarian split squat" -> "Mantieni il bacino stabile, scendi verticalmente e spingi soprattutto con la gamba anteriore."
        "Pike push-up" -> "Mantieni il bacino alto e porta la testa tra le mani in controllo. Spingi mantenendo le spalle attive."
        "Hollow body hold" -> "Mantieni la zona lombare aderente al suolo e il bacino in retroversione. Riduci la leva se perdi la posizione."
        else -> "Esegui il movimento lentamente e in controllo, senza slanci, mantenendo una tecnica stabile."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ex.name) },
        text = {
            Column {
                Text(ex.group + " • " + ex.equipment + " • " + ex.level, color = Color.Gray)
                Spacer(Modifier.height(14.dp))
                Text("Come eseguirlo", fontWeight = FontWeight.Bold)
                Text(instructions, modifier = Modifier.padding(top = 6.dp))
                ex.skill?.let {
                    Spacer(Modifier.height(12.dp))
                    Text("Progressione collegata: " + it, color = Accent, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CHIUDI") } }
    )
}

@Composable
private fun EquipmentProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (EquipmentProfile) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val options = listOf("Corpo libero", "Sbarra", "Parallele", "Bilanciere", "Manubri", "Cavi", "Macchine", "Elastici")
    val selected = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo profilo attrezzatura") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome (es. Hotel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Attrezzatura disponibile", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                }
                items(options) { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (item in selected) selected.remove(item) else selected.add(item)
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item in selected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (item !in selected) selected.add(item)
                                } else selected.remove(item)
                            }
                        )
                        Text(item)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(EquipmentProfile(name.trim(), "📍", selected.toList())) },
                enabled = name.isNotBlank() && selected.isNotEmpty()
            ) { Text("CREA") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANNULLA") } }
    )
}
