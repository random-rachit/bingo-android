package com.cafedroid.bingo_android

import android.app.Application

class BingoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        BingoSocket.connect()
//        AndroidNetworking.initialize(this)
//        AndroidNetworking.download(
//            "https://pocketbingo-deployments-mobilehub-1135494544.s3.ap-south-1.amazonaws.com/urls.json",
//            cacheDir.absolutePath,
//            "urls.json"
//        )
//            .build()
//            .startDownload(object : DownloadListener {
//                override fun onDownloadComplete() {
//                    val urlFile = File(cacheDir, "urls.json")
//                    val inputStream = urlFile.inputStream()
//                    var jsonStr = "null"
//                    try {
//                        val fc: FileChannel = inputStream.channel
//                        val bb: MappedByteBuffer =
//                            fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
//                        jsonStr = Charset.defaultCharset().decode(bb).toString()
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    } finally {
//                        inputStream.close()
//                    }
//                }
//
//                override fun onError(anError: ANError?) {
//                    anError?.printStackTrace()
//                    Toast.makeText(
//                        this@BingoApplication,
//                        anError?.errorDetail.toString(),
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//
//            })
    }
}