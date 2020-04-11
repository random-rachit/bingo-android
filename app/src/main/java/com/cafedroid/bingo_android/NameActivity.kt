package com.cafedroid.bingo_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_name.*

class NameActivity : AppCompatActivity() {

    companion object {
        const val USER_KEY = "user"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_name)

        initView()
    }

    private fun initView() {
        btn_proceed.setOnClickListener {
            if (et_username.text.isNotBlank()) {
                USERNAME = et_username.text.toString()
                setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(USER_KEY, USERNAME)
                )
                finish()
            } else Toast.makeText(applicationContext, "Name's required buddy!", Toast.LENGTH_SHORT).show()
        }
    }
}
