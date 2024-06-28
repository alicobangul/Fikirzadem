package com.basesoftware.fikirzadem.presentation.ui.profile

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.presentation.adapter.FeedRecyclerAdapter
import com.basesoftware.fikirzadem.presentation.adapter.SocialRecyclerAdapter
import com.basesoftware.fikirzadem.databinding.FragmentProfileBinding
import com.basesoftware.fikirzadem.model.*
import com.basesoftware.fikirzadem.presentation.viewmodel.ProfileViewModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.dataAvailable
import com.basesoftware.fikirzadem.util.ExtensionUtil.firestoreError
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.WorkUtil.changeFragment
import com.basesoftware.fikirzadem.util.WorkUtil.getDeviceHeight
import com.basesoftware.fikirzadem.util.ExtensionUtil.gone
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.show
import com.basesoftware.fikirzadem.util.ExtensionUtil.toPostModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.toSocialModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.toUserModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil.systemLanguage
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding : FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore : FirebaseFirestore

    private lateinit var textToSpeech : TextToSpeech

    private var snackbar : Snackbar? = null

    private lateinit var userReportSheetDialog : BottomSheetDialog
    private lateinit var userReportSheetView : View

    private lateinit var socialSheetDialog : BottomSheetDialog
    private lateinit var socialSheetView : View

    private lateinit var recyclerSocial : RecyclerView
    private lateinit var progressSocial : ProgressBar

    private val sharedViewModel : SharedViewModel by activityViewModels { SharedViewModel.provideFactory(requireActivity().application, requireActivity()) }
    private val profileViewModel : ProfileViewModel by viewModels()


    override fun onCreateView(inf: LayoutInflater, group: ViewGroup?, inst: Bundle?): View {

        _binding = FragmentProfileBinding.inflate(layoutInflater, group, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()

        viewListener()

        profileViewModel.getUpdateScreen().observe(viewLifecycleOwner, {

            binding.profile = profileViewModel

        })

        when(profileViewModel.getProfile() == null) {
            true -> initializeSettings() // Bu ekran ilk kez açılıyor akışa devam et
            false -> {
                /**
                 * Uygulama daha önce açılıp arka plana atılmış şimdi geri yüklendi
                 * Gerekli aksiyon alınıyor
                 */
                when(sharedViewModel.getShowMyProfile()) {

                    true -> getMyProfile() // Kişi en son kendi profilini inceliyordu

                    false -> {
                        // Kişi en son başkasının profilini inceliyordu
                        otherUserSocialButtonSettings()
                        userPostSettings() // Postlar getirilmeye başlandı
                        if(profileViewModel.getSocialSheetShow()) socialUserSheetSettings()
                    }

                }

            }
        }

    }

    override fun onDestroyView() {

        profileViewModel.setFragmentResume(false)

        if(userReportSheetDialog.isShowing) userReportSheetDialog.dismiss()
        if(socialSheetDialog.isShowing) socialSheetDialog.dismiss()

        textToSpeech.apply {
            stop()
            shutdown()
        }

        snackbar?.let { if(it.isShown) it.dismiss() }

        _binding = null

        super.onDestroyView()
    }

    private fun checkContext(context: Context.() -> Unit) { if (isAdded) { context(requireContext()) } }



    private fun initialize() {

        checkContext {

            textToSpeech = TextToSpeech(requireActivity().applicationContext) {
                if(it == TextToSpeech.SUCCESS) {
                    val addLangResult = textToSpeech.setLanguage(
                        when (systemLanguage().matches(Regex("tr"))) {
                            true -> Locale.forLanguageTag("tr")
                            else -> Locale.forLanguageTag("en")
                        }
                    )
                    if(addLangResult == TextToSpeech.LANG_MISSING_DATA || addLangResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech.language = Locale.US
                    }
                }
            }

            firestore = WorkUtil.firestore()

            userReportSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)

            socialSheetDialog = BottomSheetDialog(requireContext(),R.style.BottomSheetDialogTheme)

        }

    }


    private fun viewListener() {

        binding.swipeRefreshProfile.apply {
            setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(),R.color.lite)) // Progressbar arka plan rengi
            setColorSchemeResources(R.color.white) // Progressbar iç rengi

            setOnChildScrollUpCallback { _, _ ->

                /**
                 * Eğer profil sayfası aşağı scroll edilmiş ise yukarı scroll edildiğinde swipeRefreshLayout çalışmayacak
                 * Eğer profil sayfası scroll'u en tepede ise swipeRefreshLayout çalışabilir
                 */

                return@setOnChildScrollUpCallback binding.constProfileMotion.progress != 0.0f
            }

            setOnRefreshListener {

                isRefreshing = false

                if(
                    !profileViewModel.getArgumentUserId().matches(Regex("null")) &&
                    !sharedViewModel.getLastRefreshProfile().matches(Regex(profileViewModel.getArgumentUserId()))
                ) {

                    sharedViewModel.setLastRefreshProfile(profileViewModel.getArgumentUserId())

                    profileViewModel.clearUserPostList()

                    when(sharedViewModel.getShowMyProfile()) {

                        true -> getMyProfile()

                        false -> getOtherUserProfile()

                    }

                }

            }
        }


        binding.imgProfileImg.setOnClickListener {

            profileViewModel.getProfile()?.userProfilePicture?.let { pictureLink ->
                try {

                    if(!pictureLink.matches(Regex("default"))){

                        checkContext {

                            val dialog = Dialog(requireContext())

                            dialog.setContentView(R.layout.profile_picture_dialog)

                            Glide
                                .with(requireContext())
                                .setDefaultRequestOptions(WorkUtil.glideDefault(requireContext()))
                                .load(pictureLink)
                                .into(dialog.findViewById(R.id.imgProfilePictureOrg))

                            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


                            dialog.findViewById<ConstraintLayout>(R.id.constraintPpDialog).isSoundEffectsEnabled = false

                            dialog.findViewById<ConstraintLayout>(R.id.constraintPpDialog).setOnClickListener { dialog.dismiss() }

                            dialog.show()

                        }

                    }
                }
                catch (e : Exception) { Log.e("ProfileFrag-OpenPp","Profile resmi dialog kutusu gösterilemedi") }
            }
        }



        binding.linearFollower.setOnClickListener {
            if(!binding.txtFollowerCount.text.matches(Regex("0"))) socialUserSheetSettings()
        }



        binding.btnEditProfile.setOnClickListener {

            profileViewModel.setUiEnable(false) // Butonlar kapandı

            checkContext { changeFragment(findNavController(),null,R.id.editProfileFragment) } // Profil düzenlemeye gidiyor

        }

        binding.btnFollow.setOnClickListener(profileNewFollow())

        binding.btnDeleteFollow.setOnClickListener(profileUnfollow())

        binding.btnUserReport.setOnClickListener(userReport())



        val socialMediaListener = View.OnClickListener {
            val model = profileViewModel.getProfile()

            val mobileLink = when(it.id) {
                binding.imgProfileInstagram.id -> "instagram://user?username="+model?.userInstagram // Instagram app deeplink
                binding.imgProfileTwitter.id -> "twitter://user?screen_name="+model?.userTwitter // Twitter app deeplink
                binding.imgProfileFacebook.id -> "fb://facewebmodal/f?href=http://www.facebook.com/"+model?.userFacebook // Facebook app deeplink
                else -> "-"
            }

            val webLink = when(it.id) {
                binding.imgProfileInstagram.id -> "http://instagram.com/_u/"+model?.userInstagram // Instagram web link
                binding.imgProfileTwitter.id -> "https://twitter.com/#!/"+model?.userTwitter // Twitter web link
                binding.imgProfileFacebook.id -> "http://www.facebook.com/"+model?.userFacebook // Facebook web link
                else -> "-"
            }

            try
            {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mobileLink)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            catch (e : ActivityNotFoundException)
            {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webLink)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }

        }

        binding.imgProfileInstagram.setOnClickListener(socialMediaListener)

        binding.imgProfileTwitter.setOnClickListener(socialMediaListener)

        binding.imgProfileFacebook.setOnClickListener(socialMediaListener)


        binding.recyclerProfile.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if(binding.constraintLayout.visibility == View.GONE && newState == 2 && !recyclerView.canScrollVertically(1)) {

                    if(
                        when(profileViewModel.getUserPostList().isNullOrEmpty()) {
                            true -> binding.progressProfileLoading.visibility != View.VISIBLE
                            else -> binding.progressProfileDownload.visibility != View.VISIBLE
                        }
                    ) {

                        downloadUserPost(true)

                    }
                }
            }

        })


    }

    private fun initializeSettings() {

        checkContext {

            if(fragmentContinue()) {

                profileViewModel.setArgumentUserId(requireArguments().getString("userId","null"))
                // Argüman boş değil, gelen userId alınıyor

                when(profileViewModel.getArgumentUserId() == sharedViewModel.getMyUserId()){

                    true -> {
                        // Kullanıcı kendi profilini görüntülüyor
                        sharedViewModel.setShowMyProfile(true)
                        profileViewModel.setMyProfile(true)
                        getMyProfile()
                    }

                    false -> {
                        // Kullanıcı başkasının profilini görüntülüyor
                        sharedViewModel.setShowMyProfile(false)
                        profileViewModel.setMyProfile(false)
                        getOtherUserProfile() // Kullanıcı profili indirme işlemlerine başlandı
                    }

                }

            }

            else{ profileErrorGoFeed() } // Argüman veya userId boş feed gidiyor

        }

    }

    private fun fragmentContinue() : Boolean {

        return when(arguments != null && !(requireArguments().getString("userId","null").matches(Regex("null")))) {
            // Eğer argüman boş değilse ve dolu gelen argüman içerisindeki userId null değilse
            true -> true

            false -> false
        }

    }

    private fun profileErrorGoFeed(){

        CoroutineScope(Dispatchers.Main).launch {

            checkContext {

                Log.e("ProfileFrag-ErrorGoFeed","Profile fragment, boş USERID")

                Snackbar
                    .make(requireContext(),binding.root, getString(R.string.profile_error), Snackbar.LENGTH_SHORT)
                    .settings()
                    .widthSettings()
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            checkContext { changeFragment(findNavController(),null,R.id.feedFragment) }
                        }
                    })
                    .show()

            }

        }

    }



    @SuppressLint("LogConditional")
    private fun logSnackbar(tag : String, msg : Int, type: String, inFragment : Boolean, action : Snackbar.Callback?) {

        CoroutineScope(Dispatchers.Main).launch {

            checkContext {

                when(type) {
                    "error" -> Log.e(tag, getString(msg))

                    "warning" -> Log.w(tag, getString(msg))

                    "info" -> Log.i(tag, getString(msg))
                }

                val view = when(inFragment){
                    true -> binding.root // Snackbar fragment içerisinde gösteriliyor
                    false -> socialSheetView.rootView // Snackbar BottomDialog'da gösteriliyor
                }

                snackbar = if(action == null){

                    Snackbar
                        .make(requireContext(), view, getString(msg), Snackbar.LENGTH_SHORT)
                        .settings()
                        .widthSettings()
                        .setGestureInsetBottomIgnored(true)
                }
                else {
                    // Dismissed aksiyonu var
                    Snackbar
                        .make(requireContext(), view, getString(msg), Snackbar.LENGTH_SHORT)
                        .settings()
                        .widthSettings()
                        .setGestureInsetBottomIgnored(true)
                        .addCallback(action)
                }

                snackbar?.let {

                    if(!inFragment) { it.anchorView = socialSheetView.rootView.findViewById(R.id.viewSocialSnackbar) }

                    it.show()

                }

            }

        }

    }



    private fun profileNewFollow() : View.OnClickListener{
        return View.OnClickListener {

            val tagNewFollow = "ProfileFrag-NewFollow"

            profileViewModel.setUiEnable(false) // Butonlar kapandı

            if(sharedViewModel.getLastFollow().matches(Regex(profileViewModel.getArgumentUserId()))) {

                logSnackbar(tagNewFollow, R.string.user_last_follow, "info", true, null)

                profileViewModel.setUiEnable(true) // Butonlar açıldı

            }

            else {

                // Kullanıcının profil referansı ve kullanıcının takip ettikleri koleksiyonunun referansı
                val myProfileRef = firestore
                    .collection("Users")
                    .document(sharedViewModel.getMyUserId())
                val myProfileFollowingRef = firestore
                    .collection("Users")
                    .document(sharedViewModel.getMyUserId())
                    .collection("UserFollowing")
                    .document(profileViewModel.getArgumentUserId())

                // Hedef kullanıcının referansı ve hedef kullanıcının takipçileri koleksiyonunun referansı
                val argumentProfileRef = firestore
                    .collection("Users")
                    .document(profileViewModel.getArgumentUserId())
                val argumentProfileFollowerRef = firestore
                    .collection("Users")
                    .document(profileViewModel.getArgumentUserId())
                    .collection("UserFollower")
                    .document(sharedViewModel.getMyUserId())

                firestore.runTransaction {

                    // Following action kullanıcı için modellendi
                    val following = SocialModel(profileViewModel.getArgumentUserId(),"following",null)

                    // Follower action hedef kullanıcı için modellendi
                    val follower = SocialModel(sharedViewModel.getMyUserId(),"follower",null)

                    it.set(myProfileFollowingRef, following) // Following modeli yazıldı
                    it.set(argumentProfileFollowerRef, follower) // Follower modeli yazıldı

                    it.update(myProfileRef, "userFollowing", FieldValue.increment(1)) // Following 1 artırma yazıldı
                    it.update(argumentProfileRef, "userFollower", FieldValue.increment(1)) // Follower 1 artırma yazıldı

                }
                    .addOnSuccessListener {

                        if(!binding.txtFollowerCount.text.matches(Regex("-"))){
                            // Kullanıcın bilgileri okununca takipçi sayısı yazılmış

                            val model : UserModel = profileViewModel.getProfile()!!
                            model.userFollower = model.userFollower + 1
                            profileViewModel.setProfile(model)
                            // Ekranda takipçi sayısı 1 artırıldı

                        }

                        profileViewModel.setNowFollow(true)
                        profileViewModel.setUpdateScreen(true)
                        sharedViewModel.setLastFollow(profileViewModel.getArgumentUserId())

                        logSnackbar(tagNewFollow, R.string.user_follow_success, "info", true, null)

                        profileViewModel.setUiEnable(true) // Butonlar açıldı

                    }
                    .addOnFailureListener {
                        logSnackbar(tagNewFollow, R.string.user_follow_fail, "error", true, null)
                        Log.e(tagNewFollow, it.msg())
                        profileViewModel.setUiEnable(true) // Butonlar açıldı
                    }

            }

        }
    }

    private fun profileUnfollow() : View.OnClickListener {

        return View.OnClickListener {

            profileViewModel.setUiEnable(false) // Butonlar kapandı
            val tagUnfollow = "ProfileFrag-Unfollow"

            // Kullanıcının profil referansı ve kullanıcının takip ettikleri koleksiyonunun referansı
            val myProfile = firestore
                .collection("Users")
                .document(sharedViewModel.getMyUserId())

            val myProfileFollowing = firestore
                .collection("Users")
                .document(sharedViewModel.getMyUserId())
                .collection("UserFollowing")
                .document(profileViewModel.getArgumentUserId())

            // Hedef kullanıcının referansı ve hedef kullanıcının takipçileri koleksiyonunun referansı
            val argumentProfile = firestore
                .collection("Users")
                .document(profileViewModel.getArgumentUserId())
            val argumentProfileFollower = firestore
                .collection("Users")
                .document(profileViewModel.getArgumentUserId())
                .collection("UserFollower")
                .document(sharedViewModel.getMyUserId())


            firestore
                .runTransaction {

                    it.delete(myProfileFollowing) // Kullanıcının takip edilen tablosundan silindi
                    it.delete(argumentProfileFollower) // Profili incelenen kullanıcının takipçi tablosundan silindi

                    it.update(myProfile, "userFollowing", FieldValue.increment(-1)) // Following 1 azaltma yazıldı
                    it.update(argumentProfile, "userFollower", FieldValue.increment(-1)) // Follower 1 azaltma yazıldı

                }
                .addOnSuccessListener {

                    if(!binding.txtFollowerCount.text.matches(Regex("-"))){
                        // Kullanıcın bilgileri okununca takipçi sayısı yazılmış

                        val model : UserModel = profileViewModel.getProfile()!!
                        if(model.userFollower > 0) {
                            model.userFollower = model.userFollower - 1
                            profileViewModel.setProfile(model)
                            // Ekranda takipçi sayısı 1 azaltıldı { min. 0 yazacak -1 engelleniyor (bug ihtimali)}
                        }

                    }

                    profileViewModel.apply {
                        setNowUnfollow(true)
                        setUpdateScreen(true)
                    }


                    logSnackbar(tagUnfollow, R.string.user_unfollow_success, "info", true, null)

                    firestore
                        .collection("Users")
                        .document(sharedViewModel.getMyUserId())
                        .collection("UserFollowing")
                        .document(profileViewModel.getArgumentUserId())
                        .get(Source.SERVER)

                    profileViewModel.setUiEnable(true) // Butonlar açıldı

            }
                .addOnFailureListener {
                    logSnackbar(tagUnfollow, R.string.user_unfollow_fail, "error", true, null)
                    Log.e(tagUnfollow, it.msg())
                    profileViewModel.setUiEnable(true) // Butonlar açıldı
            }

        }
    }

    @SuppressLint("InflateParams")
    private fun userReport() : View.OnClickListener{
        return View.OnClickListener {

            sharedViewModel.getUser()?.let { user ->
                when(!user.userAddReport) {

                    true -> logSnackbar(
                            "ProfileFrag-UserReport",
                            R.string.notification_new_report_block,
                            "error",
                            true,
                            null
                        )

                    else -> checkContext {

                        userReportSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_dialog_user_report,null)
                        userReportSheetView.findViewById<Button>(R.id.btnReportSend).setOnClickListener {
                            newUserReport() // Daha önce raporlandı mı önbellek kontrol ediliyor
                            if (userReportSheetDialog.isShowing) { userReportSheetDialog.dismiss() }
                        }

                        userReportSheetDialog.apply {
                            setContentView(userReportSheetView) // View eklendi
                            show() // BottomSheet açıldı
                        }

                    }

                }
            }

        }
    }





    private fun newUserReport() {

        CoroutineScope(Dispatchers.Main).launch {

            val tagNewReport = "ProfileFragSetNewReport"

            var category = 0
            var content = "User - "
            var contentDetail = "-"

            when(userReportSheetView.findViewById<RadioGroup>(R.id.radioGroupUserReportSheet).checkedRadioButtonId){
                R.id.radioSheetName -> {
                    category = 0
                    content += "Uygunsuz İsim"
                    contentDetail = binding.profile?.getProfile()?.userRealName ?: "-"
                }
                R.id.radioSheetUserName -> {
                    category = 1
                    content += "Uygunsuz Kullanıcıadı"
                    contentDetail = binding.profile?.getProfile()?.userName ?: "-"
                }
                R.id.radioSheetBiyography -> {
                    category = 2
                    content += "Uygunsuz Biyografi"
                    contentDetail = binding.profile?.getProfile()?.userBiography ?: "-"
                }
                R.id.radioSheetPp -> {
                    category = 3
                    content += "Uygunsuz Fotoğraf"
                    contentDetail = binding.profile?.getProfile()?.userProfilePicture ?: "-"
                }
                R.id.radioSheetProfile -> {
                    category = 4
                    content += "Hatalı Profil"
                }
            }


            val reportId = profileViewModel.getArgumentUserId()

            val sendReportRef = firestore.collection("Reports").document(reportId)

            val reportModel = ReportModel(
                reportId,
                category,
                profileViewModel.getArgumentUserId(),
                "-",
                content,
                contentDetail,
                "user",
                sharedViewModel.getMyUserId(),
                null
            ) // Firebase için report modeli

            withContext(Dispatchers.IO) {

                sendReportRef
                    .set(reportModel)
                    .addOnSuccessListener {
                        // Rapor önbellekte veya firebase'de mevcut değil
                        // Rapor firebase'de oluşturuldu

                        profileViewModel.setNowReport(true)
                        profileViewModel.setUpdateScreen(true)

                        logSnackbar(tagNewReport, R.string.report_send_success, "info", true, null)

                    }
                    .addOnFailureListener {
                        logSnackbar(tagNewReport, it.firestoreError(), "error", true, null)
                        Log.e(tagNewReport, it.msg())
                    }

            }

        }

    }




    private fun getMyProfile() {

        profileViewModel.apply {
            clearUserPostList()
            clearSocialUserList()
            setUiEnable(true)
        }

        sharedViewModel.getUserLive().observe(viewLifecycleOwner, {

            profileViewModel.apply {
                setProfile(it)
                setUpdateScreen(true)
            }

        })

        userPostSettings() // Kullanıcı postları getiriliyor

    }

    private fun getOtherUserProfile() {

        CoroutineScope(Dispatchers.IO).launch {

            val tagOtherProfile = "ProfileFragOtherProfile"

            val goFeedCallback = object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    checkContext { changeFragment(findNavController(), null, R.id.feedFragment) }
                }
            }

            firestore
                .collection("Users")
                .document(profileViewModel.getArgumentUserId())
                .get(Source.SERVER)
                .addOnSuccessListener {
                    if (it.exists()) {

                        if(!it.toUserModel().userIsActive){
                            // Kullanıcı kaydı var ama isActive false yapılmış silme emri verilmiş
                            logSnackbar(tagOtherProfile, R.string.user_deleted, "warning", true, goFeedCallback)
                        }
                        else {

                            // Eğer kullanıcı silinmediyse
                            profileViewModel.setUiEnable(true)// Veri doğrulandığı için ui açıldı

                            // Eğer kullanıcı kendi profilini görüntülemiyor ise Takip Et - Takibi Bırak - Raporla butonlarını ayarla
                            otherUserSocialButtonSettings()

                            val user = it.toUserModel()

                            profileViewModel.apply {
                                setProfile(user) // Bilgiler yazdırılıyor
                                setUpdateScreen(true)
                                clearUserPostList()
                                clearSocialUserList()
                            }

                            userPostSettings() // Kullanıcı postları getiriliyor

                        }

                    }
                    else {
                        // Kullanıcı silindi ise

                        logSnackbar(tagOtherProfile, R.string.user_deleted, "warning", true, goFeedCallback)
                    }
                }
                .addOnFailureListener {
                    Log.e(tagOtherProfile, "Kullanıcı firebaseden alınamadı")
                    Log.e(tagOtherProfile, it.msg())

                    logSnackbar(tagOtherProfile, R.string.error, "error", true, goFeedCallback)
                }

        }

    }




    private fun userPostSettings() {

        checkContext {

            binding.recyclerProfile.apply {
                setHasFixedSize(true)
                layoutManager = when (sharedViewModel.getStaggeredLayout()) {
                    true -> StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                    else -> LinearLayoutManager(requireContext())

                }
                adapter = FeedRecyclerAdapter(
                    sharedViewModel,
                    textToSpeech,
                    profileViewModel.getUserPostList(),
                    profileViewModel.getFragmentResume(),
                    "profile"
                )

                adapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            }

            when(profileViewModel.getUserPostList().isNullOrEmpty()) {
                true ->  downloadUserPost()
                else -> (binding.recyclerProfile.adapter as FeedRecyclerAdapter).setData(profileViewModel.getUserPostList())
            }

        }

    }

    private fun downloadUserPost(pagination: Boolean = false) {

        val tagPostNoCache = "ProfileFragPostNoCache"

        progress(true)

        val query = when(pagination) {

            true -> firestore
                .collection("Posts")
                .whereEqualTo("postUserId", profileViewModel.getArgumentUserId())
                .orderBy("postDate", Query.Direction.DESCENDING)
                .startAfter(profileViewModel.getUserPostList().last().postDate)

            else -> firestore
                .collection("Posts")
                .whereEqualTo("postUserId", profileViewModel.getArgumentUserId())
                .orderBy("postDate", Query.Direction.DESCENDING)

        }

        CoroutineScope(Dispatchers.IO).launch {

            query
                .limit(5)
                .get(Source.SERVER)
                .addOnSuccessListener { userPost ->

                    if(userPost.dataAvailable()) {

                        CoroutineScope(Dispatchers.Default).launch {

                            for (document : DocumentSnapshot in userPost.documents) {

                                profileViewModel.setUserPost(document.toPostModel())

                                if(document == userPost.documents.last()) {
                                    withContext(Dispatchers.Main) {
                                        checkContext {

                                            // Datalar eklendi
                                            (binding.recyclerProfile.adapter as FeedRecyclerAdapter).setData(
                                                profileViewModel.getUserPostList()
                                            )

                                            progress(false)

                                            Log.i(tagPostNoCache, "İçerik yüklendi")

                                        }
                                    }
                                }

                            }

                        }

                    }

                    else {
                        // Yeni içerik indirilemiyor

                        CoroutineScope(Dispatchers.Main).launch {
                            progress(false)

                            val exists = when(profileViewModel.getUserPostList().size > 0) {
                                true -> R.string.old_post_fail
                                false -> R.string.post_not_exists
                            }

                            // Eğer liste komple boş ise hiç içerik yok, liste dolu ise başka içerik yok ...

                            logSnackbar(tagPostNoCache, exists, "info", true, null)

                        }

                    }

                }
                .addOnFailureListener {
                    progress(false)
                    logSnackbar(tagPostNoCache, R.string.post_profile_feed_download_fail, "error", true, null)
                    Log.e(tagPostNoCache, it.msg())
                }

        }

    }




    private fun progress(downloading : Boolean) {

        /**
         * Post indirmesi denemesi yapıyorsa ortadaki mavi progress gizleniyor - alttaki beyaz progress gösteriliyor
         * Post indirme denemesi bitti bittiğinde alttaki beyaz progress gizleniyor
         * Eğer indirme denemesi sonrası içerik bulunmazsa ortadaki mavi progressbar gösteriliyor
         */

        CoroutineScope(Dispatchers.Main).launch {
            profileViewModel.apply {

                setDataDownloading(downloading)

                when(downloading) {
                    true -> setLoadingProfile(false)
                    else -> if(getUserPostList().isNullOrEmpty()) setLoadingProfile(true)
                }

                binding.apply {

                    profile = profileViewModel

                    if(hasPendingBindings()) executePendingBindings()

                }

            }
        }

    }



    private fun otherUserSocialButtonSettings(){

        val tagSocialBtnSettings = "ProfileFrag-SocialBtn"

        CoroutineScope(Dispatchers.IO).launch {
            // Kullanıcı şuan başkasının profilini görüntülüyor

            // Kullanıcı incelediği profili takip ediyor mu kontrol ediliyor {SERVER-FIREBASE}


            firestore
                .collection("Users")
                .document(sharedViewModel.getMyUserId())
                .collection("UserFollowing")
                .document(profileViewModel.getArgumentUserId())
                .get(Source.SERVER)
                .addOnSuccessListener {

                    profileViewModel.setSocialComplete(true)

                    CoroutineScope(Dispatchers.Main).launch {

                        when (it.exists()) {
                            // Sunucuda veri bulundu kullanıcı bu profili takip ediyor {unfollow açıldı}
                            true -> profileViewModel.setFollow(true)

                            // Sunucuda veri bulunamadı kullanıcı bu profili takip etmiyor {follow açıldı}
                            false -> profileViewModel.setFollow(false)

                        }

                        profileViewModel.setUpdateScreen(true)

                    }

                }
                .addOnFailureListener {
                    // Önbellekte ve sunucuda veri okuma başarısız takip et ve takibi bırak butonları görünmez durumda

                    Log.e(tagSocialBtnSettings, "Kullanıcının UserFollower tablosunu okuma başarısız")
                    Log.e(tagSocialBtnSettings, it.msg())

                    /**
                     * Profil hatası diyerek anasayfaya yönlendir
                     * Sebep: Görüntülenen profil takip ediliyor mu kontrol edilemedi
                     * Eğer takip et vb butona basılır ise buga yol açabilir..
                     */
                    profileErrorGoFeed()
                }

        }
    }



    @SuppressLint("InflateParams")
    private fun socialUserSheetSettings() {

        checkContext {

            profileViewModel.getSocialUserList().clear()

            socialSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_dialog_social,null)

            socialSheetView.findViewById<TextView>(R.id.txtSocialSheetTitle).text = getString(R.string.userFollower)

            progressSocial = socialSheetView.findViewById(R.id.progressSocialSheet)
            recyclerSocial = socialSheetView.findViewById(R.id.recyclerSocialSheet)


            val height = (getDeviceHeight(requireContext()) / 100) * 60

            socialSheetDialog.setContentView(socialSheetView)

            socialSheetView.findViewById<ConstraintLayout>(R.id.constraintSocialSheet).layoutParams.height = height

            socialSheetDialog.apply {

                setOnShowListener { profileViewModel.setSocialSheetShow(true) }

                setOnDismissListener { profileViewModel.setSocialSheetShow(false) }

                show()

            }

            // Recycler adapter ile bağlanıyor
            recyclerSocial.apply {

                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = SocialRecyclerAdapter(
                    sharedViewModel,
                    socialSheetDialog,
                    findNavController(),
                    profileViewModel.getArgumentUserId()
                )

                addOnScrollListener(object : RecyclerView.OnScrollListener(){
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)

                        if(newState == 2){

                            if(!recyclerView.canScrollVertically(1)){

                                if(progressSocial.visibility == View.GONE) {

                                    downloadSocialUserFromServer(true)

                                }
                            }

                        }

                    }
                })

            }

            when(profileViewModel.getSocialUserList().isNullOrEmpty()) {
                true -> downloadSocialUserFromServer()
                else -> (recyclerSocial.adapter as SocialRecyclerAdapter).setData(profileViewModel.getSocialUserList())
            }

        }

    }

    private fun downloadSocialUserFromServer(pagination : Boolean = false) {

        recyclerSocial.show()

        val tagSocialDownload = "ProfileFragSocialDown"

        val query = when(pagination) {

            true -> firestore
                .collection("Users")
                .document(profileViewModel.getArgumentUserId())
                .collection("UserFollower")
                .orderBy("actionDate", Query.Direction.DESCENDING)
                .startAfter(profileViewModel.getSocialUserList().last().actionDate)

            else -> firestore
                .collection("Users")
                .document(profileViewModel.getArgumentUserId())
                .collection("UserFollower")
                .orderBy("actionDate", Query.Direction.DESCENDING)
        }

        CoroutineScope(Dispatchers.IO).launch {

            query
                .limit(5)
                .get(Source.SERVER)
                .addOnSuccessListener { socialData ->

                    when(socialData.dataAvailable()) {

                        true -> {

                            CoroutineScope(Dispatchers.Default).launch {

                                for (document : DocumentSnapshot in socialData.documents) {

                                    profileViewModel.setSocialUser(document.toSocialModel())

                                    if(document == socialData.documents.last()) {
                                        withContext(Dispatchers.Main) {

                                            // User indirme bilgisi işlendi ve progressbar işlendi
                                            (recyclerSocial.adapter as SocialRecyclerAdapter).setData(profileViewModel.getSocialUserList())

                                            progressSocial.gone()

                                            Log.i(tagSocialDownload, "Follower/following social yüklendi")

                                        }
                                    }

                                }
                            }

                        }

                        // Yeni kullanıcı indirilemiyor...
                        else -> {

                            CoroutineScope(Dispatchers.Main).launch {

                                progressSocial.gone()

                                logSnackbar(tagSocialDownload, R.string.more_user_fail, "info", false, null)

                            }

                        }

                    }

                }
                .addOnFailureListener {

                    progressSocial.gone()

                    logSnackbar(tagSocialDownload, R.string.user_fail, "error", false,null)

                }

        }

    }


}