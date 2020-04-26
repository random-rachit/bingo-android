package com.cafedroid.bingo_android

import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_name.*

class NameActivity : BaseActivity() {

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
            if (et_username.text?.isNotBlank() == true) {
                lottie_proceed.playAnimation()
                lottie_proceed.addAnimatorListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {}

                    override fun onAnimationEnd(animation: Animator?) {
                        USERNAME = et_username.text.toString().trim()
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(USER_KEY, USERNAME)
                        )
                        finish()
                    }

                    override fun onAnimationCancel(animation: Animator?) {}

                    override fun onAnimationStart(animation: Animator?) {}

                })
            } else showToast("Name's required buddy!")
        }
    }
}
