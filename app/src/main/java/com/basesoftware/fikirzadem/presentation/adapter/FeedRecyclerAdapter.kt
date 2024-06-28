package com.basesoftware.fikirzadem.presentation.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.data.local.FikirzademDatabase
import com.basesoftware.fikirzadem.data.local.dto.SavedPostModel
import com.basesoftware.fikirzadem.databinding.RowFeedDesignBinding
import com.basesoftware.fikirzadem.model.*
import com.basesoftware.fikirzadem.util.ExtensionUtil.dataAvailable
import com.basesoftware.fikirzadem.util.ExtensionUtil.firestoreError
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.WorkUtil.resDrawable
import com.basesoftware.fikirzadem.util.WorkUtil.changeFragment
import com.basesoftware.fikirzadem.util.WorkUtil.getDeviceHeight
import com.basesoftware.fikirzadem.util.ExtensionUtil.gone
import com.basesoftware.fikirzadem.util.ExtensionUtil.itemAnimation
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.message
import com.basesoftware.fikirzadem.util.ExtensionUtil.questionSnackbar
import com.basesoftware.fikirzadem.util.ExtensionUtil.roomSavedPost
import com.basesoftware.fikirzadem.util.ExtensionUtil.safePosition
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.show
import com.basesoftware.fikirzadem.util.ExtensionUtil.toLikeDislikeModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.toPostModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.toUserModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.ArrayList

