package com.cafedroid.bingo_android

import android.app.Application

class BingoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        BingoSocket.connect()
    }
}