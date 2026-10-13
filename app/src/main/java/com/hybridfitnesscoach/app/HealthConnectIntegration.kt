package com.hybridfitnesscoach.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.reflect.KClass

data class HealthConnectState(val readiness:Int?=null,val description:String="Health Connect non collegato",val busy:Boolean=false,val connect:()->Unit={},val refresh:()->Unit={})
val LocalHealthConnect=compositionLocalOf{HealthConnectState()}

private val healthPermissions=setOf(
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(RestingHeartRateRecord::class),
    HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class)
)

private suspend fun <T:Record> readHealthRecords(client:HealthConnectClient,type:KClass<T>,permissions:Set<String>,start:Instant,end:Instant):List<T> {
    if(HealthPermission.getReadPermission(type) !in permissions)return emptyList()
    val result=mutableListOf<T>();var token:String?=null
    do {
        val page=client.readRecords(ReadRecordsRequest(type,TimeRangeFilter.between(start,end),pageSize=1000,pageToken=token))
        result+=page.records;token=page.pageToken
    }while(token!=null)
    return result
}

private suspend fun readHealthSnapshot(context:Context):Pair<RawHealthSnapshot,String> {
    val client=HealthConnectClient.getOrCreate(context)
    val permissions=client.permissionController.getGrantedPermissions()
    val now=Instant.now();val day=now.minusSeconds(86_400);val baselineStart=now.minusSeconds(14*86_400)
    var failed=0
    suspend fun <T:Record> read(type:KClass<T>,start:Instant):List<T> = try {readHealthRecords(client,type,permissions,start,now)}
        catch(e:CancellationException){throw e} catch(_:Exception){failed++;emptyList()}
    val sleep=read(SleepSessionRecord::class,day)
    val heart=read(RestingHeartRateRecord::class,baselineStart)
    val hrv=read(HeartRateVariabilityRmssdRecord::class,baselineStart)
    val activity=read(ExerciseSessionRecord::class,day)
    fun intervals(start:Instant,end:Instant)=maxOf(start.toEpochMilli(),day.toEpochMilli()) to minOf(end.toEpochMilli(),now.toEpochMilli())
    val sleepIntervals=sleep.flatMap { record ->
        if(record.stages.isEmpty())listOf(intervals(record.startTime,record.endTime))
        else record.stages.filter{it.stage in setOf(SleepSessionRecord.STAGE_TYPE_SLEEPING,SleepSessionRecord.STAGE_TYPE_LIGHT,SleepSessionRecord.STAGE_TYPE_DEEP,SleepSessionRecord.STAGE_TYPE_REM)}.map{intervals(it.startTime,it.endTime)}
    }
    // Equal weight per day prevents devices with more samples dominating the baseline.
    fun baseline(values:List<Pair<Instant,Double>>):Double? {
        val days=values.filter{it.first<day}.groupBy{it.first.epochSecond/86_400}.values.map{xs->xs.map{it.second}.average()}
        return days.takeIf{it.size>=3}?.average()
    }
    val raw=RawHealthSnapshot(now.toEpochMilli(),
        sleepMinutes=if(sleep.isEmpty())null else mergedIntervalMinutes(sleepIntervals),
        restingHeartRateBpm=heart.filter{it.time>=day}.maxByOrNull{it.time}?.beatsPerMinute?.toDouble(),
        hrvMs=hrv.filter{it.time>=day}.maxByOrNull{it.time}?.heartRateVariabilityMillis,
        activeMinutes=if(activity.isEmpty())null else mergedIntervalMinutes(activity.map{intervals(it.startTime,it.endTime)}),
        baselineRestingHeartRateBpm=baseline(heart.map{it.time to it.beatsPerMinute.toDouble()}),
        baselineHrvMs=baseline(hrv.map{it.time to it.heartRateVariabilityMillis}))
    val description=buildList {
        raw.sleepMinutes?.let{add("Sonno: ${it/60}h ${it%60}m")}
        raw.restingHeartRateBpm?.let{add("FC riposo: ${it.toInt()} bpm")}
        raw.hrvMs?.let{add("HRV: ${it.toInt()} ms")}
        raw.activeMinutes?.let{add("Attività: $it min")}
        if((raw.restingHeartRateBpm!=null&&raw.baselineRestingHeartRateBpm==null)||(raw.hrvMs!=null&&raw.baselineHrvMs==null))add("Baseline personale in raccolta")
        if(permissions.intersect(healthPermissions).isEmpty())add("Concedi i permessi per usare i dati disponibili")
        else if(isEmpty())add("Nessun dato recente: uso dello storico workout")
        if(failed>0)add("Alcuni dati non disponibili; puoi riprovare")
    }.joinToString(" • ")
    return raw to description
}

