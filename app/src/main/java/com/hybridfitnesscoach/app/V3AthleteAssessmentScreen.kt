package com.hybridfitnesscoach.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.hybridfitnesscoach.app.domain.v3.V3AssessmentRevision
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

private val italianLevelLabels = listOf(
    "In familiarizzazione", "Base con assistenza", "Autonomia nella variante scelta",
    "Variante impegnativa controllata", "Progressione avanzata controllata"
)

private data class AssessmentLevelCopy(val anchor: String, val example: String)

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

private fun levelLabel(level: V3AssessmentLevel): String = italianLevelLabels[V3AssessmentLevel.entries.indexOf(level)]

private fun levelCopy(
    dimension: V3AssessmentDimension,
    index: Int,
    selectedNode: SkillNode?
): AssessmentLevelCopy {
    val commonAnchors = listOf(
        "Variante molto facilitata e stabile; può servire sostegno o guida frequente.",
        "Variante semplice e prevedibile; controllo di base e autocorrezione dopo un richiamo.",
        "Variante standard scelta, con assetto e traiettoria coerenti senza guida passo per passo.",
        "Compito più impegnativo per leva, resistenza, ROM attivo o coordinazione, mantenendo i criteri della prova.",
        "Progressione avanzata o complessa con controllo ripetibile; il livello vale solo per questa prova."
    )
    val examples = when (dimension) {
        V3AssessmentDimension.STRENGTH -> listOf(
            "Spinta al muro, alzata da seduta alta con supporto o presa leggera vicino al corpo.",
            "Spinta inclinata alta, sit-to-stand controllato o remata con banda ancorata in sicurezza.",
            "Push-up inclinato o press leggero, squat al ROM scelto o remata leggera.",
            "Push-up a terra, squat caricato o trazione con assistenza ridotta, valutati singolarmente.",
            "Variante avanzata, zavorrata o unilaterale con resistenza scelta individualmente; non è un massimale."
        )
        V3AssessmentDimension.TECHNICAL -> listOf(
            "Pattern semplice solo dopo dimostrazione e con richiami frequenti.",
            "Pattern noto dopo dimostrazione; mantiene alcuni punti e corregge dopo un richiamo.",
            "Variante standard con sequenza coerente; recepisce un richiamo e riconosce una ripetizione non riuscita.",
            "Mantiene i criteri quando cambia una variabile già compresa, per esempio presa o attrezzo.",
            "Adatta i criteri a progressioni complesse senza feedback continuo."
        )
        V3AssessmentDimension.MOBILITY -> listOf(
            "Posizione o ROM breve e controllabile, eventualmente con un target o supporto.",
            "ROM semplice senza forzatura, con supporto o ampiezza ridotta per conservare l'assetto.",
            "ROM richiesto dalla variante scelta e transizioni controllate.",
            "ROM più ampio pertinente al compito senza compensi dominanti.",
            "Posizione attiva avanzata controllata; il risultato non si trasferisce ad altri distretti."
        )
        V3AssessmentDimension.BALANCE_COORDINATION -> listOf(
            "Posizione con appoggio o base ampia; un trasferimento richiede sostegno.",
            "Trasferimento semplice e prevedibile; usa appoggio se serve e arresta il gesto controllando.",
            "Sequenza standard e trasferimento del peso senza appoggio continuo; recupera un piccolo errore controllato.",
            "Coordinazione con base ridotta o cambio di direzione, se pertinenti al compito scelto.",
            "Progressione dinamica complessa solo con setup e guida appropriati; non generalizzare ad altri contesti."
        )
        V3AssessmentDimension.HORIZONTAL_PUSH -> listOf(
            "Spinta al muro o verso un supporto alto.",
            "Spinta inclinata con linea e appoggi mantenuti.",
            "Push-up standard o press scelto con ROM controllato.",
            "Leva o resistenza maggiore con tronco e traiettoria controllati.",
            "Variante avanzata, zavorrata o unilaterale documentata con controllo ripetibile."
        )
        V3AssessmentDimension.VERTICAL_PUSH -> listOf(
            "Press facilitato in ROM confortevole, con supporto.",
            "Press seduto leggero con tronco stabile e ritorno controllato.",
            "Press overhead o pike facilitato con ROM attivo controllato.",
            "Leva o resistenza superiore mantenendo l'assetto richiesto.",
            "Progressione handstand press con setup e uscita controllati; la supervisione dipende dal contesto."
        )
        V3AssessmentDimension.HORIZONTAL_PULL -> listOf(
            "Trazione breve con banda o cavo stabile e resistenza minima.",
            "Remata supportata senza slancio.",
            "Inverted row facilitata o remata standard con ritorno controllato.",
            "Leva, resistenza o unilateralità maggiori senza rotazione compensatoria dominante.",
            "Variante a leva sfavorevole o unilaterale complessa, registrata per questa prova."
        )
        V3AssessmentDimension.VERTICAL_PULL -> listOf(
            "Pull-down leggero e supportato; la sospensione non è richiesta.",
            "Pull-down o trazione assistita con presa e assetto controllati, senza slancio.",
            "Trazione assistita dichiarata o standard con transizione controllata.",
            "Pull-up o variazione più impegnativa senza kip involontario.",
            "Variante avanzata, zavorrata o unilaterale con assistenza dichiarata."
        )
        V3AssessmentDimension.SQUAT -> listOf(
            "Sit-to-stand con altezza e supporto che consentono controllo.",
            "Squat verso un target con base stabile e ROM non forzato.",
            "Squat libero al ROM scelto o macchina controllata.",
            "Carico, split o ROM maggiore con traiettorie previste.",
            "Progressione unilaterale avanzata controllata, specifica al compito."
        )
        V3AssessmentDimension.HINGE -> listOf(
            "Hinge al bastone, senza carico e con richiami frequenti.",
            "Hinge al muro o pick-up elevato con rapporto tronco-bacino controllato.",
            "Deadlift da rialzo o RDL standard con traiettoria ripetibile.",
            "Resistenza o ROM maggiori senza perdere il pattern.",
            "Variante unilaterale avanzata con bacino controllato."
        )
        V3AssessmentDimension.CORE -> listOf(
            "Brace supino o quadrupedico con supporto.",
            "Dead bug o bird dog semplificato, mantenendo la posizione scelta.",
            "Plank modificato, carry semplice o antirotazione con controllo respiratorio.",
            "Leva o resistenza superiore senza compensi dominanti.",
            "Rollout o leva avanzata controllata per questa funzione."
        )
        V3AssessmentDimension.ISOMETRIC -> listOf(
            "Posizione statica con appoggio ampio e uscita volontaria.",
            "Forma facilitata con respiro e allineamento controllati.",
            "Posizione standard scelta senza compensi che annullano il compito.",
            "Leva maggiore o supporto ridotto mantenendo la forma.",
            "Skill statica complessa con setup e assistenza documentati."
        )
        V3AssessmentDimension.SKILL -> {
            val name = selectedNode?.exerciseName ?: "progressione selezionata"
            listOf(
                "Familiarizzazione o regressione di «$name», con sostegno o richiami frequenti.",
                "Variante semplice collegata a «$name», in setup riproducibile e con autocorrezione dopo un richiamo.",
                "«$name» nella forma scelta, con transizione controllata e senza guida continua.",
                "Progressione collegata a «$name» con meno assistenza o leva maggiore, solo se descritta nei dati.",
                "Nodo avanzato collegato a «$name» con controllo ripetibile, solo se la relazione è documentata."
            )
        }
    }
    return AssessmentLevelCopy(commonAnchors[index], examples[index])
}

