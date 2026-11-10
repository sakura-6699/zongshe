package com.example.loan

import android.app.Application
import com.example.loan.data.local.db.DemoUserSeeder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LoanApplication : Application() {

    @Inject
    lateinit var demoUserSeeder: DemoUserSeeder

    override fun onCreate() {
        super.onCreate()
        demoUserSeeder.seedIfNeeded()
    }
}
