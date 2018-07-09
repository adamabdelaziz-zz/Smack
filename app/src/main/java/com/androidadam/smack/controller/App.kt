package com.androidadam.smack.controller

import android.app.Application
import com.androidadam.smack.utilities.SharedPrefs

class App : Application() {

    companion object {
        lateinit var  sharedPreferences : SharedPrefs

    }

    override fun onCreate() {
        sharedPreferences = SharedPrefs(applicationContext)
        super.onCreate()
    }

}