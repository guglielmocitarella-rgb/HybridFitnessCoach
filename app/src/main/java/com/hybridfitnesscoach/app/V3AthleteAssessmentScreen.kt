package com.hybridfitnesscoach.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentDimension
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentLevel
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentTarget
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentVerification

private val guidedAssessmentDimensions = listOf(
    V3AssessmentDimension.STRENGTH, V3AssessmentDimension.TECHNICAL,
    V3AssessmentDimension.MOBILITY, V3AssessmentDimension.BALANCE_COORDINATION,
    V3AssessmentDimension.HORIZONTAL_PUSH, V3AssessmentDimension.VERTICAL_PUSH,
    V3AssessmentDimension.HORIZONTAL_PULL, V3AssessmentDimension.VERTICAL_PULL,
    V3AssessmentDimension.SQUAT, V3AssessmentDimension.HINGE,
    V3AssessmentDimension.CORE, V3AssessmentDimension.ISOMETRIC,
    V3AssessmentDimension.SKILL
)

private val levelAnchors = listOf(
    "Gestisco una variante molto facilitata e stabile, eventualmente con sostegno o guida frequente.",
    "Gestisco una variante semplice e prevedibile, con controllo di base e autocorrezione dopo un cue.",
    "Gestisco la variante standard scelta con assetto coerente, senza guida passo per passo.",
    "Gestisco un compito più impegnativo mantenendo i criteri della prova.",
    "Gestisco una progressione avanzata o complessa con controllo ripetibile; vale per questa prova."
)

private fun dimensionTitle(dimension: V3AssessmentDimension): String = when (dimension) {
    V3AssessmentDimension.STRENGTH -> "Forza"
    V3AssessmentDimension.TECHNICAL -> "Competenza tecnica"
    V3AssessmentDimension.MOBILITY -> "Mobilità attiva"
    V3AssessmentDimension.BALANCE_COORDINATION -> "Equilibrio e coordinazione"
    V3AssessmentDimension.HORIZONTAL_PUSH -> "Spinta orizzontale"
    V3AssessmentDimension.VERTICAL_PUSH -> "Spinta verticale"
    V3AssessmentDimension.HORIZONTAL_PULL -> "Tirata orizzontale"
    V3AssessmentDimension.VERTICAL_PULL -> "Tirata verticale"
    V3AssessmentDimension.SQUAT -> "Squat"
    V3AssessmentDimension.HINGE -> "Hip hinge"
    V3AssessmentDimension.CORE -> "Core"
    V3AssessmentDimension.ISOMETRIC -> "Isometriche"
    V3AssessmentDimension.SKILL -> "Skill specifica"
}

