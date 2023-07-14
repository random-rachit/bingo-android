package com.cafedroid.bingo_android

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity: AppCompatActivity() {

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//        toast.view = layoutInflater.inflate(R.layout.custom_toast_layout, ll_custom_toast)
//        toast.view.tv_custom_toast.text = message
//        toast.show()
    }
}