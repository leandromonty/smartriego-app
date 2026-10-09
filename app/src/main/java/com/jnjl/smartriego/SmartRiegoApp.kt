package com.jnjl.smartriego

import android.app.Application

class SmartRiegoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.sesion = SessionManager(this)
    }
}