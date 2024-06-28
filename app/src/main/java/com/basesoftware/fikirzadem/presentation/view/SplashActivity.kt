package com.basesoftware.fikirzadem.presentation.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.Animation
import androidx.activity.viewModels
import com.basesoftware.fikirzadem.databinding.ActivitySplashBinding
import com.basesoftware.fikirzadem.util.ExtensionUtil.fullScreen
import com.basesoftware.fikirzadem.presentation.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private var _binding : ActivitySplashBinding? = null
    private val binding get() = _binding!!

    private val viewModel : SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.fullScreen() // Statusbar ve navigation gizlenerek tam ekrana alındı

        binding.animStart = true // Animasyonu başlatacak databinding değişkeni çağırıldı

        observeViewModel() // ViewModel gözlemleniyor

        observeAnim() // Animasyon bitişine göre Login sayfasına geçiş yapıyor

    }


    override fun onDestroy() {

        _binding = null

        super.onDestroy()
    }

    private fun observeViewModel() {

        viewModel.navigateToLogin.observe(this) { isAnimationComplete ->  if (isAnimationComplete) goLogin() }

    }

    private fun observeAnim() {

        binding.txtCharF.apply {

            postOnAnimation {

                animation.setAnimationListener(object : Animation.AnimationListener {

                    override fun onAnimationEnd(animation: Animation?) { viewModel.onAnimationEnd() }

                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationRepeat(animation: Animation?) {}

                })

            }

        }

    }



    private fun goLogin() {

        startActivity(Intent(this@SplashActivity, AppActivity::class.java)) // Logine git

        finish() // SplashActivity öldür

    }


}