private fun levelExample(dimension: V3AssessmentDimension, index: Int): String {
    val examples = when (dimension) {
        V3AssessmentDimension.STRENGTH -> listOf("Spinta al muro o alzata da seduta con supporto.", "Spinta inclinata alta o remata con banda sicura.", "Push-up inclinato, squat libero o remata leggera.", "Push-up a terra, squat caricato o trazione con meno assistenza.", "Variante avanzata, zavorrata o unilaterale scelta individualmente.")
        V3AssessmentDimension.TECHNICAL -> listOf("Hinge al bastone con dimostrazione e cue frequenti.", "Hinge noto dopo dimostrazione; correggo un errore dopo richiamo.", "Variante standard con sequenza coerente e autocorrezione.", "Mantengo i criteri quando cambia presa, posizione o attrezzo.", "Adatto i criteri a progressioni complesse senza feedback continuo.")
        V3AssessmentDimension.MOBILITY -> listOf("ROM breve o posizione supportata.", "ROM semplice con supporto o ampiezza ridotta.", "ROM richiesto dalla variante standard con transizioni controllate.", "ROM più ampio pertinente senza compensi dominanti.", "Posizione avanzata controllata; non si trasferisce ad altri distretti.")
        V3AssessmentDimension.BALANCE_COORDINATION -> listOf("Posizione stabile con appoggio e base ampia.", "Trasferimento semplice e prevedibile, con appoggio se serve.", "Sequenza standard senza appoggio continuo.", "Base ridotta o cambio di direzione controllato nel compito.", "Progressione dinamica complessa con setup e guida appropriati.")
        V3AssessmentDimension.HORIZONTAL_PUSH -> listOf("Spinta al muro o supporto alto.", "Spinta inclinata con linea e appoggi mantenuti.", "Push-up standard o press scelto con ROM controllato.", "Leva o resistenza maggiore con tronco e traiettoria controllati.", "Variante avanzata, zavorrata o unilaterale documentata.")
        V3AssessmentDimension.HORIZONTAL_PULL -> listOf("Trazione breve con banda/cavo stabile.", "Remata supportata senza slancio.", "Inverted row facilitata o remata standard controllata.", "Leva, resistenza o unilateralità maggiori senza rotazione dominante.", "Variante a leva sfavorevole o unilaterale registrata.")
        V3AssessmentDimension.VERTICAL_PUSH -> listOf("Press facilitato in ROM confortevole con supporto.", "Press seduto leggero con tronco stabile.", "Press overhead o pike facilitato controllato.", "Leva o resistenza superiore con assetto costale e scapolare.", "Progressione handstand press con setup e uscita controllati.")
        V3AssessmentDimension.VERTICAL_PULL -> listOf("Pull-down leggero e supportato.", "Pull-down o trazione assistita senza slancio.", "Trazione assistita documentata o standard con ritorno controllato.", "Pull-up o variazione più impegnativa senza kip involontario.", "Variante avanzata, zavorrata o unilaterale assistita documentata.")
        V3AssessmentDimension.SQUAT -> listOf("Sit-to-stand con altezza e supporto adeguati.", "Squat verso target con base stabile.", "Squat libero al ROM scelto o macchina controllata.", "Carico, split o ROM maggiore con traiettorie previste.", "Progressione unilaterale controllata, specifica al compito.")
        V3AssessmentDimension.HINGE -> listOf("Hinge al bastone, senza carico e con cue frequenti.", "Hinge al muro o pick-up elevato controllato.", "Deadlift da rialzo o RDL standard ripetibile.", "Resistenza o ROM maggiori senza perdere il pattern.", "Variante unilaterale avanzata con bacino controllato.")
        V3AssessmentDimension.CORE -> listOf("Brace supino o quadrupedico con supporto.", "Dead bug o bird dog semplificato controllato.", "Plank modificato, carry semplice o antirotazione.", "Leva o resistenza superiore senza compensi dominanti.", "Rollout o leva avanzata controllata per questa funzione.")
        V3AssessmentDimension.ISOMETRIC -> listOf("Posizione statica con appoggio ampio e uscita volontaria.", "Forma facilitata con respiro e allineamento controllati.", "Posizione standard senza compensi che annullano il compito.", "Leva maggiore o supporto ridotto mantenendo la forma.", "Skill statica complessa con setup e assistenza documentati.")
        V3AssessmentDimension.SKILL -> listOf("Familiarizzazione della skill specifica con sostegno o cue.", "Prerequisiti e regressione semplice con setup riproducibile.", "Progressione assistita/intermedia con entrata e uscita controllate.", "Nodo target con meno assistenza o leva maggiore.", "Nodo avanzato con controllo ripetibile e relazione verificata.")
    }
    return examples[index]
}

