package com.xslczx.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.lifecycleScope
import com.xslczx.app.databinding.ActivitySplashBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("CustomSplashScreen")
class SplashActivity : androidx.activity.ComponentActivity() {

    private lateinit var splashScreen: SplashScreen
    private lateinit var binding: ActivitySplashBinding
    private var mKeepOnAtomicBool = AtomicBoolean(true)
    private val defaultExitDuration = 300L
    private var countDownJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val onExit = {
                splashScreenViewProvider.remove()
            }
            showSplashExitAnimator(splashScreenViewProvider)
            showSplashIconExitAnimator(splashScreenViewProvider, onExit)
        }
        splashScreen.setKeepOnScreenCondition { mKeepOnAtomicBool.get() }

        binding = ActivitySplashBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            mKeepOnAtomicBool.compareAndSet(true, false)
            withContext(Dispatchers.Main) {
                start()
            }
        }
    }

    private fun start() {
        val count = 30
        val interval = 100L
        countDownJob = flow {
            for (i in count downTo 0) {
                emit(i)
                delay(interval)
            }
        }.flowOn(Dispatchers.Main)
            .onStart { }
            .onCompletion {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            }
            .onEach {
                Log.d(">>>:splash", "onTick:$it")
                binding.progressHorizontal.progress = (count - it) * 100 / count
            }
            .launchIn(lifecycleScope)
    }

    private fun showSplashExitAnimator(provider: SplashScreenViewProvider) {
        val splashScreenView = provider.view
        Log.d(
            "Splash", "showSplashExitAnimator() splashScreenView:$splashScreenView" +
                    " context:${splashScreenView.context}" +
                    " parent:${splashScreenView.parent}"
        )
        val alphaOut = ObjectAnimator.ofFloat(
            splashScreenView,
            View.ALPHA,
            1f,
            0f
        )
        AnimatorSet().run {
            duration = getRemainingDuration(provider)
            Log.d("Splash", "showSplashExitAnimator() duration:$duration")
            playTogether(alphaOut)
            doOnEnd {
                Log.d("Splash", "showSplashExitAnimator() onEnd")
            }
            start()
        }
    }

    private fun showSplashIconExitAnimator(
        provider: SplashScreenViewProvider,
        onExit: () -> Unit = {}
    ) {
        val iconView = provider.iconView
        Log.d(
            "Splash", "showSplashIconExitAnimator()" +
                    " iconView[:${iconView.width}, ${iconView.height}]" +
                    " translation[:${iconView.translationX}, ${iconView.translationY}]"
        )
        val alphaOut = ObjectAnimator.ofFloat(
            iconView,
            View.ALPHA,
            1f,
            0f
        )
        val scaleOut = ObjectAnimator.ofFloat(
            iconView,
            View.SCALE_X,
            View.SCALE_Y,
            Path().apply {
                moveTo(1.0f, 1.0f)
                lineTo(0.3f, 0.3f)
            }
        )
        val slideUp = ObjectAnimator.ofFloat(
            iconView,
            View.TRANSLATION_Y,
            0f,
            -(iconView.height).toFloat() * 2.25f
        ).apply {
            addUpdateListener {
                Log.d(
                    "Splash",
                    "showSplashIconExitAnimator() translationY:${iconView.translationY}"
                )
            }
        }
        AnimatorSet().run {
            duration = getRemainingDuration(provider)
            Log.d("Splash", "showSplashIconExitAnimator() duration:$duration")
            playTogether(alphaOut, scaleOut, slideUp)
            doOnEnd {
                Log.d("Splash", "showSplashIconExitAnimator() onEnd remove")
                onExit()
            }
            start()
        }
    }

    private fun getRemainingDuration(splashScreenView: SplashScreenViewProvider): Long {
        val animationDuration = splashScreenView.iconAnimationDurationMillis
        val animationStart = splashScreenView.iconAnimationStartMillis
        return if (animationDuration == 0L || animationStart == 0L)
            defaultExitDuration
        else (animationDuration - SystemClock.uptimeMillis() + animationStart)
            .coerceAtLeast(0L)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownJob?.cancel()
    }
}