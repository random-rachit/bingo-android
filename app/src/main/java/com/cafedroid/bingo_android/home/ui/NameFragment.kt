package com.cafedroid.bingo_android.home.ui

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cafedroid.bingo_android.databinding.FragmentNameBinding
import com.cafedroid.bingo_android.showToast
import com.cafedroid.bingo_android.utils.NAME_KEY

class NameFragment : Fragment() {

    private lateinit var binding: FragmentNameBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.btnProceed.setOnClickListener {
            if (binding.etUsername.text?.isNotBlank() == true) {
                binding.lottieProceed.playAnimation()
                binding.lottieProceed.addAnimatorListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}

                    override fun onAnimationEnd(animation: Animator) {
                        findNavController().run {
                            previousBackStackEntry?.savedStateHandle?.set(
                                NAME_KEY,
                                binding.etUsername.text.toString().trim()
                            )
                            popBackStack()
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {}

                    override fun onAnimationRepeat(animation: Animator) {}

                })
            } else requireContext().showToast("The name's required buddy!")
        }
    }
}