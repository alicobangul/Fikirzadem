package com.basesoftware.fikirzadem.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.basesoftware.fikirzadem.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import com.basesoftware.fikirzadem.databinding.SuggestionDialogBinding
import com.basesoftware.fikirzadem.util.ExtensionUtil.gone
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.message
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.Timestamp
import java.util.*
import java.util.concurrent.TimeUnit

object WorkUtil {

    fun goMyProfile(navController: NavController, myUserId : String) {
        val argument = Bundle()
        argument.putString("userId", myUserId)
        changeFragment(navController, argument, R.id.profileFragment)
    }

    fun changeFragment(navController: NavController, argument : Bundle?, fragment: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            val tag = "Util-ChangeFragment"
            try {

                // Sadece feed & interest fragmentları backstackde tutuluyor onun dışında her şey yeni açılıyor
                try {
                    navController.getBackStackEntry(fragment)

                    when(fragment == R.id.feedFragment || fragment == R.id.interestFragment) {
                        true -> navController.popBackStack(fragment, false)
                        else -> navController.navigate(fragment, argument, NavOptions.Builder().setPopUpTo(fragment, true).build())
                    }

                }
                catch (ex: IllegalArgumentException){ navController.navigate(fragment,argument) }

                Log.i(tag, "Fragment değiştirme başarılı")

            }
            catch (e: Exception) {
                Log.e(tag, "Fragment değiştirme başarısız")
                Log.e(tag, e.msg())
            }

        }
    }

    fun getDeviceHeight(context: Context) : Int {

        val displayMetrics = DisplayMetrics()

        return if(Build.VERSION.SDK_INT >= 30){ (context as Activity).windowManager.currentWindowMetrics.bounds.height() }
        else {
            (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }
    }

    fun glideDefault(context: Context) : RequestOptions {
        return RequestOptions()
            .timeout(5000) // 20 sn gecikme verildi
            .error(AppCompatResources.getDrawable(context, R.drawable.login_user_ico)) // Link hatalı/indirme başarısız ise refresh ico göster
            .onlyRetrieveFromCache(false) // Sadece önbellekten okuma kapatıldı
            .skipMemoryCache(true) // Sadece disk önbelleği kullanılacak böylece bellekten tasarruf edildi.
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // Disk önbelleği otomatik kullanıma alındı.
    }

    fun systemLanguage() : String {

        return when (Locale.getDefault().language.matches(Regex("tr"))) {
            true -> "tr"
            false -> "en"
        }

    }

    fun snackbarColor(context: Context) : Int = ContextCompat.getColor(context, R.color.snackbar)



    fun openShareMenu(context: Context, shareText : String) {

        try {

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                type = "text/plain"
            }

            context.startActivity(Intent.createChooser(sendIntent,null))

            Log.i("WorkUtil-openShareMenu", "Paylaş ekranı açıldı")

        } catch (e: ActivityNotFoundException) {
            Log.e("WorkUtil-openShareMenu", "Paylaş ekranı açılamadı")
            Log.e("WorkUtil-openShareMenu", e.msg())
        }

    }



    fun visitPlayStoreAppPage(context : Context) {

        if((context as AppCompatActivity).lifecycle.currentState == Lifecycle.State.RESUMED) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.playstore_app_link)))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            catch (anfe : ActivityNotFoundException) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.playstore_developer_link)))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }

    }



    @SuppressLint("LogConditional")
    fun suggestion(context: Context, sharedViewModel: SharedViewModel) {

        /**
         * [100 / 1 ORAN -> 100 Ekran değiştirmede 1] gerçekleştirilecek aksiyon:
         * - Kullanıcıya; inAppReview ile yorum yapma veya uygulamayı paylaşma dialog kutusu gösterilecek.
         * - Yorum veya paylaşma önerisi dialog kutularının gösterimi için min. 1 saat zaman aralığı gereklidir.
         * - [100 / 1] oran sonrasında [2 / 1] oran ile yorum veya paylaşma önerisi dialog kutusundan birisi seçiliyor.
         * - Seçilen dialog kutusu gösterime uygun değil ise ( 1 saat önce gösterildi ise [App'in açık olması şart değil] )
         * - Diğer dialog kutusunun uygunluğu kontrol ediliyor.
         */

        if(sharedViewModel.getSuggestionDialog()) {

            if(Random().nextInt(100) == 0) {

                context.getSharedPreferences(context.packageName.toString(), Context.MODE_PRIVATE).apply {

                    val now = Timestamp.now().seconds

                    val default = Timestamp.now().seconds - 3600 // Şuanki zamandan 1 saat öncesi
                    

                    val commentTime = TimeUnit.SECONDS.toHours(now - getLong("_commentSuggestion", default)).toInt() >= 1

                    val shareTime = TimeUnit.SECONDS.toHours(now - getLong("_shareSuggestion", default)).toInt() >= 1


                    when(Random().nextInt(2)) {

                        0 -> if(commentTime) commentSuggestion(context, sharedViewModel)
                        else if(shareTime) shareSuggestion(context, sharedViewModel)

                        1 -> if(shareTime) shareSuggestion(context, sharedViewModel)
                        else if(commentTime) commentSuggestion(context, sharedViewModel)

                    }

                }

            }

        }

    }

    @SuppressLint("LogConditional")
    fun commentSuggestion(context: Context, sharedViewModel: SharedViewModel) {

        Dialog(context).apply {

            val binding = SuggestionDialogBinding
                .inflate(LayoutInflater.from(context.applicationContext), null, false)

            binding.commentSuggestion = true

            setContentView(binding.root)

            window?.let {

                it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                it.attributes?.windowAnimations = R.style.likeDialog

            }

            setCancelable(false)

            binding.btnSuggestionYes.setOnClickListener {

                dismiss()

                if((context as AppCompatActivity).lifecycle.currentState == Lifecycle.State.RESUMED) {

                    /**
                     * - Test için fake inAppReview
                     * - val reviewManager = FakeReviewManager(context)
                     */

                    val reviewManager = ReviewManagerFactory.create(context)

                    reviewManager.requestReviewFlow()
                        .addOnCompleteListener { task ->

                            when(task.isSuccessful) {

                                true -> {

                                    val reviewInfo = task.result

                                    reviewManager.launchReviewFlow(context, reviewInfo)
                                        .addOnCompleteListener {

                                            when(it.isSuccessful) {
                                                true -> Log.i(
                                                    "Util-commentSuggestion",
                                                    "In-App Review Başarılı - launchReviewFlow"
                                                )
                                                else -> {
                                                    visitPlayStoreAppPage(context)
                                                    Log.e(
                                                        "Util-commentSuggestion",
                                                        "In-App Review Başarısız - launchReviewFlow"
                                                    )
                                                }
                                            }

                                        }

                                }

                                else -> {
                                    visitPlayStoreAppPage(context)
                                    Log.e("Util-commentSuggestion", "In-App Review Başarısız - requestReviewFlow")
                                }

                            }

                        }

                }

            }

            binding.btnSuggestionLater.setOnClickListener { dismiss().also { sharedViewModel.setSuggestionDialog(true) } }

            show().also {

                if((context as AppCompatActivity).lifecycle.currentState == Lifecycle.State.RESUMED) {
                    context.getSharedPreferences(context.packageName.toString(), Context.MODE_PRIVATE).edit {
                        putLong("_commentSuggestion", Timestamp.now().seconds)
                        apply()
                    }
                }

                sharedViewModel.setSuggestionDialog(false)

            }

        }

    }

    @SuppressLint("LogConditional")
    fun shareSuggestion(context: Context, sharedViewModel: SharedViewModel) {

        Dialog(context).apply {

            val binding = SuggestionDialogBinding
                .inflate(LayoutInflater.from(context.applicationContext), null, false)

            binding.commentSuggestion = false

            setContentView(binding.root)

            window?.let {

                it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                it.attributes?.windowAnimations = R.style.likeDialog

            }

            setCancelable(false)

            binding.btnSuggestionYes.setOnClickListener {

                dismiss()

                if((context as AppCompatActivity).lifecycle.currentState == Lifecycle.State.RESUMED) {

                    val text = context.getString(R.string.invitation_message)
                    val link = context.getString(R.string.playstore_app_link)

                    val shareText = "$text\n$link"

                    openShareMenu(context, shareText)

                }

            }

            binding.btnSuggestionLater.setOnClickListener { dismiss().also { sharedViewModel.setSuggestionDialog(true) } }

            show().also {

                if((context as AppCompatActivity).lifecycle.currentState == Lifecycle.State.RESUMED) {
                    context.getSharedPreferences(context.packageName.toString(), Context.MODE_PRIVATE).edit {
                        putLong("_shareSuggestion", Timestamp.now().seconds)
                        apply()
                    }
                }

                sharedViewModel.setSuggestionDialog(false)

            }

        }

    }



    fun resDrawable(context : Context, value : Int) : Drawable = ResourcesCompat.getDrawable(context.resources, value,null)!!


    fun firestore() : FirebaseFirestore {

        val firestore = FirebaseFirestore.getInstance()

        val settings = FirebaseFirestoreSettings
            .Builder()
            .setCacheSizeBytes(26214400)
            .setPersistenceEnabled(true)
            .build()

        firestore.firestoreSettings = settings

        return firestore
    }



    suspend fun calculateDate(context: Context, time: Long, from: String): String = CoroutineScope(Dispatchers.Default).async {
        val nowSeconds = Timestamp.now().seconds
        val calculateWithSecond = nowSeconds - time

        val timeText = when {
            (nowSeconds < time && nowSeconds - time >= -5) || nowSeconds == time -> {
                Log.i("$from-DateText", "Cihaz tarihi yanlış ${(nowSeconds - time)} ${context.getString(R.string.time_second)}")
                context.getString(R.string.time_now)
            }
            calculateWithSecond > TimeUnit.DAYS.toSeconds(365) -> "${TimeUnit.SECONDS.toDays(calculateWithSecond) / 365} ${context.getString(R.string.time_year)}"
            calculateWithSecond > TimeUnit.DAYS.toSeconds(30) -> "${TimeUnit.SECONDS.toDays(calculateWithSecond) / 30} ${context.getString(R.string.time_month)}"
            calculateWithSecond > TimeUnit.DAYS.toSeconds(7) -> "${TimeUnit.SECONDS.toDays(calculateWithSecond) / 7} ${context.getString(R.string.time_week)}"
            calculateWithSecond >= TimeUnit.DAYS.toSeconds(1) -> "${TimeUnit.SECONDS.toDays(calculateWithSecond)} ${context.getString(R.string.time_day)}"
            calculateWithSecond >= TimeUnit.HOURS.toSeconds(1) -> "${TimeUnit.SECONDS.toHours(calculateWithSecond)} ${context.getString(R.string.time_hour)}"
            calculateWithSecond >= TimeUnit.MINUTES.toSeconds(1) -> "${TimeUnit.SECONDS.toMinutes(calculateWithSecond)} ${context.getString(R.string.time_minute)}"
            else -> context.getString(R.string.time_now)
        }

        return@async timeText
    }.await()


}