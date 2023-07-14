package com.cafedroid.bingo_android

import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.cafedroid.bingo_android.databinding.ActivityNameBinding

class NameActivity : BaseActivity() {
    companion object {
        const val USER_KEY = "user"
    }

    private lateinit var binding: ActivityNameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        binding.btnProceed.setOnClickListener {
            if (binding.etUsername.text?.isNotBlank() == true) {
                binding.lottieProceed.playAnimation()
                binding.lottieProceed.addAnimatorListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}

                    override fun onAnimationEnd(animation: Animator) {
                        USERNAME = binding.etUsername.text.toString().trim()
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(USER_KEY, USERNAME)
                        )
                        finish()
                    }

                    override fun onAnimationCancel(animation: Animator) {}

                    override fun onAnimationRepeat(animation: Animator) {}

                })
            } else showToast("The name's required buddy!")
        }
    }
}
