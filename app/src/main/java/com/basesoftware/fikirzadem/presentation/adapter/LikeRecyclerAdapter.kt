package com.basesoftware.fikirzadem.presentation.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.PostCommentDialogBinding
import com.basesoftware.fikirzadem.databinding.RowLikeDesignBinding
import com.basesoftware.fikirzadem.model.CommentDetailModel
import com.basesoftware.fikirzadem.model.LikeDislikeModel
import com.basesoftware.fikirzadem.model.ReportModel
import com.basesoftware.fikirzadem.model.UserModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.dataAvailable
import com.basesoftware.fikirzadem.util.ExtensionUtil.firestoreError
import com.basesoftware.fikirzadem.util.ExtensionUtil.gone
import com.basesoftware.fikirzadem.util.ExtensionUtil.itemAnimation
import com.basesoftware.fikirzadem.util.WorkUtil.changeFragment
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.message
import com.basesoftware.fikirzadem.util.ExtensionUtil.safePosition
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.toLikeDislikeModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.toUserModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList

class LikeRecyclerAdapter constructor(
    private var adapterContext : Context,
    private var sharedViewModel : SharedViewModel,
    private var likeSheetDialog : BottomSheetDialog,
    private var likeSheetView : View,
    private val navController : NavController) : RecyclerView.Adapter<LikeRecyclerAdapter.RecyclerHolder>() {

    private var likeList : ArrayList<LikeDislikeModel> = arrayListOf()
    private lateinit var arrayLikeUsers : ArrayList<String> // Sunucudan indirilen kullanıcılar

    private val firestore = WorkUtil.firestore()

    private lateinit var recycler : RecyclerView

    private lateinit var commentDialog : Dialog
    private lateinit var dialogBinding : PostCommentDialogBinding

    private lateinit var arrayDeletedPost : ArrayList<Int>
    private lateinit var arrayRefreshedPost : ArrayList<Int>


    class LikeDislikeUtil(private val oldList : ArrayList<LikeDislikeModel>, private val newList : ArrayList<LikeDislikeModel>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(old: Int, new: Int): Boolean = oldList[old].actionUserId.matches(Regex(newList[new].actionUserId))

        override fun areContentsTheSame(old: Int, new: Int): Boolean = oldList[old].actionUserId.matches(Regex(newList[new].actionUserId))

    }

    class RecyclerHolder(val binding : RowLikeDesignBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {

        recycler = recyclerView

        commentDialog = Dialog(likeSheetDialog.context)

        arrayDeletedPost = arrayListOf()

        arrayRefreshedPost = arrayListOf()

        arrayLikeUsers = arrayListOf()

        super.onAttachedToRecyclerView(recyclerView)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        val binding = RowLikeDesignBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        val holder = RecyclerHolder(binding)

        val listener  = View.OnClickListener {

            holder.binding.deletedUser?.let {

                if(!it) {

                    if(likeSheetDialog.isShowing) { likeSheetDialog.dismiss() }
                    val argument = Bundle()
                    argument.putString("userId", likeList[holder.bindingAdapterPosition.safePosition()].actionUserId)
                    changeFragment(navController, argument, R.id.profileFragment)

                }

            }

        }

        holder.binding.apply {

            imgLikeProfilePicture.setOnClickListener(listener)
            txtLikeUserName.setOnClickListener(listener)

            imgPostComment.setOnClickListener {
                like?.let {
                    if(it.postCommentExist && arrayDeletedPost.indexOf(holder.bindingAdapterPosition.safePosition()) == -1) {
                        // Eğer şuan tıklanan beğenideki yorum, yorum sahibi veya içerik sahibi tarafıdan şimdi silinmedi ise popup aç
                        addCommentPopup(holder)
                    }
                }
            }

            ratingLike.setOnTouchListener { _, event ->

                like?.let {
                    if (it.postCommentExist && event != null && event.action == MotionEvent.ACTION_UP) {
                        // Eğer şuan tıklanan beğenideki yorum, yorum sahibi veya içerik sahibi tarafıdan şimdi silinmedi ise popup aç
                        if(arrayDeletedPost.indexOf(holder.bindingAdapterPosition.safePosition()) == -1) addCommentPopup(holder)
                    }
                }

                return@setOnTouchListener true

            }

        }


        return holder
    }

    override fun getItemCount(): Int = likeList.size

    override fun onBindViewHolder(holder: RecyclerHolder, position: Int) {

        holder.itemView.itemAnimation(sharedViewModel.getAnimation())

        when(likeList[holder.bindingAdapterPosition.safePosition()].actionUserId == sharedViewModel.getMyUserId()) {

            true -> {
                writeProfile(holder, sharedViewModel.getUser()!!, likeList[holder.bindingAdapterPosition.safePosition()])
                Log.i("LikeAdapter-BindHolder","Kullanıcı viewmodeldan alındı")
            }

            false -> when(arrayLikeUsers.contains(likeList[holder.bindingAdapterPosition.safePosition()].actionUserId)) {

                // Kullanıcı daha önce indirildi ise önbellekten al
                true -> userDownloadFromCache(holder, likeList[holder.bindingAdapterPosition.safePosition()].actionUserId)

                // Kullanıcı adaha önce indirilmedi ise sunucudan al
                else -> userDownloadFromServer(holder, likeList[holder.bindingAdapterPosition.safePosition()].actionUserId)

            }

        }

    }



    private fun logSnackbar(tag : String, view : View, message : Int, type : String) {

        CoroutineScope(Dispatchers.Main).launch {

            when(type)
            {
                "error" -> Log.e(tag, likeSheetDialog.context.getString(message))
                "warning" -> Log.w(tag, likeSheetDialog.context.getString(message))
                "info" -> Log.i(tag, likeSheetDialog.context.getString(message))
            }

            Snackbar
                .make(likeSheetDialog.context, view.rootView, likeSheetDialog.context.getString(message), Snackbar.LENGTH_SHORT)
                .settings()
                .widthSettings()
                .setGestureInsetBottomIgnored(true)
                .setAnchorView(likeSheetView.rootView.findViewById(R.id.viewLikeSnackbar))
                .show()

        }

    }

    private fun dialogSnackbar(text : Int, textColor : Int) {

        Snackbar
            .make(dialogBinding.viewLikeDialogSnackbar, adapterContext.getString(text), Snackbar.LENGTH_SHORT)
            .apply {
                duration = 1150
                animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
                setBackgroundTint(WorkUtil.snackbarColor(adapterContext))
                setTextColor(textColor)
                isGestureInsetBottomIgnored = true
                widthSettings()
                anchorView = dialogBinding.viewLikeDialogSnackbar
                show()
            }

    }



    fun setData(newLikeList : ArrayList<LikeDislikeModel>) {

        val utilObj = LikeDislikeUtil(likeList, newLikeList)
        val utilResult = DiffUtil.calculateDiff(utilObj)
        likeList.clear()
        likeList.addAll(newLikeList)
        utilResult.dispatchUpdatesTo(this)

    }

    @SuppressLint("InflateParams")
    private fun addCommentPopup(holder: RecyclerHolder) {

        commentDialog = Dialog(adapterContext)

        commentDialog.apply {

            dialogBinding = PostCommentDialogBinding
                .inflate(LayoutInflater.from(adapterContext.applicationContext), null, false)

            setContentView(dialogBinding.root)

            likeList[holder.bindingAdapterPosition.safePosition()].apply {

                dialogBinding.commentDetail = CommentDetailModel(
                    postComment,
                    actionUserId,
                    postUserId == sharedViewModel.getMyUserId() || actionUserId == sharedViewModel.getMyUserId(),
                    actionUserId != sharedViewModel.getMyUserId()
                )

                // Eğer bu yorum daha önce yenilenmedi ise yenileme izni ver (VISIBLE Yap)
                dialogBinding.refreshPermission = arrayRefreshedPost.indexOf(holder.bindingAdapterPosition.safePosition()) == -1

            }

            window?.let {

                it.setLayout(-1,-1)

                it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                it.attributes?.windowAnimations = R.style.likeDialog

            }

            dialogBinding.constraintPostComment.setOnClickListener { dismiss() }

            dialogBinding.imgCommentDelete.setOnClickListener {

                Snackbar
                    .make(dialogBinding.viewLikeDialogSnackbar, adapterContext.getString(R.string.comment_delete), Snackbar.LENGTH_LONG)
                    .apply {
                        animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
                        setBackgroundTint(WorkUtil.snackbarColor(adapterContext))
                        setTextColor(Color.LTGRAY)
                        isGestureInsetBottomIgnored = true
                        (view as Snackbar.SnackbarLayout).addView(
                            (adapterContext as Activity).layoutInflater.inflate(R.layout.bottom_question_snackbar, null), 0
                        )
                        setAction(adapterContext.getString(R.string.yes)) { deleteComment(holder) }
                        setActionTextColor(Color.GREEN)
                        widthSettings()
                        anchorView = dialogBinding.viewLikeDialogSnackbar
                        show()
                    }

            }

            dialogBinding.imgCommentRefresh.setOnClickListener {

                CoroutineScope(Dispatchers.IO).launch {

                    firestore
                        .collection("Posts")
                        .document(likeList[holder.bindingAdapterPosition.safePosition()].postId)
                        .collection("PostAction")
                        .document(likeList[holder.bindingAdapterPosition.safePosition()].actionUserId)
                        .get(Source.SERVER)
                        .addOnCompleteListener { actionTask ->

                            when(actionTask.isSuccessful) {
                                true -> {

                                    when(actionTask.result.exists()) {

                                        true -> {

                                            if(!actionTask.result.toLikeDislikeModel().postCommentExist) {
                                                // Yorum silinmiş
                                                arrayDeletedPost.add(holder.bindingAdapterPosition.safePosition())
                                                commentDialog.dismiss()
                                                logSnackbar("LikeAdaptrRefrshComment",likeSheetView,R.string.comment_not_exist, "error")
                                            }
                                            else {

                                                if(commentDialog.isShowing) {
                                                    dialogBinding.imgCommentRefresh.gone()
                                                    dialogBinding.txtPostComment.text = actionTask.result.getString("postComment") ?: "-"
                                                    arrayRefreshedPost.add(holder.bindingAdapterPosition.safePosition())
                                                    dialogSnackbar(R.string.comment_update_success, Color.GREEN)
                                                }

                                            }

                                        }

                                        else -> {
                                            commentDialog.dismiss()
                                            logSnackbar("LikeAdaptrRefrshComment",likeSheetView,R.string.user_or_post_deleted, "error")
                                        }

                                    }

                                }

                                else -> dialogSnackbar(R.string.comment_update_fail, Color.RED)
                            }

                        }

                }

                dialogBinding.imgCommentRefresh.gone()

                arrayRefreshedPost.add(holder.bindingAdapterPosition.safePosition())

            }

            dialogBinding.imgCommentReport.setOnClickListener {

                sharedViewModel.getUser()?.let { user ->

                    when(!user.userAddReport) {

                        true -> dialogSnackbar(R.string.notification_new_report_block, Color.RED)

                        else -> when(dialogBinding.imgCommentRefresh.visibility == View.GONE) {

                            // Yorum yenilendi rapor gönderilebilir
                            true -> sendReport(holder, dialogBinding.txtPostComment.text.toString()).also { commentDialog.dismiss() }

                            else -> dialogSnackbar(R.string.comment_delete_refresh, Color.RED)

                        }

                    }

                }

            }

            show()

        }

    }

    private fun deleteComment(holder: RecyclerHolder) {
        CoroutineScope(Dispatchers.IO).launch {

            /**
             * ### Update işlemi (yorum alanlarının güncellenmesi)
             *
             * - onSuccess -- İşlem başarılı
             *
             * - İşlem başarısız -- Like/dislike dökümanının varlığı kontrol et
             *
             * - Like/dislike dökümanı mevcut -- İşlem başarısız
             *
             * - Like/dislike dökümanı mevcut değil -- İçeriği kontrol et
             *
             * - İçerik mevcut -- Kullanıcı silinmiş
             *
             * - İçerik mevcut değil -- İçerik silinmiş
             * */

            val postRef = firestore
                .collection("Posts")
                .document(likeList[holder.bindingAdapterPosition.safePosition()].postId)

            val actionRef = postRef
                .collection("PostAction")
                .document(likeList[holder.bindingAdapterPosition.safePosition()].actionUserId)

            val funTag = "LikeAdaptrDeleteComment"

            actionRef
                .update(mutableMapOf<String, Any?>("postCommentExist" to false))
                .addOnCompleteListener { updateTask ->

                    commentDialog.dismiss()

                    when (updateTask.isSuccessful) {

                        true -> {
                            arrayDeletedPost.add(holder.bindingAdapterPosition.safePosition())

                            logSnackbar(funTag, likeSheetView, R.string.comment_delete_success, "info")
                        }

                        else -> actionRef
                            .get(Source.SERVER)
                            .addOnCompleteListener { actionTask ->

                                when(actionTask.isSuccessful) {

                                    true -> {

                                        when(actionTask.result.exists()) {

                                            true -> logSnackbar(funTag, likeSheetView, R.string.comment_delete_fail, "error")

                                            else -> {

                                                postRef
                                                    .get(Source.SERVER)
                                                    .addOnSuccessListener { postTask ->
                                                        when(postTask.exists()) {
                                                            true -> logSnackbar(funTag,likeSheetView,R.string.user_deleted, "error")
                                                            else -> logSnackbar(funTag,likeSheetView,R.string.post_not_found, "error")
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        logSnackbar(funTag, likeSheetView, R.string.comment_delete_fail, "error")
                                                    }

                                            }

                                        }

                                    }

                                    else -> logSnackbar(funTag, likeSheetView, R.string.comment_delete_fail, "error")

                                }

                            }

                    }

                }

        }
    }

    private fun sendReport(holder: RecyclerHolder, comment : String) {

        CoroutineScope(Dispatchers.IO).launch {

            val sendTag = "LikeAdapter-sendReport"

            val reportId = likeList[holder.bindingAdapterPosition.safePosition()].postId+"-"+likeList[holder.bindingAdapterPosition.safePosition()].actionUserId

            firestore
                .collection("Reports")
                .document(reportId)
                .set(ReportModel(
                    reportId,
                    0,
                    likeList[holder.bindingAdapterPosition.safePosition()].postId,
                    likeList[holder.bindingAdapterPosition.safePosition()].actionUserId,
                    "Comment - Report",
                    comment,
                    "comment",
                    sharedViewModel.getMyUserId(),
                    null
                ))
                .addOnSuccessListener { logSnackbar(sendTag, likeSheetView, R.string.report_send_success, "info") }
                .addOnFailureListener {
                    logSnackbar(
                        sendTag,
                        likeSheetView,
                        it.firestoreError(),
                        "error")

                    Log.e(sendTag, it.msg())
                }

        }

    }



    private fun deletedProfile(holder: RecyclerHolder) { CoroutineScope(Dispatchers.Main).launch { holder.binding.deletedUser = true } }

    private fun writeProfile(holder : RecyclerHolder, userModel : UserModel, likeModel : LikeDislikeModel) {
        CoroutineScope(Dispatchers.Main).launch {

            holder.binding.apply {

                deletedUser = false

                user = userModel

                like = likeModel

            }

        }
    }



    private fun userDownloadFromCache(holder: RecyclerHolder, userId: String) {

        CoroutineScope(Dispatchers.IO).launch {
            val tagLikeAdapterCache = "LikeAdapterChkCacheUser"

            firestore
                .collection("Users")
                .document(userId)
                .get(Source.CACHE)
                .addOnCompleteListener {
                    when(it.dataAvailable()) {
                        true -> {

                            Log.i(tagLikeAdapterCache, "Kullanıcı önbellekten alınıyor")
                            writeProfile(holder,it.result.toUserModel(),likeList[holder.bindingAdapterPosition.safePosition()])

                        }
                        else -> {
                            // Kullanıcı önbellekte bulunamadı firestore gidiyor
                            Log.i(tagLikeAdapterCache,"Kullanıcı önbellekten alınamadı, Firebase gidiliyor")
                            userDownloadFromServer(holder, userId)
                        }
                    }
                }

        }

    }

    @SuppressLint("LogConditional")
    private fun userDownloadFromServer(holder: RecyclerHolder, userId: String) {

        val tagLikeAdapterServer = "LikeAdapterGoToFirebase"

        firestore
            .collection("Users")
            .document(userId)
            .get(Source.SERVER)
            .addOnSuccessListener { user ->

                when(user.exists()) {
                    true -> {

                        // Kullanıcı firebasede mevcut
                        Log.i(tagLikeAdapterServer, "Kullanıcı önbelleğe kaydedildi")
                        writeProfile(holder, user.toUserModel(), likeList[holder.bindingAdapterPosition.safePosition()])
                        arrayLikeUsers.add(userId)

                    }
                    false -> {

                        // Kullanıcı firebasede mevcut değil
                        // Post'un like-dislike tablosundaki kullanıcının yeri

                        firestore
                            .collection("Posts")
                            .document(likeList[holder.bindingAdapterPosition.safePosition()].postId)
                            .collection("PostAction")
                            .document(userId)
                            .delete()
                            .addOnSuccessListener {
                                Log.i(tagLikeAdapterServer, "Kullanıcı silinmiş, like tablosundan da silindi [Server]")
                            }
                            .addOnFailureListener {
                                Log.e(tagLikeAdapterServer, "Kullanıcı silinmiş, like tablosundan silinemedi [SERVER]")
                                Log.e(tagLikeAdapterServer, it.msg())
                            }

                        Log.i(tagLikeAdapterServer, recycler.context.getString(R.string.user_deleted))
                        CoroutineScope(Dispatchers.Main).launch { deletedProfile(holder) }

                    }
                }

            }
            .addOnFailureListener {
                CoroutineScope(Dispatchers.Main).launch {
                        Log.e(tagLikeAdapterServer, recycler.context.getString(R.string.error))
                        Snackbar
                            .make(likeSheetDialog.window!!.decorView,
                                recycler.context.getString(R.string.error),
                                Snackbar.LENGTH_SHORT)
                            .settings().widthSettings().setGestureInsetBottomIgnored(true)
                            .setAnchorView(likeSheetDialog.window!!.decorView.findViewById(R.id.viewLikeSnackbar))
                            .show()

                    Log.e(tagLikeAdapterServer, "Kullanıcı firebaseden alınamadı")
                    Log.e(tagLikeAdapterServer, it.msg())
                    deletedProfile(holder)
                }
            }


    }


}