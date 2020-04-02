package com.cafedroid.bingo_android

import android.app.Application
import com.androidnetworking.AndroidNetworking

class BingoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AndroidNetworking.initialize(this)
        BingoSocket.connect()
    }
}