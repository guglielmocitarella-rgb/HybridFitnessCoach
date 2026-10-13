package com.hybridfitnesscoach.app

/** Conservative coaching heuristics, not a physiological measurement or diagnosis.
 * HR/HRV require a personal baseline. Missing values never become synthetic readings.
 */
object DefaultHealthNormalizer:HealthDataNormalizer {
    override fun normalize(raw:RawHealthSnapshot):NormalizedHealthState {
        val sleep=raw.sleepMinutes?.takeIf{it in 1..1440}?.let{(it/480.0*100).coerceIn(0.0,100.0)}
        fun valid(x:Double?,range:ClosedFloatingPointRange<Double>)=x?.takeIf{it.isFinite()&&it in range}
        val hr=valid(raw.restingHeartRateBpm,30.0..220.0)
        val baselineHr=valid(raw.baselineRestingHeartRateBpm,30.0..220.0)
        val hrv=valid(raw.hrvMs,1.0..300.0)
        val baselineHrv=valid(raw.baselineHrvMs,1.0..300.0)
        val scores=listOfNotNull(
            if(hr!=null&&baselineHr!=null)(100-(hr-baselineHr).coerceAtLeast(0.0)*4).coerceIn(0.0,100.0) else null,
            if(hrv!=null&&baselineHrv!=null)(100*hrv/baselineHrv).coerceIn(0.0,100.0) else null)
        val activity=raw.activeMinutes?.takeIf{it>=0}?.let{(100-it*0.5).coerceIn(20.0,100.0)}
        return NormalizedHealthState(raw.capturedAtEpochMs,sleep,scores.takeIf{it.isNotEmpty()}?.average(),activity)
    }
}

class DefaultReadinessProvider(private val now:Long=System.currentTimeMillis()):ReadinessProvider {
    override fun readiness(input:AthleteReadinessInput):Double? {
        val health=input.health?.takeIf{now-it.capturedAtEpochMs in 0..36L*3_600_000}
        val signals=listOfNotNull(
            input.recoveryByMuscle.values.minOrNull()?.toDouble(),
            health?.sleepScore,health?.cardiovascularRecoveryScore,health?.activityLoadScore,
            input.recentWorkoutRpe.filter{it in 1..10}.takeIf{it.isNotEmpty()}?.average()?.let{(110-it*7).coerceIn(20.0,100.0)}
        ).filter{it.isFinite() && it in 0.0..100.0}
        // Do not average away a severely low sleep or recovery signal.
        return signals.minOrNull()
    }
}

/** Union interval durations from multiple data sources, to avoid double counting. */
internal fun mergedIntervalMinutes(intervals:List<Pair<Long,Long>>):Int {
    val sorted=intervals.filter{it.second>it.first}.sortedBy{it.first}
    if(sorted.isEmpty())return 0
    var start=sorted.first().first;var end=sorted.first().second;var duration=0L
    sorted.drop(1).forEach{(a,b)->if(a<=end)end=maxOf(end,b) else {duration+=end-start;start=a;end=b}}
    return ((duration+end-start)/60_000).toInt()
}