@Composable
internal fun V3AthleteAssessmentScreen(store: AppStore, onClose: () -> Unit, onChanged: () -> Unit) {
    var snapshot by remember { mutableStateOf(store.loadV3AssessmentSnapshot(store.loadAthleteRevision())) }
    var page by remember { mutableIntStateOf(0) }
    var selectedSkill by remember {
        mutableStateOf(snapshot.currentRows().lastOrNull { it.target.dimension == V3AssessmentDimension.SKILL }?.target?.targetId.orEmpty())
    }
    val dimension = guidedAssessmentDimensions[page]
    val target = if (dimension == V3AssessmentDimension.SKILL && selectedSkill.isNotBlank())
        V3AssessmentTarget.skill(selectedSkill) else if (dimension == V3AssessmentDimension.SKILL) null
        else V3AssessmentTarget(dimension)
    val current = target?.let(snapshot::current)
    var answer by remember(page, selectedSkill) { mutableStateOf(current?.level) }
    var skillMenu by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val nodes = remember { SkillCatalog.nodes.sortedBy { it.id } }
    fun saveAnswer(level: V3AssessmentLevel?) {
        val activeTarget = target ?: return
        val saved = store.saveV3Assessment(activeTarget, level,
            assessedAtEpochMs = if (level == null) null else System.currentTimeMillis())
        if (saved) {
            snapshot = store.loadV3AssessmentSnapshot(store.loadAthleteRevision())
            onChanged()
            if (page < guidedAssessmentDimensions.lastIndex) page++
            message = "Risposta salvata come dichiarata."
        } else message = "Salvataggio non riuscito: i dati locali richiedono verifica."
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Valutazione capacità V3", style = MaterialTheme.typography.headlineSmall)
                Text("Stime separate, modificabili e facoltative", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClose) { Text("Chiudi") }
        }
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(progress = { (page + 1f) / guidedAssessmentDimensions.size }, Modifier.fillMaxWidth())
        Text("${page + 1} di ${guidedAssessmentDimensions.size}", modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(dimensionTitle(dimension), style = MaterialTheme.typography.titleLarge)
                Text("Quale variante controlli oggi senza guida passo per passo?", modifier = Modifier.padding(top = 8.dp))
                Text("Scegli il livello più alto per cui riconosci con coerenza i criteri della prova. Nessun numero di ripetizioni o carico universale è richiesto.",
                    modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (dimension == V3AssessmentDimension.SKILL) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { skillMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(nodes.firstOrNull { it.id == selectedSkill }?.let { "${it.skill} • ${it.exerciseName} (${it.id})" } ?: "Scegli skill e nodo")
                    }
                    DropdownMenu(expanded = skillMenu, onDismissRequest = { skillMenu = false }) {
                        nodes.forEach { node ->
                            DropdownMenuItem(text = { Text("${node.skill} • ${node.exerciseName} · ${node.id}") },
                                onClick = { selectedSkill = node.id; skillMenu = false; message = null })
                        }
                    }
                    Text("La skill si registra per questo node ID. Una stima dichiarata non abilita la selezione della skill.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                V3AssessmentLevel.entries.forEachIndexed { index, level ->
                    val selected = answer == level
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = selected, onClick = { answer = level },
                            modifier = Modifier.testTag("v3-assessment-level-${level.name}"))
                        Column(Modifier.padding(top = 12.dp)) {
                            Text(level.name.replace('_', ' '), style = MaterialTheme.typography.titleSmall)
                            Text(levelAnchors[index], style = MaterialTheme.typography.bodySmall)
                            Text("Esempio: ${levelExample(dimension, index)}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                TextButton(onClick = { saveAnswer(null) }, enabled = target != null,
                    modifier = Modifier.testTag("v3-assessment-unknown-option")) {
                    Text("Non so / non l’ho provata · salva come sconosciuta")
                }
                Text("Resta sconosciuta; non equivale al livello più basso.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Conferma: stima autodichiarata basata sugli esempi, non una misurazione oggettiva.",
            modifier = Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall)
        Text("Le prove osservate e le derivazioni non sono attive in questa schermata: non verranno simulate né promosse automaticamente.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        message?.let { Text(it, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(enabled = page > 0, onClick = { page--; message = null }) { Text("Indietro") }
            Button(enabled = answer != null && target != null, onClick = { saveAnswer(answer) }) {
                Text(if (page < guidedAssessmentDimensions.lastIndex) "Salva e continua" else "Salva risposta")
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Valutazioni attuali", style = MaterialTheme.typography.titleMedium)
        snapshot.currentRows().sortedBy { it.target.dimension.ordinal }.forEach { row ->
            val title = if (row.target.dimension == V3AssessmentDimension.SKILL)
                "Skill ${row.target.targetId}" else dimensionTitle(row.target.dimension)
            Text("$title: ${row.level?.name?.replace('_', ' ') ?: "Non so"} • " + when (row.verification) {
                V3AssessmentVerification.DECLARED -> "Dichiarato"
                V3AssessmentVerification.OBSERVED_VALID -> "Osservato"
                V3AssessmentVerification.DERIVED_VALID -> "Derivato"
                V3AssessmentVerification.INVALIDATED -> "Invalidato"
                V3AssessmentVerification.UNKNOWN -> "Sconosciuto"
            }, modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall)
            if (snapshot.hasContradictoryHistory(row.target)) {
                Text("Lo storico suggerisce una rivalutazione; il livello corrente non viene modificato automaticamente.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
