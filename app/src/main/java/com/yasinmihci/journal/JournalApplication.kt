package com.yasinmihci.journal

import android.app.Application
import com.yasinmihci.journal.di.AppContainer

class JournalApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
