package com.basesoftware.fikirzadem.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.edit
import androidx.core.text.toSpannable
import androidx.databinding.BindingAdapter
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.room.Room
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.BottomDialogPostMoreBinding
import com.basesoftware.fikirzadem.model.*
import com.basesoftware.fikirzadem.model.recycler.SearchRecyclerModel
import com.basesoftware.fikirzadem.data.local.FikirzademDatabase
import com.basesoftware.fikirzadem.data.local.dto.SavedPostModel
import com.bumptech.glide.Glide
import com.google.android.gms.ads.*
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object ExtensionUtil {

    fun Window.hideStatusBar() {

        if (Build.VERSION.SDK_INT >= 30) decorView.windowInsetsController!!.hide(WindowInsets.Type.statusBars())

        else setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

    }

    fun Window.fullScreen() {

        if (Build.VERSION.SDK_INT in 30..34) {

            decorView.windowInsetsController?.apply {

                hide(WindowInsets.Type.statusBars())
                hide(WindowInsets.Type.navigationBars())

            }

        }

        else {
            /**
             * 2048 = View.SYSTEM_UI_FLAG_IMMERSIVE
             * 1024 = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
             * 512 = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
             * 4 = View.SYSTEM_UI_FLAG_FULLSCREEN
             * 2 = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
             */

            decorView.systemUiVisibility = (2048 or 1024 or 512 or 4 or 2)
            setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

        }

    }

    fun Throwable.message() : String { return this.localizedMessage ?: "empty message" }

    fun Exception.msg() : String { return this.localizedMessage ?: "empty message" }

    fun ImageView.downloadFromUrl(url : String) {

        Glide
            .with(context)
            .setDefaultRequestOptions(WorkUtil.glideDefault(this.context))
            .load(url) // İndirme linki verildi
            .into(this)

    }

    fun ImageView.downloadFromUri(uri : Uri) {

        Glide
            .with(context)
            .setDefaultRequestOptions(WorkUtil.glideDefault(this.context))
            .load(uri)
            .into(this)

    }

    fun ImageView.getDrawableWithCode(drawableCode : Int) {

        Glide
            .with(context)
            .setDefaultRequestOptions(WorkUtil.glideDefault(this.context))
            .load(AppCompatResources.getDrawable(this.context, drawableCode)!!)
            .into(this)

    }

    fun ImageView.downloadFromDrawable(drawable : Drawable) {

        Glide
            .with(context)
            .setDefaultRequestOptions(WorkUtil.glideDefault(this.context))
            .load(drawable)
            .into(this)

    }


    fun View.show() { visibility = View.VISIBLE }
    fun View.hide() { visibility = View.INVISIBLE }
    fun View.gone() { visibility = View.GONE }

    fun View.itemAnimation(animationPermission : Boolean) {

        if (animationPermission) this.startAnimation(AnimationUtils.loadAnimation(this.context, android.R.anim.slide_in_left))

    }

    fun Int.safePosition() : Int = if(this != -1) this else 0


    fun Exception.firestoreError() : Int {
        return when((this as FirebaseFirestoreException).code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            true -> R.string.report_available
            else -> R.string.report_send_fail
        }
    }


    fun DocumentSnapshot.toUserModel() : UserModel {

        return UserModel(
            getString("userId") ?: "-",
            getString("userMail") ?: "-",
            getString("userName") ?: "-",
            getString("userRealName") ?: "-",
            getString("userBiography") ?: "-",
            getString("userProfilePicture") ?: "-",
            getString("userTwitter") ?: "-",
            getString("userFacebook") ?: "-",
            getString("userInstagram") ?: "-",
            getString("userAdminMessageTr") ?: "Fikirzademe hoşgeldiniz",
            getString("userAdminMessageEn") ?: "Welcome to the Fikirzadem",
            (getLong("userFollowing") ?: 0f).toInt(),
            (getLong("userFollower") ?: 0f).toInt(),
            (getLong("userReport") ?: 0f).toInt(),
            (getLong("userSpamPost") ?: 0f).toInt(),
            (getLong("userSpamComment") ?: 0f).toInt(),
            (getLong("userSpamReport") ?: 0f).toInt(),
            (getLong("userSpamContact") ?: 0f).toInt(),
            getBoolean("userAddPost") ?: true,
            getBoolean("userAddComment") ?: true,
            getBoolean("userAddReport") ?: true,
            getBoolean("userAddContact") ?: true,
            getBoolean("userEditProfile") ?: true,
            getBoolean("userIsActive") ?: true,
            getBoolean("userEmailConfirm") ?: false,
            getTimestamp("userAdminMessageDate") ?: Timestamp.now(),
            getTimestamp("userRegisterDate") ?: Timestamp.now()
        )

    }

    fun DocumentSnapshot.toPostModel () : PostModel {

        return PostModel(
            (this.getLong("postCategoryId") ?: 0f).toInt(),
            this.getString("postContent") ?: "-",
            this.getBoolean("postLikePublic") ?: false,
            this.getString("postId") ?: "-",
            this.getString("postUserId") ?: "-",
            this.getTimestamp("postDate") ?: Timestamp.now()
        )

    }

    fun DocumentSnapshot.toLikeDislikeModel() : LikeDislikeModel {

        return LikeDislikeModel(
            getString("actionUserId") ?: "-",
            getString("postId") ?: "-",
            getString("postUserId") ?: "-",
            getString("postRating") ?: "5",
            getString("postComment") ?: "-",
            getBoolean("postCommentExist") ?: false,
            getTimestamp("actionDate") ?: Timestamp.now()
        )

    }

    fun DocumentSnapshot.toSearchModelServer() : SearchRecyclerModel {

        return SearchRecyclerModel(
            getString("userId") ?: "-",
            getString("userMail") ?: "-",
            getString("userName") ?: "-",
            getString("userRealName") ?: "-",
            getString("userBiography") ?: "-",
            getString("userProfilePicture") ?: "-",
            getString("userTwitter") ?: "-",
            getString("userFacebook") ?: "-",
            getString("userInstagram") ?: "-",
            getString("userAdminMessageTr") ?: "Fikirzademe hoşgeldiniz",
            getString("userAdminMessageEn") ?: "Welcome to the Fikirzadem",
            (getLong("userFollowing") ?: 0f).toInt(),
            (getLong("userFollower") ?: 0f).toInt(),
            (getLong("userReport") ?: 0f).toInt(),
            (getLong("userSpamPost") ?: 0f).toInt(),
            (getLong("userSpamComment") ?: 0f).toInt(),
            (getLong("userSpamReport") ?: 0f).toInt(),
            (getLong("userSpamContact") ?: 0f).toInt(),
            getBoolean("userAddPost") ?: true,
            getBoolean("userAddComment") ?: true,
            getBoolean("userAddReport") ?: true,
            getBoolean("userAddContact") ?: true,
            getBoolean("userEditProfile") ?: true,
            getBoolean("userIsActive") ?: true,
            getBoolean("userEmailConfirm") ?: false,
            getTimestamp("userAdminMessageDate") ?: Timestamp.now(),
            getTimestamp("userRegisterDate") ?: Timestamp.now(),
            true
        )

    }

    fun DocumentSnapshot.toSearchModelCache() : SearchRecyclerModel {

        return SearchRecyclerModel(
            getString("userId") ?: "-",
            getString("userMail") ?: "-",
            getString("userName") ?: "-",
            getString("userRealName") ?: "-",
            getString("userBiography") ?: "-",
            getString("userProfilePicture") ?: "-",
            getString("userTwitter") ?: "-",
            getString("userFacebook") ?: "-",
            getString("userInstagram") ?: "-",
            getString("userAdminMessageTr") ?: "Fikirzademe hoşgeldiniz",
            getString("userAdminMessageEn") ?: "Welcome to the Fikirzadem",
            (getLong("userFollowing") ?: 0f).toInt(),
            (getLong("userFollower") ?: 0f).toInt(),
            (getLong("userReport") ?: 0f).toInt(),
            (getLong("userSpamPost") ?: 0f).toInt(),
            (getLong("userSpamComment") ?: 0f).toInt(),
            (getLong("userSpamReport") ?: 0f).toInt(),
            (getLong("userSpamContact") ?: 0f).toInt(),
            getBoolean("userAddPost") ?: true,
            getBoolean("userAddComment") ?: true,
            getBoolean("userAddReport") ?: true,
            getBoolean("userAddContact") ?: true,
            getBoolean("userEditProfile") ?: true,
            getBoolean("userIsActive") ?: true,
            getBoolean("userEmailConfirm") ?: false,
            getTimestamp("userAdminMessageDate") ?: Timestamp.now(),
            getTimestamp("userRegisterDate") ?: Timestamp.now(),
            false
        )

    }

    fun DocumentSnapshot.toSocialModel() : SocialModel {

        return SocialModel(
            getString("userId") ?: "-",
            getString("actionType") ?: "-",
            getTimestamp("actionDate") ?: Timestamp.now()
        )

    }

    fun DocumentSnapshot.toContactModel () : ContactModel {
        return ContactModel(
            getString("contactId") ?: "-",
            getString("contactUserId") ?: "-",
            getString("contactMessage") ?: "-",
            getTimestamp("contactDate") ?: Timestamp.now()
        )
    }

    fun DocumentSnapshot.toReportModel () : ReportModel {
        return ReportModel(
            getString("reportId") ?: "-",
            (getLong("reportCategory") ?: 0f).toInt(),
            getString("reportContentId") ?: "-",
            getString("reportContentExtraId") ?: "-",
            getString("reportContent") ?: "-",
            getString("reportContentDetail") ?: "-",
            getString("reportType") ?: "-",
            getString("reporterUserId") ?: "-",
            getTimestamp("reportDate") ?: Timestamp.now()
        )
    }



    fun QuerySnapshot.dataAvailable() : Boolean { return !documents.isNullOrEmpty() }

    fun Task<QuerySnapshot>.dataAvailable() : Boolean {

        return isSuccessful && !result.documents.isNullOrEmpty()

        /**
         * Eğer task isSuccessful ise ve dönen documents listesi boş değil ise true
         * Değil ise false
         */
    }

    @JvmName("dataAvailableDocumentSnapshot")
    fun Task<DocumentSnapshot>.dataAvailable() : Boolean {

        return isSuccessful && result.exists()

        /**
         * Eğer task isSuccessful ise ve dönen data var ise true
         * Değil ise false
         */
    }

    fun Task<QuerySnapshot>.exceptionMsg() : String {

        return when(exception == null) {
            true -> "empty message"
            else -> when(exception!!.localizedMessage == null) {
                    true -> "empty message"
                    else -> exception!!.localizedMessage!!.toString()
                }
        }
    }



    fun Snackbar.settings() : Snackbar {
        this.duration = 1150
        this.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
        this.setBackgroundTint(WorkUtil.snackbarColor(context))
        this.setTextColor(Color.LTGRAY)
        return this
    }

    fun Snackbar.widthSettings() : Snackbar {

        this.view.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT

        return this

    }

    @SuppressLint("InflateParams")
    fun Snackbar.questionSnackbar() : Snackbar {
        (view as SnackbarLayout).addView(
            (context as Activity).layoutInflater.inflate(R.layout.bottom_question_snackbar, null), 0
        )

        return this
    }


    fun SavedPostModel.roomSavedPost(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {

            val tag = "Util-RoomSavedPost"
            try {
                val db = Room
                    .databaseBuilder(context, FikirzademDatabase::class.java,"FikirzademDB")
                    .build()

                db.fikirzademDao().addSavedPost(this@roomSavedPost)
                db.close()

                Log.i(tag, "Favori post veritabanına eklendi")
            }
            catch (e: Exception) {
                Log.e(tag, "Favori post veritabanına eklenemedi")
                Log.e(tag, e.msg())
            }
        }
    }

}