package com.hojang.gooddriver

import android.app.Application
import android.content.Context

class MyApplication : Application() {

    companion object {
        private var instance: MyApplication? = null

        fun applicationContext(): Context {
            return instance?.applicationContext ?: throw IllegalStateException("MyApplication instance is null")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