private fun assessmentStatus(revision: V3AssessmentRevision?): String {
    if (revision == null) return "Non valutata"
    if (revision.verification == V3AssessmentVerification.INVALIDATED) {
        return "Da rivalutare: valutazione precedente invalidata"
    }
    val level = revision.level ?: return "Sconosciuta · Non so / non l'ho provata"
    val label = levelLabel(level)
    return when (revision.verification) {
        V3AssessmentVerification.DECLARED -> "$label · Autodichiarata"
        V3AssessmentVerification.OBSERVED_VALID -> "$label · Osservata"
        V3AssessmentVerification.DERIVED_VALID -> "$label · Derivata"
        V3AssessmentVerification.UNKNOWN -> "Sconosciuta · Non so / non l'ho provata"
        V3AssessmentVerification.INVALIDATED -> "Da rivalutare: valutazione precedente invalidata"
    }
}

@Composable
internal fun V3AthleteAssessmentScreen(store: AppStore, onClose: () -> Unit, onChanged: () -> Unit) {
    var snapshot by remember { mutableStateOf(store.loadV3AssessmentSnapshot(store.loadAthleteRevision())) }
    val nodes = remember { SkillCatalog.nodes }
    val families = remember { SkillCatalog.skills().sortedBy { it.lowercase() } }
    val initialSkillId = snapshot.currentRows().lastOrNull {
        it.target.dimension == V3AssessmentDimension.SKILL
    }?.target?.targetId.orEmpty()
    var selectedSkill by remember { mutableStateOf(initialSkillId) }
    var selectedFamily by remember {
        mutableStateOf(nodes.firstOrNull { it.id == initialSkillId }?.skill.orEmpty())
    }
    var page by remember { mutableIntStateOf(0) }
    var completed by remember { mutableStateOf(false) }
    var skillSkipped by remember { mutableStateOf(false) }
    var familyMenu by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val dimension = guidedAssessmentDimensions[page]
    val target = if (dimension == V3AssessmentDimension.SKILL && selectedSkill.isNotBlank())
        V3AssessmentTarget.skill(selectedSkill)
    else if (dimension == V3AssessmentDimension.SKILL) null
    else V3AssessmentTarget(dimension)
    val current = target?.let(snapshot::current)
    var answer by remember(page, selectedSkill) { mutableStateOf(current?.level) }
    val selectedNode = nodes.firstOrNull { it.id == selectedSkill }
    val familyNodes = if (selectedFamily.isBlank()) emptyList() else SkillCatalog.forSkill(selectedFamily)

    fun saveAnswer(level: V3AssessmentLevel?) {
        val activeTarget = target ?: return
        val saved = store.saveV3Assessment(activeTarget, level,
            assessedAtEpochMs = if (level == null) null else System.currentTimeMillis())
        if (saved) {
            snapshot = store.loadV3AssessmentSnapshot(store.loadAthleteRevision())
            onChanged()
            message = "Risposta salvata come dichiarata."
            if (activeTarget.dimension == V3AssessmentDimension.SKILL) skillSkipped = false
            if (page < guidedAssessmentDimensions.lastIndex) page++ else completed = true
        } else message = "Salvataggio non riuscito: i dati locali richiedono verifica."
    }

    if (completed) {
        V3AssessmentCompletionScreen(
            snapshot = snapshot,
            nodes = nodes,
            skillSkipped = skillSkipped,
            onFinish = onClose,
            onModify = {
                completed = false
                page = 0
                message = null
            }
        )
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Valutazione capacità V3", style = MaterialTheme.typography.headlineSmall)
                Text("Stime separate, aggiornabili e facoltative", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClose) { Text("Chiudi") }
        }
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(progress = { (page + 1f) / guidedAssessmentDimensions.size }, modifier = Modifier.fillMaxWidth())
        Text("${page + 1} di ${guidedAssessmentDimensions.size}", modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(dimensionTitle(dimension), style = MaterialTheme.typography.titleLarge)
                Text("Quale variante controlli oggi senza guida passo per passo?", modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Scegli il livello più alto per cui riconosci con coerenza i criteri della prova. " +
                        "Nessun numero di ripetizioni o carico universale è richiesto.",
                    modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (dimension == V3AssessmentDimension.SKILL) {
                    Spacer(Modifier.height(12.dp))
                    Text("Passaggio 1 · scegli una famiglia", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = { familyMenu = true },
                        modifier = Modifier.fillMaxWidth().testTag("v3-skill-family-selector")
                    ) { Text(selectedFamily.ifBlank { "Scegli una famiglia skill" }) }
                    DropdownMenu(expanded = familyMenu, onDismissRequest = { familyMenu = false }) {
                        families.forEach { family ->
                            DropdownMenuItem(
                                text = { Text(family) },
                                onClick = {
                                    selectedFamily = family
                                    selectedSkill = ""
                                    familyMenu = false
                                    message = null
                                },
                                modifier = Modifier.testTag("v3-skill-family-option")
                            )
                        }
                    }

                    if (selectedFamily.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Passaggio 2 · scegli una progressione", style = MaterialTheme.typography.titleMedium)
                        familyNodes.forEachIndexed { index, node ->
                            OutlinedButton(
                                onClick = {
                                    selectedSkill = node.id
                                    answer = snapshot.current(V3AssessmentTarget.skill(node.id))?.level
                                    message = null
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                    .testTag("v3-skill-progression-option-$index")
                            ) { Text(node.exerciseName) }
                            if (selectedSkill == node.id) {
                                val prerequisiteNames = node.prerequisites.mapNotNull { prerequisiteId ->
                                    nodes.firstOrNull { it.id == prerequisiteId }?.exerciseName
                                }
                                Text(
                                    "Descrizione tecnica dettagliata non presente nel catalogo per questa progressione.",
                                    modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Controllo da considerare: setup, forma, transizione e uscita; indica mentalmente l'assistenza usata. " +
                                        "Questa è un'autovalutazione, non una prova osservata.",
                                    modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    when {
                                        prerequisiteNames.isNotEmpty() -> "Prerequisiti indicati nel catalogo: ${prerequisiteNames.joinToString()}."
                                        node.prerequisites.isNotEmpty() -> "Alcuni prerequisiti del catalogo non hanno una descrizione leggibile."
                                        else -> "Il catalogo non riporta prerequisiti per questa progressione."
                                    },
                                    modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("La valutazione riguarda questa progressione e non si trasferisce ad altre skill.",
                                    modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(
                            "La scelta identifica la progressione; non mostra né modifica l'ID tecnico usato internamente.",
                            modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Le famiglie disponibili provengono dal catalogo skill dell'app.",
                            modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (dimension != V3AssessmentDimension.SKILL || selectedNode != null) {
                    Spacer(Modifier.height(10.dp))
                    V3AssessmentLevel.entries.forEachIndexed { index, level ->
                        val selected = answer == level
                        val copy = levelCopy(dimension, index, selectedNode)
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(selected = selected, onClick = { answer = level },
                                modifier = Modifier.testTag("v3-assessment-level-${level.name}"))
                            Column(Modifier.padding(top = 12.dp)) {
                                Text(italianLevelLabels[index], style = MaterialTheme.typography.titleSmall)
                                Text(copy.anchor, style = MaterialTheme.typography.bodySmall)
                                Text("Esempio: ${copy.example}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    TextButton(
                        onClick = { saveAnswer(null) },
                        enabled = target != null,
                        modifier = Modifier.testTag("v3-assessment-unknown-option")
                    ) { Text("Non so / non l'ho provata · salva come sconosciuta") }
                    Text("Resta sconosciuta; non equivale al livello più basso.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Scegli una progressione prima di dichiararne il livello o indicare «Non so».",
                        modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall)
                }
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
            Button(
                enabled = answer != null && target != null,
                onClick = { saveAnswer(answer) },
                modifier = Modifier.testTag("v3-assessment-save")
            ) {
                Text(if (page < guidedAssessmentDimensions.lastIndex) "Salva e continua" else "Salva e vedi riepilogo")
            }
        }
        if (dimension != V3AssessmentDimension.SKILL) {
            TextButton(
                onClick = {
                    if (page < guidedAssessmentDimensions.lastIndex) page++ else completed = true
                    message = null
                },
                modifier = Modifier.fillMaxWidth().testTag("v3-assessment-skip-dimension")
            ) { Text("Lascia non valutata e continua") }
        }
        if (dimension == V3AssessmentDimension.SKILL) {
            OutlinedButton(
                onClick = {
                    skillSkipped = true
                    completed = true
                    message = null
                },
                modifier = Modifier.fillMaxWidth().testTag("v3-assessment-skip-skill")
            ) { Text("Salta per ora") }
        }
        Spacer(Modifier.height(20.dp))
        Text("Valutazioni attuali", style = MaterialTheme.typography.titleMedium)
        snapshot.currentRows().sortedBy { it.target.dimension.ordinal }.forEach { row ->
            val title = if (row.target.dimension == V3AssessmentDimension.SKILL) {
                val node = nodes.firstOrNull { it.id == row.target.targetId }
                node?.let { "${it.skill} · ${it.exerciseName}" } ?: "Skill non presente nel catalogo corrente"
            } else dimensionTitle(row.target.dimension)
            Text("$title: ${assessmentStatus(row)}",
                modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall)
            if (snapshot.hasContradictoryHistory(row.target)) {
                Text("Lo storico suggerisce una rivalutazione; il livello corrente non viene modificato automaticamente.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

}

@Composable
private fun V3AssessmentCompletionScreen(
    snapshot: com.hybridfitnesscoach.app.domain.v3.V3AssessmentSnapshot,
    nodes: List<SkillNode>,
    skillSkipped: Boolean,
    onFinish: () -> Unit,
    onModify: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).navigationBarsPadding()) {
        Text("Valutazione completata", style = MaterialTheme.typography.headlineSmall)
        Text("Riepilogo delle risposte salvate", modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))

        val dimensions = guidedAssessmentDimensions.filter { it != V3AssessmentDimension.SKILL }
        val declared = dimensions.mapNotNull { dimension ->
            snapshot.current(V3AssessmentTarget(dimension))?.takeIf {
                it.level != null && it.verification != V3AssessmentVerification.INVALIDATED
            }?.let { dimensionTitle(dimension) to it }
        }
        val unknownOrUnevaluated = dimensions.map { dimension ->
            dimensionTitle(dimension) to snapshot.current(V3AssessmentTarget(dimension))
        }.filter { (_, revision) ->
            revision == null || revision.level == null || revision.verification == V3AssessmentVerification.INVALIDATED
        }

        Text("Capacità e pattern con un livello presente", style = MaterialTheme.typography.titleMedium)
        if (declared.isEmpty()) Text("Nessuna risposta con livello salvata.")
        declared.forEach { (title, revision) ->
            Text("$title: ${assessmentStatus(revision)}", modifier = Modifier.padding(top = 5.dp))
        }

        Spacer(Modifier.height(12.dp))
        Text("Sconosciute o non valutate", style = MaterialTheme.typography.titleMedium)
        if (unknownOrUnevaluated.isEmpty()) Text("Nessuna.")
        unknownOrUnevaluated.forEach { (title, revision) ->
            Text("$title: ${assessmentStatus(revision)}", modifier = Modifier.padding(top = 5.dp))
        }

        Spacer(Modifier.height(12.dp))
        Text("Skill valutate", style = MaterialTheme.typography.titleMedium)
        val skillRows = snapshot.currentRows().filter { it.target.dimension == V3AssessmentDimension.SKILL }
        if (skillRows.isEmpty()) {
            Text(if (skillSkipped) "Nessuna skill valutata: la sezione è stata saltata per ora."
                else "Nessuna skill specifica valutata.")
        } else {
            skillRows.forEach { revision ->
                val node = nodes.firstOrNull { it.id == revision.target.targetId }
                val title = node?.let { "${it.skill} · ${it.exerciseName}" }
                    ?: "Skill non presente nel catalogo corrente"
                Text("$title: ${assessmentStatus(revision)}", modifier = Modifier.padding(top = 5.dp))
            }
            if (skillSkipped) Text("Hai saltato per ora nuove risposte skill; quelle già salvate sono state conservate.",
                modifier = Modifier.padding(top = 6.dp))
        }

        Spacer(Modifier.height(12.dp))
        Text("Le risposte sono autodichiarazioni basate sugli esempi mostrati; non sono misurazioni oggettive né certificazioni. " +
            "«Non so» è una valutazione sconosciuta; «Non valutata» significa che non è stata salvata alcuna risposta. " +
            "Saltare la sezione skill non crea una valutazione e non assegna un livello. Una skill non determina le capacità generali.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(18.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().testTag("v3-assessment-finish")) {
            Text("Termina e torna al profilo")
        }
        TextButton(onClick = onModify, modifier = Modifier.fillMaxWidth().testTag("v3-assessment-modify")) {
            Text("Modifica valutazioni")
        }
    }
}
