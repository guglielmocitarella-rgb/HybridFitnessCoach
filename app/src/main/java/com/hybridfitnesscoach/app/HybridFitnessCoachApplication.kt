package com.hybridfitnesscoach.app

import android.app.Application
import com.hybridfitnesscoach.app.domain.v3.V3AndroidAssetReader
import com.hybridfitnesscoach.app.domain.v3.V3RuntimeComponent

class HybridFitnessCoachApplication : Application() {
    internal val v3RuntimeComponent: V3RuntimeComponent by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        V3RuntimeComponent(assetReader = { V3AndroidAssetReader.load(assets) })
    }
}
