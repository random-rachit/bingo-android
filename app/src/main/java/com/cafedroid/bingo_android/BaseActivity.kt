package com.cafedroid.bingo_android

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.custom_toast_layout.*
import kotlinx.android.synthetic.main.custom_toast_layout.view.*

open class BaseActivity: AppCompatActivity() {

    fun showToast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.view = layoutInflater.inflate(R.layout.custom_toast_layout, ll_custom_toast)
        toast.view.tv_custom_toast.text = message
        toast.show()
    }
}