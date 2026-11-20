package com.moneytrack.app

import android.app.Application
import com.moneytrack.app.auth.AuthBackend

class MoneyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthBackend.init(this)
    }
}
