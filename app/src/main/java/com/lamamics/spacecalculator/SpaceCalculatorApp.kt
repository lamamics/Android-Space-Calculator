package com.lamamics.spacecalculator

import android.app.Application
import com.lamamics.spacecalculator.scan.ScanRepository
import com.lamamics.spacecalculator.scan.StorageStatsProvider
import com.lamamics.spacecalculator.scan.VolumeProvider
import com.lamamics.spacecalculator.shizuku.ShizukuManager

/** Hosts the long-lived singletons (no DI framework for now — kept deliberately small). */
class SpaceCalculatorApp : Application() {

    lateinit var shizukuManager: ShizukuManager
        private set
    lateinit var scanRepository: ScanRepository
        private set
    lateinit var statsProvider: StorageStatsProvider
        private set
    lateinit var volumeProvider: VolumeProvider
        private set

    override fun onCreate() {
        super.onCreate()
        shizukuManager = ShizukuManager(this).also { it.register() }
        scanRepository = ScanRepository(this, shizukuManager)
        statsProvider = StorageStatsProvider(this)
        volumeProvider = VolumeProvider(this)
    }

    override fun onTerminate() {
        shizukuManager.unregister()
        super.onTerminate()
    }
}
