package com.cafedroid.bingo_android

import android.app.Application
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.DownloadListener
import org.json.JSONObject
import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset

class BingoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        BingoSocket.connect()
        AndroidNetworking.initialize(this)
        AndroidNetworking.download(
            "https://pocketbingo-deployments-mobilehub-1135494544.s3.ap-south-1.amazonaws.com/urls.json",
            cacheDir.absolutePath,
            "urls.json"
        )
            .build()
            .startDownload(object : DownloadListener {
                override fun onDownloadComplete() {
                    val urlFile = File(cacheDir, "urls.json")
                    val inputStream = urlFile.inputStream()
                    var jsonStr = "null"
                    try {
                        val fc: FileChannel = inputStream.channel
                        val bb: MappedByteBuffer =
                            fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
                        jsonStr = Charset.defaultCharset().decode(bb).toString()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        inputStream.close()
                    }
                    JSONObject(jsonStr).apply {
                        BASE_URL = getString("base_url")
                    }
                }

                override fun onError(anError: ANError?) {
                    TODO("Not yet implemented")
                }

            })
    }
}