class FeedRecyclerAdapter constructor(
    private val shared : SharedViewModel,
    private val textToSpeech: TextToSpeech,
    private val sourceData : ArrayList<PostModel>,
    private val fragmentResume : MutableLiveData<Boolean>,
    private val callFrom : String) :
    RecyclerView.Adapter<FeedRecyclerAdapter.RecyclerHolder>() {

    private var postList : ArrayList<PostModel> = arrayListOf()
    private val firestore = WorkUtil.firestore()

    private lateinit var likeDialog : Dialog

    private lateinit var optionSheetDialog : BottomSheetDialog
    private lateinit var optionSheetView : View

    private lateinit var reportSheetDialog : BottomSheetDialog
    private lateinit var reportSheetView : View

    private lateinit var socialSheetDialog : BottomSheetDialog
    private lateinit var socialSheetView : View

    private lateinit var recyclerAdapter: RecyclerView
    private lateinit var groupAdapter: ViewGroup

    private var speechText : String? = null

    private val emptyText : String = "-"
    private lateinit var selectUserName : String
    private lateinit var selectPostContent : String
    private lateinit var selectPostTime : String
    private lateinit var selectedHolder : RecyclerHolder

    private lateinit var arrayNewDeletedPost : ArrayList<String> // Kullanıcının şimdi sildiği gönderiler
    private lateinit var arrayUsers : ArrayList<String> // Sunucudan indirilen kullanıcılar

    private lateinit var progressSheet : ProgressBar
    private lateinit var recyclerSheet : RecyclerView
    private lateinit var txtSheetCount : TextView

    private var lastDataTime : Timestamp? = null

    private var socialDataList : ArrayList<LikeDislikeModel> = arrayListOf()



    class FeedUtil(private val oldList : ArrayList<PostModel>, private val newList : ArrayList<PostModel>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(old: Int, new: Int): Boolean = oldList[old].postId.matches(Regex(newList[new].postId))

        override fun areContentsTheSame(old: Int, new: Int): Boolean {

            return when {

                !oldList[old].postContent.equals(newList[new].postContent,false) -> false
                !oldList[old].postId.equals(newList[new].postId,false) -> false
                !oldList[old].postUserId.equals(newList[new].postUserId,false) -> false
                oldList[old].postDate!!.nanoseconds != newList[new].postDate!!.nanoseconds -> false
                oldList[old].postCategoryId != newList[new].postCategoryId -> false
                oldList[old].postLikePublic != newList[new].postLikePublic -> false

                else -> true
            }

        }

    }

    class RecyclerHolder(val binding: RowFeedDesignBinding) : RecyclerView.ViewHolder(binding.root)




    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        recyclerAdapter = recyclerView

        arrayNewDeletedPost = arrayListOf()

        arrayUsers = arrayListOf()

        likeDialog = Dialog(context())

        optionSheetDialog = BottomSheetDialog(context(), R.style.BottomSheetDialogTheme)

        reportSheetDialog = BottomSheetDialog(context(), R.style.BottomSheetDialogTheme)

        socialSheetDialog = BottomSheetDialog(context(),R.style.BottomSheetDialogTheme)

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

            override fun onStart(utteranceId: String?) { speechText = utteranceId }

            override fun onDone(utteranceId: String?) { speechText = null }

            override fun onError(utteranceId: String?) { speechText = null }

        })

        fragmentResume.observe(context() as FragmentActivity) {
            if(!it) {

                if(likeDialog.isShowing) likeDialog.dismiss()
                if(optionSheetDialog.isShowing) optionSheetDialog.dismiss()
                if(reportSheetDialog.isShowing) reportSheetDialog.dismiss()
                if(socialSheetDialog.isShowing) socialSheetDialog.dismiss()

            }
        }

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        val binding = RowFeedDesignBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        groupAdapter = parent

        val holder = RecyclerHolder(binding)

        holder.apply {

            itemView.variableListener(holder)

            binding.apply {

                txtPostContentFeedRecycler.variableListener(holder)

                imgPostRating.likeListener(holder)
                imgPostRatingUser.likeUserListener(holder)

                imgPostOption.setOnClickListener {
                    failPost?.let { fail -> if(!fail) updateVariable(holder).also { bottomSheet() } }
                }

            }

        }

        if(!callFrom.matches(Regex("profile"))) {

            holder.binding.imgPpFeedRecycler.setOnClickListener {
                /**
                 *  Eğer adaptör feed {anasayfa veya ilgilendiklerim} için çağırıldı ise resime tıklayınca profile git
                 *  Eğer adaptör profil sayfasındaki feed için çağırıldı ise onClick koyma
                 *  Engellenen bug : Bir Profil sayfasında profil incelenirken post bölümünde sürekli resime tıklanması
                 *  Bunun sonucunda sürekli profil sayfasının yenilenmesi
                 */

                // Kullanıcı resmine tıklandı profile gidilecek.
                val argument = Bundle()
                argument.putString("userId",postList[holder.bindingAdapterPosition.safePosition()].postUserId)
                changeFragment(Navigation.findNavController(recyclerAdapter), argument, R.id.profileFragment)

            }

        }

        if(!callFrom.matches(Regex("interest"))) {

            holder.binding.txtCategoryFeedRecycler.setOnClickListener {
                /**
                 *  Eğer adaptör feed {anasayfa veya profil} için çağırıldı ise kategoriye tıklayınca ilgilendiklerim'e git
                 *  Eğer adaptör ilgilendiklerim sayfasındaki feed için çağırıldı ise onClick koyma
                 *  Engellenen bug : İlgilendiklerim sayfasında postlar incelenirken sürekli kategoriye tıklanması
                 *  Bunun sonucunda sürekli ilgilendiklerim fragment'ının yenilenmesi
                 */

                // Kullanıcı bir kategoriye tıkladı ise ilgilendiklerim fragment'ına gidecek.
                val argument = Bundle()
                argument.putInt("categoryId", postList[holder.bindingAdapterPosition.safePosition()].postCategoryId)
                changeFragment(Navigation.findNavController(recyclerAdapter), argument, R.id.interestFragment)

            }

        }


        return holder
    }

    override fun getItemCount(): Int { return postList.size }

    override fun onBindViewHolder(holder: RecyclerHolder, position: Int) {

        holder.binding.apply {
            post = postList[holder.bindingAdapterPosition.safePosition()]
            failPost = false
        }

        /**
         * Post sahibi kullanıcının kendisi ise bilgileri SharedViewModel'dan getir
         *
         * Post sahibi kullanıcının kendisi değil ise kontrol et:
         * Eğer kullanıcı daha önce sunucudan indirildi ise önbellekten al
         * Kullanıcı daha önce sunucudan indirilmedi ise sunucudan indirmeyi dene
         */
        when(postList[holder.bindingAdapterPosition.safePosition()].postUserId == shared.getMyUserId()) {

            true -> when(arrayNewDeletedPost.indexOf(postList[holder.bindingAdapterPosition.safePosition()].postId) == -1) {
                /**
                 *  Eğer görüntülenen post şuanda silinen gönderilere eklenmedi ise ui'da göster
                 *  Eğer eklendi ise ui'dan kaldır
                 */
                true -> shared.getUser()?.let { holder.binding.user = it }

                else -> deletedOrFailUserPost(holder)

            }

            false -> when(callFrom.matches(Regex("profile"))) {

                true -> checkUserInCache(holder)

                else -> when(arrayUsers.contains(postList[holder.bindingAdapterPosition.safePosition()].postUserId)) {

                    true -> checkUserInCache(holder) // Kullanıcı daha önce indirildi ise önbellekten al

                    else -> getFirebaseUser(holder)  // Kullanıcı adaha önce indirilmedi ise sunucudan al

                }

            }
        }

        holder.itemView.itemAnimation(shared.getAnimation()) // Eğer animasyonlar açık ise göster

    }




    fun setData(newPostList : ArrayList<PostModel>) {

        val utilObj = FeedUtil(postList, newPostList)
        val utilResult = DiffUtil.calculateDiff(utilObj)
        postList.clear()
        postList.addAll(newPostList)
        utilResult.dispatchUpdatesTo(this@FeedRecyclerAdapter)

    }



    private fun View.variableListener(holder: RecyclerHolder) {

        setOnLongClickListener {

            holder.binding.failPost?.let { fail -> if(!fail) updateVariable(holder).also { bottomSheet() } }

            return@setOnLongClickListener true
        }

    }


    private fun ImageView.likeUserListener(holder: RecyclerHolder) {

        setOnClickListener { updateVariable(holder).also { socialListener(true) } }

    }

    private fun ImageView.likeListener(holder: RecyclerHolder) {

        setOnClickListener {

            updateVariable(holder)

            when(postList[holder.bindingAdapterPosition.safePosition()].postUserId != shared.getMyUserId()) {

                true -> checkLikeInCache(holder)

                false -> {
                    snackbar(
                        "FeedAdapter-MyPostLike",
                        context().getString(R.string.like_fail_my_post),
                        "info",
                        true)
                }

            }

        }

    }




    private fun snackbar(tag : String, message : String, type: String, parentFragment : Boolean) {

        CoroutineScope(Dispatchers.Main).launch {

            when(type) {
                "error" -> Log.e(tag, message)
                "warning" -> Log.w(tag, message)
                "info" -> Log.i(tag, message)
            }

            val view = when(parentFragment){
                true -> groupAdapter
                false -> socialSheetView.rootView
            }

            val snackbar = when(parentFragment) {
                true ->
                    Snackbar
                        .make(context(), view, message, Snackbar.LENGTH_SHORT)
                        .settings()
                        .widthSettings()
                else ->
                    Snackbar
                    .make(context(), view, message, Snackbar.LENGTH_SHORT)
                    .settings()
                    .widthSettings()
                    .setGestureInsetBottomIgnored(true)
                    .setAnchorView(socialSheetView.rootView.findViewById(R.id.viewLikeSnackbar))
            }

            snackbar.show()

        }

    }

    private fun context() : Context = recyclerAdapter.context





    //------------------------------ HOLDER SETTINGS ------------------------------

    private fun updateVariable(holder: RecyclerHolder) {
        this.selectedHolder = holder
        this.selectUserName = holder.binding.txtUserNameFeedRecycler.text.toString()
        this.selectPostContent = holder.binding.txtPostContentFeedRecycler.text.toString()
        this.selectPostTime = holder.binding.txtPostTime.text.toString()
    }

    //------------------------------ HOLDER-POSITION SETTINGS ------------------------------





    //------------------------------ POST LIKE ACTION ------------------------------

    private fun checkLikeInCache(holder: RecyclerHolder) {

        CoroutineScope(Dispatchers.IO).launch {

            val tagCheckLikeCache = "FeedAdapterChkLikeCache"

            firestore
                .collection("Posts")
                .document(postList[holder.bindingAdapterPosition.safePosition()].postId)
                .collection("PostAction")
                .document(shared.getMyUserId())
                .get(Source.CACHE)
                .addOnCompleteListener {

                    when(it.dataAvailable()) {

                        true -> snackbar(
                            tagCheckLikeCache,
                            context().getString(R.string.likedislike_available),
                            "info",
                            true
                        ) // Daha önce like/dislike gönderilmiş

                        else -> {
                            // Önbellekte like/dislike bulunamadı firestore kontrol edilecek
                            getFirebaseLike(holder)
                            Log.i(tagCheckLikeCache, "Like dislike önbellekte bulunamadı")
                        }

                    }

                }

        }
    }

    private fun getFirebaseLike(holder: RecyclerHolder) {

        val tagCheckLikeCache = "FeedAdaptrGetLikeServer"

        val likeSendFail = OnFailureListener {
            snackbar(
                tagCheckLikeCache,
                context().getString(R.string.likedislike_fail),
                "error",
                true
            )
            Log.e(tagCheckLikeCache, it.msg())
        }

        firestore
            .collection("Posts")
            .document(postList[holder.bindingAdapterPosition.safePosition()].postId)
            .get(Source.SERVER)
            .addOnSuccessListener { checkPostSnapshot ->

                if(checkPostSnapshot.exists()){
                    // Post firebasede mevcut şimdi like veya dislike mevcut mu o kontrol edilecek

                    val likeDislikeRef = firestore
                        .collection("Posts")
                        .document(postList[holder.bindingAdapterPosition.safePosition()].postId)
                        .collection("PostAction")
                        .document(shared.getMyUserId())

                    likeDislikeRef
                        .get(Source.SERVER)
                        .addOnSuccessListener { ref ->
                            if (ref.exists()) {
                                // Görüş firestore'da var, önbelleğe kaydediliyor...

                                snackbar(
                                    tagCheckLikeCache,
                                    context().getString(R.string.likedislike_available),
                                    "info",
                                    true
                                )
                                Log.i(tagCheckLikeCache,"Görüş önbelleğe kaydedildi")

                            }
                            else {
                                // Görüş firebase'de yok, yeni görüş oluşturuluyor...

                                setFirebaseLike(holder)

                            }
                        }
                        .addOnFailureListener(likeSendFail)

                }
                else {
                    // Post firebasede mevcut değil ui'dan & önbellekten siliniyor
                    deletedOrFailUserPost(holder)
                    snackbar(tagCheckLikeCache, context().getString(R.string.post_not_found),"warning",true)
                    Log.e(tagCheckLikeCache,"İçerik önbellekten silindi")
                }

            }
            .addOnFailureListener(likeSendFail)

    }

    private fun setFirebaseLike(holder: RecyclerHolder) {

        CoroutineScope(Dispatchers.Main).launch {

            val docRef = firestore
                .collection("Posts")
                .document(postList[holder.bindingAdapterPosition.safePosition()].postId)
                .collection("PostAction")
                .document(shared.getMyUserId())

            likeDialog.apply {

                setContentView(R.layout.post_like_dialog)

                window?.let {

                    it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                    it.attributes?.windowAnimations = R.style.likeDialog

                }

                setCancelable(false)


                findViewById<TextInputEditText>(R.id.txtRatingComment).apply {

                    addTextChangedListener {

                        shared.getUser()?.let {
                            if(!it.userAddComment) {

                                text?.clear()

                                Snackbar
                                    .make(window!!.decorView.rootView.findViewById(R.id.viewLikeDialog),
                                        context().getString(R.string.notification_new_comment_block),
                                        Snackbar.LENGTH_SHORT
                                    )
                                    .apply {
                                        duration = 1150
                                        animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
                                        setBackgroundTint(WorkUtil.snackbarColor(context()))
                                        setTextColor(Color.RED)
                                        isGestureInsetBottomIgnored = true
                                        widthSettings()
                                        anchorView = window!!.decorView.rootView.findViewById(R.id.viewLikeDialog)
                                        show()
                                    }

                            }
                        }

                    }

                }

                findViewById<RatingBar>(R.id.ratingPostLike).setOnRatingBarChangeListener { view, rating, _ ->
                    if(rating == 0f) view.rating = 0.5f
                }

                findViewById<AppCompatButton>(R.id.btnLikeCancel).setOnClickListener { fragmentResume.value?.let { if (it) { dismiss() } } }

                findViewById<AppCompatButton>(R.id.btnLikeSend).setOnClickListener {

                    fragmentResume.value?.let {

                        if (it) {

                            val rating = findViewById<RatingBar>(R.id.ratingPostLike).rating.toString()
                            var comment : String
                            var commentExist : Boolean

                            findViewById<TextInputEditText>(R.id.txtRatingComment).text.toString().apply {

                                when(isEmpty()) {
                                    true -> {
                                        comment = "-" // Yorum yok
                                        commentExist = false // Yorum yok
                                    }
                                    else -> {
                                        val commentSpamLineControl = this.trim().replace("\n","")

                                        when(commentSpamLineControl.isEmpty()) {
                                            true -> {
                                                comment = "-" // Yorum yok
                                                commentExist = false // Yorum yok
                                            }
                                            else -> {
                                                comment = commentSpamLineControl // Yorum var
                                                commentExist = true // Yorum var
                                            }
                                        }

                                    }
                                }

                            }

                            dismiss()

                            CoroutineScope(Dispatchers.IO).launch {

                                docRef
                                    .set(
                                        LikeDislikeModel(
                                            shared.getMyUserId(),
                                            postList[holder.bindingAdapterPosition.safePosition()].postId,
                                            postList[holder.bindingAdapterPosition.safePosition()].postUserId,
                                            rating,
                                            comment,
                                            commentExist,
                                            null)
                                    )
                                    .addOnSuccessListener {
                                        snackbar(
                                            "FeedAdaptrSetLikeServer",
                                            context().getString(R.string.likedislike_success),
                                            "info",
                                            true
                                        )
                                    }
                                    .addOnFailureListener { excep ->
                                        snackbar(
                                            "FeedAdaptrSetLikeServer",
                                            context().getString(R.string.likedislike_fail),
                                            "error",
                                            true)
                                        Log.e("FeedAdaptrSetLikeServer", excep.msg())
                                    }


                            }

                        }

                    }

                }

                fragmentResume.value?.let { if (it) { show() } }
            }

        }

    }

    //------------------------------ POST LIKE ACTION ------------------------------





    //------------------------------ LIKE/DISLIKE - COMMENT SOCIAL LIST ------------------------------

    @SuppressLint("InflateParams")
    private fun socialListener(isForLikes : Boolean) {

        // true -> Beğenileri görüntülemek için / false -> Yorumları görüntülemek için

        lastDataTime = null

        socialDataList.clear()

        socialSheetView = LayoutInflater
            .from(context().applicationContext)
            .inflate( if(isForLikes) R.layout.bottom_dialog_like else R.layout.bottom_dialog_comment , null )

        socialSheetView.apply {

            findViewById<ConstraintLayout>( if(isForLikes) R.id.constraintLikeSheet else R.id.constraintCommentSheet )
                .layoutParams.height = (getDeviceHeight(context()) / 100) * 65

            progressSheet = findViewById( if(isForLikes) R.id.progressLikeSheet else R.id.progressCommentSheet )
            recyclerSheet = findViewById( if(isForLikes) R.id.recyclerLikeSheet else R.id.recyclerCommentSheet )
            txtSheetCount = findViewById( if(isForLikes) R.id.txtLikeSheetCount else R.id.txtCommentSheetCount )

            if(isForLikes) {

                findViewById<ImageView>(R.id.imgPostAllComments).setOnClickListener {

                    socialSheetDialog.dismiss()

                    socialListener(false)

                }

            }

        }

        socialSheetDialog.apply {

            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            setContentView(socialSheetView)

            show()

        }


        // Recycler adapter ile bağlanıyor
        recyclerSheet.apply {

            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context())
            adapter = when(isForLikes) {
                true -> LikeRecyclerAdapter(context(), shared, socialSheetDialog, socialSheetView, Navigation.findNavController(recyclerAdapter))
                else -> CommentRecyclerAdapter(context(), shared, socialSheetDialog, socialSheetView, Navigation.findNavController(recyclerAdapter))
            }

            clearOnScrollListeners()

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if(newState == 2 && !recyclerView.canScrollVertically(1)){

                        if(progressSheet.visibility == View.GONE) socialDownload(isForLikes, true)

                    }
                }
            })

        }


        socialDownload(isForLikes, false)

    }

    private fun socialDownload (isForLikes : Boolean, pagination : Boolean) {

        recyclerSheet.show()

        val tagSocialDownload = "FeedAdptrSocialDownload"
        val actionCollection = firestore
            .collection("Posts")
            .document(postList[selectedHolder.bindingAdapterPosition.safePosition()].postId)
            .collection("PostAction")

        val query : Query = when(isForLikes) {
            true -> when(!pagination) {
                    true -> actionCollection.orderBy("actionDate", Query.Direction.DESCENDING)
                    else -> actionCollection.orderBy("actionDate", Query.Direction.DESCENDING).startAfter(lastDataTime)
            }
            else -> when(!pagination) {
                    true -> actionCollection
                        .whereEqualTo("postCommentExist", true)
                        .orderBy("actionDate", Query.Direction.DESCENDING)
                    else -> actionCollection
                        .whereEqualTo("postCommentExist", true)
                        .orderBy("actionDate", Query.Direction.DESCENDING)
                        .startAfter(lastDataTime)
                }
        }

        CoroutineScope(Dispatchers.IO).launch {

            query
                .limit(5)
                .get(Source.SERVER)
                .addOnSuccessListener {

                    if(it.dataAvailable()) {

                        CoroutineScope(Dispatchers.Default).launch {

                            for (document : DocumentSnapshot in it.documents) {

                                socialDataList.add(document.toLikeDislikeModel())

                                lastDataTime = document.toLikeDislikeModel().actionDate

                                if(document == it.documents.last()) {

                                    withContext(Dispatchers.Main) {

                                        // User indirme bilgisi işlendi ve progressbar işlendi
                                        when(isForLikes) {
                                            true -> (recyclerSheet.adapter as LikeRecyclerAdapter).setData(socialDataList)
                                            else -> (recyclerSheet.adapter as CommentRecyclerAdapter).setData(socialDataList)
                                        }

                                        progressSheet.gone()

                                        txtSheetCount.text = socialDataList.size.toString()
                                        txtSheetCount.show()

                                        Log.i(tagSocialDownload,"Like/dislike social yüklendi")

                                    }

                                }

                            }

                        }

                    }

                    else {
                        // Yeni kullanıcı indirilemiyor...

                        CoroutineScope(Dispatchers.Main).launch {
                            progressSheet.gone()

                            val msg = when(socialDataList.size > 0) {
                                true -> context().getString(R.string.more_user_fail)
                                false -> context().getString(R.string.user_not_exists)
                            }
                            // Eğer liste komple boş ise hiç kullanıcı yok, liste dolu ise başka kullanıcı yok ...

                            Log.i(tagSocialDownload, msg)

                            Snackbar
                                .make(context(), socialSheetView.rootView, msg, Snackbar.LENGTH_SHORT)
                                .settings()
                                .widthSettings()
                                .setGestureInsetBottomIgnored(true)
                                .setAnchorView(when(isForLikes) {
                                    true -> socialSheetView.rootView.findViewById(R.id.viewLikeSnackbar)
                                    else -> socialSheetView.rootView.findViewById(R.id.viewCommentSnackbar)
                                })
                                .show()

                        }

                    }

                }
                .addOnFailureListener {
                    CoroutineScope(Dispatchers.Main).launch { progressSheet.gone() }
                    snackbar(
                        tagSocialDownload,
                        context().getString(R.string.user_fail),
                        "error",
                        false)
                }

        }

    }

    //------------------------------ LIKE/DISLIKE - COMMENT SOCIAL LIST ------------------------------





    //------------------------------ POST MORE ACTION (REFRESH/SAVE/DELETE/SHARE/REPORT) ------------------------------

    @SuppressLint("InflateParams")
    private fun bottomSheet() {

        optionSheetView = LayoutInflater.from(context().applicationContext).inflate(R.layout.bottom_dialog_post_option,null)

        bottomSheetOption() // BottomSheet seçenekleri düzenleniyor

        optionSheetView.apply {

            findViewById<TextView>(R.id.txtSheetListen).setOnClickListener(bottomSheetListenClick())

            findViewById<TextView>(R.id.txtSheetRefresh).setOnClickListener(bottomSheetRefreshClick())

            findViewById<TextView>(R.id.txtSheetFavorite).setOnClickListener(bottomSheetSaveOrDeleteClick())

            findViewById<TextView>(R.id.txtSheetDelete).setOnClickListener(bottomSheetDeleteClick())

            findViewById<TextView>(R.id.txtSheetShare).setOnClickListener(bottomSheetShareClick())

            findViewById<TextView>(R.id.txtSheetReport).setOnClickListener(bottomSheetReportClick())

            // ClickListener eklendi...

        }

        optionSheetDialog.apply {

            setContentView(optionSheetView) // View eklendi

            show() // BottomSheet açıldı

        }

    }

    private fun bottomSheetOption() {

        when(postList[selectedHolder.bindingAdapterPosition.safePosition()].postUserId == shared.getMyUserId()){
            true -> {
                /**
                Eğer kullanıcı Feed'de kendi postunu görüntülüyor ise;
                Yenile - Favorilere ekle - Raporla seçenekleri gizleniyor.
                */
                optionSheetView.findViewById<TextView>(R.id.txtSheetReport).gone()
            }

            false -> {
                // Eğer kendi postunu görüntülemiyor ise Sil seçeneği gizleniyor.
                optionSheetView.findViewById<TextView>(R.id.txtSheetDelete).gone()

            }
        }

        // Post daha önce veritabanına kaydedildi mi ?
        CoroutineScope(Dispatchers.IO).launch {

            val db = Room
                .databaseBuilder(context().applicationContext, FikirzademDatabase::class.java,"FikirzademDB")
                .build()

            val data = db.fikirzademDao().getSavedPost(shared.getMyUserId(),
                postList[selectedHolder.bindingAdapterPosition.safePosition()].postId
            )

            withContext(Dispatchers.Main) {

                optionSheetView.findViewById<TextView>(R.id.txtSheetFavorite).apply {

                    // Item text : post veritabanında varsa "Kaldır" yok ise "Kaydet"
                    text = context().getString(if(data != null) R.string.remove else R.string.save)

                    // Item image : post veritabanında varsa "Dolu kalp" yok ise "Boş kalp"
                    setCompoundDrawablesWithIntrinsicBounds(null,
                        resDrawable(
                            context(), when(data != null) {
                                true -> R.drawable.bottom_sheet_ico_delete_fav
                                else -> R.drawable.bottom_sheet_ico_save
                            }
                        ), null, null)

                }

            }

            db.close()

        }


        // Post şuanda dinleniyor mu ?
        speechText?.let {

            if(it.contains(selectPostContent) && textToSpeech.isSpeaking) {

                optionSheetView.findViewById<TextView>(R.id.txtSheetListen).apply {

                    text = context().getString(R.string.stop)

                    setCompoundDrawablesWithIntrinsicBounds(
                        null,
                        resDrawable(context(), R.drawable.bottom_sheet_ico_stop),
                        null,
                        null
                    )

                }

            }

        }

    }

    private fun bottomSheetClose() {

        CoroutineScope(Dispatchers.Main).launch {

            if(optionSheetDialog.isShowing){ optionSheetDialog.dismiss() }

        }

    }

    private fun bottomSheetListenClick() : View.OnClickListener {

        return View.OnClickListener {

            bottomSheetClose()

            when(optionSheetView
                    .findViewById<TextView>(R.id.txtSheetListen)
                    .text.toString().matches(Regex(context().getString(R.string.stop)))) {
                true -> if(textToSpeech.isSpeaking) textToSpeech.stop()
                else -> {

                    CoroutineScope(Dispatchers.Default).launch {

                        try {

                            textToSpeech.apply {

                                setSpeechRate(0.8f)

                                setPitch(0.9f)

                                speak(
                                    when(!selectUserName.matches(Regex(emptyText)) && !selectPostContent.matches(Regex(emptyText))) {
                                        true ->  "$selectUserName, $selectPostContent"
                                        else -> context().getString(R.string.error) },
                                    TextToSpeech.QUEUE_ADD,
                                    null,
                                    selectPostContent
                                )

                            }

                            Log.i("FeedAdapterSheetListen", "İçerik okutma başarılı")
                        }
                        catch (e: Exception) { Log.e("FeedAdapterSheetListen", e.msg()) }

                    }

                }
            }

        }

    }

    private fun bottomSheetRefreshClick() : View.OnClickListener {

        return View.OnClickListener {

            val tagSheetRefresh = "FeedAdapterSheetRefresh"

            bottomSheetClose()

            CoroutineScope(Dispatchers.IO).launch {

                // Post refresh ile yenileniyor

                val postFirebaseRef = firestore
                    .collection("Posts")
                    .document(postList[selectedHolder.bindingAdapterPosition.safePosition()].postId)

                val userFirebaseRef = firestore
                    .collection("Users")
                    .document(postList[selectedHolder.bindingAdapterPosition.safePosition()].postUserId)

                postFirebaseRef
                    .get(Source.SERVER)
                    .addOnSuccessListener { post ->

                        if(post.exists()){

                            Log.i(tagSheetRefresh,"İçerik Firebasede mevcut")

                            // Kullanıcı kendi içeriğini yenilemeye çalışıyor
                            if(postList[selectedHolder.bindingAdapterPosition.safePosition()].postUserId == shared.getMyUserId()) {

                                shared.getUser()?.let { updatePostInUi(post.toPostModel(), it) }

                            }
                            else {

                                userFirebaseRef
                                    .get(Source.SERVER)
                                    .addOnSuccessListener {  user ->

                                        when(!user.exists()) {
                                            true -> {
                                                // Firebasede kullanıcı bulunamadı
                                                // Bu yüzden user cacheten silinecek post firebaseden ve cacheten silinecek

                                                // Post Firebaseden & önbellekten siliniyor
                                                deletePostInFirebase(postList[selectedHolder.bindingAdapterPosition.safePosition()].postId)
                                                deletedOrFailUserPost(selectedHolder) // Post arayüzden siliniyor.
                                                Log.i(tagSheetRefresh,"Kullanıcı önbellekten silindi")
                                            }

                                            // Post ve user mevcut, önbellekte veriler güncelleniyor ui güncelleniyor
                                            else -> updatePostInUi(post.toPostModel(), user.toUserModel())

                                        }

                                    }
                                    .addOnFailureListener {
                                        // Post yenileme başarısız {Kullanıcı arama başarısız}
                                        snackbar(tagSheetRefresh,context().getString(R.string.post_refresh_fail),"error",true)
                                        Log.e(tagSheetRefresh,it.msg())
                                    }

                            }

                        }

                        else {
                            // Post silinmiş ise;
                            deletedOrFailUserPost(this@FeedRecyclerAdapter.selectedHolder)
                            snackbar(tagSheetRefresh, context().getString(R.string.post_not_found),"warning",true)

                        }
                    }
                    .addOnFailureListener {
                        // Post yenileme başarısız
                        snackbar(tagSheetRefresh,context().getString(R.string.post_refresh_fail),"error",true)
                        Log.e(tagSheetRefresh,"İçerik önbellekten silindi")
                        Log.e(tagSheetRefresh,it.msg())

                    }

            }

        }

    }

    @SuppressLint("LogConditional")
    private fun bottomSheetSaveOrDeleteClick() : View.OnClickListener {

        return View.OnClickListener {

            bottomSheetClose()

            val save = optionSheetView
                .findViewById<TextView>(R.id.txtSheetFavorite).text.toString()
                .matches(Regex(context().getString(R.string.save)))

            CoroutineScope(Dispatchers.IO).launch {

                try {

                    when(save) {
                        true -> {
                            val savePost = SavedPostModel(
                                postList[selectedHolder.bindingAdapterPosition.safePosition()].postId,
                                postList[selectedHolder.bindingAdapterPosition.safePosition()].postId,
                                shared.getMyUserId(),
                                postList[selectedHolder.bindingAdapterPosition.safePosition()].postCategoryId,
                                postList[selectedHolder.bindingAdapterPosition.safePosition()].postContent,
                                postList[selectedHolder.bindingAdapterPosition.safePosition()].postDate!!.seconds,
                                System.currentTimeMillis()
                            )

                            savePost.roomSavedPost(context()) // Post veritabanına kaydedildi
                        }
                        else -> {
                            val db = Room
                                .databaseBuilder(context(), FikirzademDatabase::class.java,"FikirzademDB")
                                .build()

                            db.fikirzademDao()
                                .deleteSavedPostWithId(shared.getMyUserId(),
                                    postList[selectedHolder.bindingAdapterPosition.safePosition()].postId
                                )
                            db.close()
                        }

                    }

                    snackbar("FeedAdaptor-SavePost",
                        context().getString(when(save) {
                            true -> R.string.post_save_in_favorite_room
                            else -> R.string.post_delete_in_favorite_success
                        }),
                        "info", true)

                    Log.i("FeedAdapter-SaveSheet",
                        when(save) {
                            true -> "Favori post kaydedildi"
                            else -> "Favori post veritabanından silindi"
                        }
                    )

                }
                catch (e: Exception) {
                    Log.e("FeedAdapter-SaveSheet", when(save) {
                        true -> "Favori post kaydedilemedi"
                        else -> "Favori post veritabanından silinemedi"
                    })

                    Log.e("FeedAdapter-SaveSheet", e.msg())
                }

            }

        }


    }

    private fun bottomSheetDeleteClick() : View.OnClickListener {

       return View.OnClickListener {

           bottomSheetClose()

           Snackbar
               .make(context(), groupAdapter, context().getString(R.string.post_delete_question), Snackbar.LENGTH_LONG)
               .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
               .setBackgroundTint(WorkUtil.snackbarColor(context()))
               .setTextColor(Color.LTGRAY)
               .setGestureInsetBottomIgnored(true)
               .questionSnackbar()
               .setAction(context().getString(R.string.yes)) {

                   CoroutineScope(Dispatchers.IO).launch {

                           firestore
                               .collection("Posts")
                               .document(postList[selectedHolder.bindingAdapterPosition.safePosition()].postId)
                               .get(Source.SERVER)
                               .addOnSuccessListener { postsnapshot ->

                                   if (postsnapshot.exists()) {
                                       // Post firebase mevcut
                                       // Bugların önüne geçmek için post user id, şuanki kullanıcı ile tekrar kontrol ediliyor.
                                       if (postList[selectedHolder.bindingAdapterPosition.safePosition()].postUserId == shared.getMyUserId()) {
                                           deletePostInFirebase(postList[selectedHolder.bindingAdapterPosition.safePosition()].postId) // Post siliniyor
                                           deletedOrFailUserPost(this@FeedRecyclerAdapter.selectedHolder) // Ui güncelleniyor
                                       }
                                   }
                                   else {
                                       // Post firebasede mevcut değil
                                       snackbar(
                                           "FeedAdapterSheetDelete",
                                           context().getString(R.string.post_not_found),
                                           "warning",
                                           true
                                       )
                                   }
                               }
                               .addOnFailureListener {
                                   snackbar(
                                       "FeedAdapterSheetDelete",
                                       context().getString(R.string.post_delete_in_firebase_fail),
                                       "error",
                                       true)
                                   Log.e("FeedAdapterSheetDelete", it.msg())
                               }

                   }

               }
               .setActionTextColor(Color.GREEN)
               .widthSettings()
               .show()

        }

    }

    private fun bottomSheetShareClick() : View.OnClickListener{

        return View.OnClickListener {

            bottomSheetClose()

            val contentTitle = context().getString(R.string.suggestion) // Öneri başlık
            val content : String =
                postList[selectedHolder.bindingAdapterPosition.safePosition()].postContent // Post içeriği
            val linkTitle = context().getString(R.string.formore) // Daha fazlası için başlık
            val link = "https://play.google.com/store/apps/details?id=com.basesoftware.fikirzadem" // İndirme linki

            val shareText = "$contentTitle $content\n\n$linkTitle $link"

            WorkUtil.openShareMenu(context(), shareText)

        }

    }

    @SuppressLint("InflateParams")
    private fun bottomSheetReportClick() : View.OnClickListener {

        return View.OnClickListener {

            bottomSheetClose()

            shared.getUser()?.let { user ->

                when(!user.userAddReport) {

                    true -> snackbar(
                            "FeedAdaptr-ReportClick",
                            context().getString(R.string.notification_new_report_block),
                            "error",
                            true
                        )

                    else -> Snackbar
                            .make(context(), groupAdapter, context().getString(R.string.post_refresh_for_report), Snackbar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setBackgroundTint(WorkUtil.snackbarColor(context()))
                            .setTextColor(Color.LTGRAY)
                            .setGestureInsetBottomIgnored(true)
                            .questionSnackbar()
                            .setAction(context().getString(R.string.yes)) {

                                try {
                                    reportSheetView = LayoutInflater.from(context().applicationContext).inflate(R.layout.bottom_dialog_post_report,null)

                                    reportSheetDialog.setContentView(reportSheetView) // View eklendi

                                    reportSheetView.findViewById<Button>(R.id.btnReportSheetSend).setOnClickListener {

                                        setFirebaseReport()

                                    }

                                    reportSheetDialog.show() // BottomSheet açıldı

                                    Log.i("FeedAdapter-ReportSheet","ReportSheet gösterildi")
                                }
                                catch (e: Exception) {
                                    Log.e("FeedAdapter-ReportSheet",e.msg())
                                    Log.e("FeedAdapter-ReportSheet","ReportSheet gösterilemedi")
                                }

                            }
                            .setActionTextColor(Color.GREEN)
                            .widthSettings()
                            .show()
                }

            }

        }

    }

    //------------------------------ POST MORE ACTION (REFRESH/SAVE/DELETE/SHARE/REPORT) ------------------------------





    //------------------------------ POST REPORT ACTION ------------------------------

    private fun reportSheetClose() {
        CoroutineScope(Dispatchers.Main).launch {
            if(reportSheetDialog.isShowing) { reportSheetDialog.dismiss() }
        }
    }

    private fun setFirebaseReport() {

        reportSheetClose()

        CoroutineScope(Dispatchers.Main).launch {

            val tagSetReportFirebase = "FeedAdptrSetReportServr"

            var reportCategory = 0
            var reportContent = "Post - "
            var reportContentDetail = postList[selectedHolder.bindingAdapterPosition.safePosition()].postContent

            when(reportSheetView.findViewById<RadioGroup>(R.id.radioGroupReportSheet).checkedRadioButtonId) {
                R.id.radioSheetPost -> {
                    reportCategory = 0
                    reportContent += "Uygunsuz İçerik"
                }
                R.id.radioSheetWrongPost -> {
                    reportCategory = 1
                    reportContent += "Hatalı İçerik"
                }
                R.id.radioSheetSpam -> {
                    reportCategory = 2
                    reportContent += "Spam İçerik"
                }
                R.id.radioSheetCategory -> {
                    reportCategory = 3
                    reportContent += "Yanlış Kategori"
                    reportContentDetail = context()
                        .resources
                        .getStringArray(R.array.arrayCategoryText)[postList[selectedHolder.bindingAdapterPosition.safePosition()]
                        .postCategoryId
                    ]
                }
            }


            val reportId = postList[selectedHolder.bindingAdapterPosition.safePosition()].postId

            val reportRef = firestore.collection("Reports").document(reportId)

            val reportModelFirebase = ReportModel(
                reportId,
                reportCategory,
                postList[selectedHolder.bindingAdapterPosition.safePosition()].postId,
                "-",
                reportContent,
                reportContentDetail,
                "post",
                shared.getMyUserId(),
                null
            ) // Firebase için report modeli


            withContext(Dispatchers.IO) {
                reportRef
                    .set(reportModelFirebase)
                    .addOnSuccessListener {

                        // Rapor önbellekte veya firebase'de mevcut değil
                        // Rapor firebase'de oluşturuldu

                        snackbar(
                            tagSetReportFirebase,
                            context().getString(R.string.report_send_success),
                            "info",
                            true
                        )

                    }
                    .addOnFailureListener {
                        snackbar(
                            tagSetReportFirebase,
                            context().getString(it.firestoreError()),
                            "error",
                            true
                        )
                        Log.e(tagSetReportFirebase, it.msg())
                    }

            }

        }

    }

    //------------------------------ POST REPORT ACTION ------------------------------





    //------------------------------ POST UI/SERVER ACTION ------------------------------

    private fun updatePostInUi(postData : PostModel, userData : UserModel) {

        CoroutineScope(Dispatchers.Main).launch {

            try {

                sourceData[selectedHolder.bindingAdapterPosition.safePosition()] = postData
                postList[selectedHolder.bindingAdapterPosition.safePosition()] = postData

                selectedHolder.binding.apply {
                    post = postData
                    user = userData
                }

                snackbar("FeedAdapterUpdateUi", context().getString(R.string.post_refresh_success),"error",true)

            }
            catch (e : Exception) {
                Log.e("FeedAdapterUpdateUi", "Post ui da güncellenemedi")
                Log.e("FeedAdapterUpdateUi", e.msg())
            }

        }
    }

    private fun deletedOrFailUserPost(holder: RecyclerHolder) { holder.binding.failPost = true }

    private fun deletePostInFirebase(postId : String) {
        
        CoroutineScope(Dispatchers.IO).launch {

            firestore
                .collection("Posts")
                .document(postId)
                .delete()
                .addOnCompleteListener {
                    when(it.isSuccessful) {
                        true -> {
                            // Post firebaseden silinyor, cacheten de silinmesi için tekrar okuma yapılıyor...
                            Log.i("FeedAdptrDeleteServer", "İçerik silindi")
                            arrayNewDeletedPost.add(postId)
                        }
                        else -> it.exception?.msg()?.let { it1 -> Log.e("FeedAdptrDeleteServer", it1) }
                    }
                }

        }

    }

    //------------------------------ POST UI/SERVER/CACHE ACTION ------------------------------





    //------------------------------ POST DATA ACTION (USER CACHE/SERVER) ------------------------------

    private fun checkUserInCache(holder: RecyclerHolder) {

        CoroutineScope(Dispatchers.IO).launch {

            val tagChkUserInCache = "FeedAdptrChkUserCache"

            firestore
                .collection("Users")
                .document(postList[holder.bindingAdapterPosition.safePosition()].postUserId)
                .get(Source.CACHE)
                .addOnCompleteListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        when(it.dataAvailable()) {
                            true -> {

                                holder.binding.user = it.result.toUserModel()

                                // Cache'te user bulundu
                                Log.i(tagChkUserInCache, "Kullanıcı önbellekten alınıyor")

                            }
                            else -> {
                                // Cache'te user bulunamadı Firestore'dan çekilecek.
                                Log.w(tagChkUserInCache, "Kullanıcı önbellekten alınamadı, Firebase gidiliyor")
                                getFirebaseUser(holder)
                            }
                        }
                    }
                }

        }

    }

    private fun getFirebaseUser(holder: RecyclerHolder) {

        CoroutineScope(Dispatchers.IO).launch {

            firestore
                .collection("Users")
                .document(postList[holder.bindingAdapterPosition.safePosition()].postUserId)
                .get(Source.SERVER)
                .addOnSuccessListener {

                    if (it.exists()) {

                        CoroutineScope(Dispatchers.Main).launch {

                            /**
                             * Eğer; kullanıcı, sunucudan indirilenler listesinde yer almıyor ise listeye ekleniyor
                             * Böylece bir sonraki sefer önbellekten alınacak
                             * Recyclerview aşağı yukarı scroll edilişinde sürekli onBindViewHolder çalışıyor
                             * Önbellek sistemi kullanılmaz ise sürekli sunucudan kullanıcı indirilecek
                             */
                            if(!arrayUsers.contains(postList[holder.bindingAdapterPosition.safePosition()].postUserId)) {
                                arrayUsers.add(postList[holder.bindingAdapterPosition.safePosition()].postUserId)
                            }

                            holder.binding.user = it.toUserModel()

                            Log.i("FeedAdptrGetFirebaseUsr", "Kullanıcı firebaseden alındı")

                        }

                    }

                    else {
                        // User firebase'de bulunamadı post firebaseden ve cachten siliniyor, user cacheten siliniyor.

                        // Post firebase'den ve cache'ten siliniyor [İçeriğin alt koleksiyonları daha sonra periyodik olarak temizlenecek]
                        deletePostInFirebase(postList[holder.bindingAdapterPosition.safePosition()].postId)

                        // Post Ui'dan siliniyor...
                        deletedOrFailUserPost(holder)

                        // Kullanıcı daha önce sunucudan indirildikten sonra tekrar indirme denemesinde {Post refresh} kullanıcı bulunamadı ise indirilenler listesinden kaldır
                        if(arrayUsers.contains(postList[holder.bindingAdapterPosition.safePosition()].postUserId)) {
                            arrayUsers.remove(postList[holder.bindingAdapterPosition.safePosition()].postUserId)
                        }

                    }

                }
                .addOnFailureListener {

                    CoroutineScope(Dispatchers.Main).launch {

                        deletedOrFailUserPost(holder)

                        Log.e("FeedAdptrGetFirebaseUsr", "Kullanıcı firebaseden alınamadı")

                    }

                }
            // Ağ bağlantısının kopma ihtimaline karşı onComplete kullanılmadı.

        }

    }

    //------------------------------ POST DATA ACTION (USER CACHE/SERVER) ------------------------------


}