@Composable
fun rememberHealthConnectState():HealthConnectState {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val scope=rememberCoroutineScope()
    var readiness by remember{mutableStateOf<Int?>(null)}
    var description by remember{mutableStateOf("Health Connect non collegato")}
    var busy by remember{mutableStateOf(false)}
    fun refresh() {
        if(busy)return
        scope.launch {
            busy=true;readiness=null
            try {
                when(HealthConnectClient.getSdkStatus(context)) {
                    HealthConnectClient.SDK_AVAILABLE->{
                        val (raw,message)=readHealthSnapshot(context)
                        readiness=DefaultReadinessProvider().readiness(AthleteReadinessInput(emptyMap(),DefaultHealthNormalizer.normalize(raw)))?.toInt()
                        description=message
                    }
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED->description="Installa o aggiorna Health Connect per collegare i dati"
                    else->description="Health Connect non disponibile: il coach usa lo storico workout"
                }
            }catch(e:CancellationException){throw e}catch(_:Exception){description="Connessione non disponibile: il coach usa lo storico workout"}
            finally{busy=false}
        }
    }
    val launcher=rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()){refresh()}
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME)refresh() else if(event==Lifecycle.Event.ON_PAUSE)readiness=null}
        lifecycle.addObserver(observer)
        onDispose{lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(Unit){refresh()}
    return HealthConnectState(readiness,description,busy,connect={
        try {
            when(HealthConnectClient.getSdkStatus(context)){
                HealthConnectClient.SDK_AVAILABLE->launcher.launch(healthPermissions)
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED->context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")))
                else->refresh()
            }
        }catch(_:Exception){description="Impossibile aprire Health Connect su questo dispositivo"}
    },refresh={refresh()})
}

@Composable
fun HealthConnectCard() {
    val health=LocalHealthConnect.current
    Card(Modifier.fillMaxWidth().padding(vertical=8.dp)) { Column(Modifier.padding(16.dp)) {
        Text("Recupero e Health Connect",style=MaterialTheme.typography.titleMedium)
        Text(health.description,style=MaterialTheme.typography.bodySmall)
        Text("Lettura facoltativa, solo sul dispositivo. Con dati incompleti resta attivo il coach basato sullo storico.",style=MaterialTheme.typography.bodySmall)
        Row { TextButton(onClick=health.connect,enabled=!health.busy){Text("COLLEGA / PERMESSI")};TextButton(onClick=health.refresh,enabled=!health.busy){Text(if(health.busy)"LETTURA…" else "AGGIORNA")} }
    }}
}

class HealthConnectPrivacyActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface { Column(Modifier.padding(24.dp)) {
            Text("Dati salute e privacy",style=MaterialTheme.typography.headlineSmall)
            Text("Hybrid legge, con il tuo consenso, sonno, frequenza cardiaca a riposo, HRV e sessioni di attività per adattare il workout. I dati sono elaborati localmente in memoria: non sono caricati su server né condivisi e Hybrid non scrive dati in Health Connect. Puoi revocare ogni permesso dalle impostazioni di Health Connect. Senza permessi l'app continua a usare lo storico degli allenamenti. La readiness è una stima per l'allenamento, non una misura medica.")
            TextButton(onClick={finish()}){Text("CHIUDI")}
        }}}}
    }
}
