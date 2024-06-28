package com.basesoftware.fikirzadem.presentation.view

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.ActivityAppBinding
import com.basesoftware.fikirzadem.databinding.NavHeaderBinding
import com.basesoftware.fikirzadem.model.recycler.NotificationRecyclerModel
import com.basesoftware.fikirzadem.model.UserModel
import com.basesoftware.fikirzadem.presentation.ui.login.LoginFragment
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.questionSnackbar
import com.basesoftware.fikirzadem.util.ExtensionUtil.hideStatusBar
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.toUserModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.WorkUtil.changeFragment
import com.basesoftware.fikirzadem.util.WorkUtil.suggestion
import com.basesoftware.fikirzadem.util.WorkUtil.goMyProfile
import com.basesoftware.fikirzadem.util.WorkUtil.visitPlayStoreAppPage
import com.basesoftware.fikirzadem.presentation.viewmodel.AppActivityViewModel
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.material.navigation.NavigationView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*


@AndroidEntryPoint
class AppActivity : AppCompatActivity() {

    private var _binding : ActivityAppBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth : FirebaseAuth

    private lateinit var storage : FirebaseStorage

    private lateinit var userSnapshot : ListenerRegistration

    private lateinit var leftHeaderView : View
    private lateinit var leftHeaderBinding: NavHeaderBinding

    private lateinit var rightHeaderView : View
    private lateinit var rightHeaderBinding: NavHeaderBinding

    private lateinit var reviewManager : ReviewManager
    private lateinit var reviewInfo: ReviewInfo

    private val appActivityViewModel by viewModels<AppActivityViewModel>()
    private val sharedViewModel by viewModels<SharedViewModel> { SharedViewModel.provideFactory(application, this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
//        firstUiSettings()
//
//        checkContext {
//
//            when(sharedViewModel.getMyUserId().matches(Regex("null"))){
//
//                true -> goLogin() // Uygulama yeni açıldı veya arka plana null iken atıldı
//
//                else -> appStarting() // Kullanıcı girişinde hata yok/UserId geldi/Daha önce app açıldı/Arka plandan geri çağırıldı
//
//            }
//
//        }

    }


    override fun onConfigurationChanged(newConfig: Configuration) {

        sharedViewModel.addBannerRotateCount()

        super.onConfigurationChanged(newConfig)
    }

    override fun onResume() {
        super.onResume()

        appActivityViewModel.setBackgroundMessage(false)

        try { Handler(Looper.getMainLooper()).postDelayed({window.hideStatusBar()},1500) }

        catch (e : Exception) {Log.e("AppActivity-onResume", e.msg())}

    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    override fun onBackPressed() {

        when(navController.previousBackStackEntry != null) {
            true -> super.onBackPressed()
            else -> when(appActivityViewModel.getBackgroundMessage()) {
                true -> moveTaskToBack(true)
                else -> {
                    appActivityViewModel.setBackgroundMessage(true)

                    checkContext {

                        when(Build.VERSION.SDK_INT >= 30) {
                            true -> Toast.makeText(this, R.string.exit_text, Toast.LENGTH_SHORT).show()
                            else -> {
                                Toast(this).apply {
                                    duration = Toast.LENGTH_SHORT
                                    setGravity(Gravity.BOTTOM,0,250)
                                    view = layoutInflater.inflate(
                                        R.layout.custom_toast, findViewById(
                                            R.id.constraintCustomToast
                                        ))
                                    show()
                                }
                            }
                        }

                    }

                }
            }
        }

    }


    private fun checkContext(context: Context.() -> Unit) { if (context != null) { context(this) } }



    private fun goLogin() {

        CoroutineScope(Dispatchers.Main).launch {

            checkContext {

                if(FirebaseAuth.getInstance().currentUser != null) { FirebaseAuth.getInstance().signOut() }
                getSharedPreferences(packageName.toString(), Context.MODE_PRIVATE).edit().putBoolean("rememberMe",false).apply()
                if(this@AppActivity::userSnapshot.isInitialized) userSnapshot.remove()
                startActivity(Intent(this@AppActivity, LoginFragment::class.java))
                finish()

            }

        }

    }


    private fun bottomUiVisible(value : Boolean) {

        // Gizlemek için görünür olması, görünür yapmak için gizli olması gerek. (Spam engel)

        CoroutineScope(Dispatchers.Main).launch {

            if(sharedViewModel.getBottomMenu()) {
                // Alt menü gösterimi açık ise, gizle/göster çalışsın
                when(value) {
                    true -> {
                        if(!appActivityViewModel.getBottomVisibility()) {
                            binding.floatingBtnNewPost.show()
                            binding.bottomAppBar.performShow()
                        }
                    }
                    false -> {
                        if(appActivityViewModel.getBottomVisibility()) {
                            binding.floatingBtnNewPost.hide()
                            binding.bottomAppBar.performHide()
                        }
                    }
                }

                appActivityViewModel.setBottomVisibility(value)
            }
            else {
                // Alt menü gösterimi kapalı ise daima gizle
                binding.floatingBtnNewPost.hide()
                binding.bottomAppBar.performHide()
                appActivityViewModel.setBottomVisibility(false)
            }
        }
    }

    private fun hideKeyboard() {
        try {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(currentFocus?.windowToken,0)
        }
        catch (e : Exception) {
            Log.w("Main-hideKeyboard", e.msg())
        }
    }

    private fun addNotificationBadgeCount() {
        CoroutineScope(Dispatchers.Main).launch {

            binding.bottomNavigation.getOrCreateBadge(R.id.notificationFragment).apply {
                number += 1
                this.isVisible = true

            }

        }
    }

    private fun resetNotificationBadgeCount() {
        CoroutineScope(Dispatchers.Main).launch {

            binding.bottomNavigation.getOrCreateBadge(R.id.notificationFragment).apply {

                number = 0
                this.isVisible = false

            }

        }
    }



    private fun inAppReview(logOut : Boolean) {

        checkContext {

            reviewManager = ReviewManagerFactory.create(this)

//            // Test için fake inAppReview
//            reviewManager = FakeReviewManager(this)

            reviewManager.requestReviewFlow()
                .addOnCompleteListener { task ->

                    when(task.isSuccessful) {

                        true -> {

                            reviewInfo = task.result

                            reviewManager.launchReviewFlow(this@AppActivity, reviewInfo)
                                .addOnCompleteListener { if (logOut) goLogin() }

                        }

                        else -> if (logOut) goLogin()

                    }

                }

        }

    }



    private fun firstUiSettings() {

        checkContext {

            try {

                navController = Navigation.findNavController(this@AppActivity,
                    R.id.fragmentAllNavHost
                )
                binding.bottomNavigation.apply {
                    setupWithNavController(navController)
                    itemIconTintList = null
                    background = null
                    getOrCreateBadge(R.id.notificationFragment).apply {
                        badgeTextColor = Color.parseColor("#FFFFFF")
                        backgroundColor = Color.parseColor("#FF0000")
                        maxCharacterCount = 2
                        isVisible = false
                    }
                }
                binding.floatingBtnNewPost.apply {
                    imageTintList = null
                    background = null
                }

                val materialShape = binding.bottomAppBar.background as MaterialShapeDrawable
                materialShape.shapeAppearanceModel =
                    materialShape.shapeAppearanceModel
                        .toBuilder()
                        .setTopRightCorner(CornerFamily.ROUNDED, 50f)
                        .setBottomRightCorner(CornerFamily.ROUNDED, 50f)
                        .setTopLeftCorner(CornerFamily.ROUNDED, 50f)
                        .setBottomLeftCorner(CornerFamily.ROUNDED, 50f)
                        .build()

            }
            catch (e: Exception) { Log.e("AppAct-firstSet-Catch",e.msg()) }

        }

    }


    private fun appStarting() {

        initialize()

        listeners()

    }

    private fun initialize() {

        checkContext {

            firestore = WorkUtil.firestore()
            auth = FirebaseAuth.getInstance()
            storage = FirebaseStorage.getInstance()

            // Left Navigation Header Binding
            leftHeaderView = binding.leftNavigation.getHeaderView(0)
            leftHeaderBinding = NavHeaderBinding.bind(leftHeaderView)

            // Right Navigation Header Binding
            rightHeaderView = binding.rightNavigation.getHeaderView(0)
            rightHeaderBinding = NavHeaderBinding.bind(rightHeaderView)

        }

    }

    private fun listeners() {

        val headerListener = View.OnClickListener {

            binding.drawerMain.closeDrawers()
            if(!sharedViewModel.getShowMyProfile()) { goMyProfile(navController, sharedViewModel.getMyUserId()) }

        }

        leftHeaderView.setOnClickListener(headerListener)

        rightHeaderView.setOnClickListener(headerListener)


        checkContext {

            sharedViewModel.getUserLive().observe(this@AppActivity) {
                leftHeaderBinding.shared = sharedViewModel
                rightHeaderBinding.shared = sharedViewModel
            }

            sharedViewModel.getScrollDownLive().observe(this@AppActivity) {

                if (sharedViewModel.getBottomMenu()) bottomUiVisible(!it)

            }

        }

        val tagOnSnapshot = "Main-OnSnapshotProfile"
        userSnapshot = firestore
            .collection("Users")
            .document(sharedViewModel.getMyUserId())
            .addSnapshotListener(MetadataChanges.EXCLUDE) { value, error ->

                error?.let {
                    userError("connection")
                    Log.e(tagOnSnapshot, it.msg())
                    return@addSnapshotListener
                }

                when(value == null || !value.exists()) {
                    true -> userError("block")
                    else -> {

                        value.let {

                            // Eğer kullanıcı aktif değilse ise uzaklaştırıldı.
                            when (it.toUserModel().userIsActive) {
                                true -> {

                                    val beforeUpdateUser = sharedViewModel.getUser()

                                    sharedViewModel.setUser(it.toUserModel())

                                    // Bildirimleri kontrol etme açık ise, bildirimler kontrol edilsin
                                    if(sharedViewModel.getNotification() && beforeUpdateUser != null) checkNotification(beforeUpdateUser)


                                    onSnapAuthUpdate(it, it.toUserModel().userRealName)

                                }
                                false -> {
                                    onSnapAuthUpdate(it, "block")
                                    userError("block") // Kullanıcı sistemden uzaklaştırılıyor..
                                }
                            }

                        }

                    }
                }

            }



        binding.apply {

            // NavigationView (sol & sağ) pencere ayarı ve scrollbar sıfırlama

            drawerMain.apply {

                addDrawerListener(object : DrawerLayout.DrawerListener {

                    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

                        if(slideOffset != 0f) {

                            when(drawerView) {
                                leftNavigation -> if(sharedViewModel.getRightMenu() || rightNavigation.isShown) closeDrawer(leftNavigation)
                                rightNavigation -> if(!sharedViewModel.getRightMenu() || leftNavigation.isShown) {
                                    closeDrawer(rightNavigation)
                                }
                            }

                        }

                    }

                    override fun onDrawerOpened(drawerView: View) {
                        when(drawerView) {
                            leftNavigation -> leftHeaderBinding.txtHeaderName.requestFocus()
                            rightNavigation -> rightHeaderBinding.txtHeaderName.requestFocus()
                        }
                    }

                    override fun onDrawerClosed(drawerView: View) {
                        if(sharedViewModel.getSideMenuAlignment()) {

                            (leftNavigation.getChildAt(0) as RecyclerView).layoutManager!!.scrollToPosition(0)
                            (rightNavigation.getChildAt(0) as RecyclerView).layoutManager!!.scrollToPosition(0)

                            /**
                             * Soldaki & sağdaki navigationview aşağı doğru scroll edildiğinde o konumda kalıyordu
                             * Artık scrollbar pozisyonu resetleniyor ve header görüntüleniyor her seferinde
                             */

                        }
                    }

                    override fun onDrawerStateChanged(newState: Int) {}

                })

            }


            /*
            Left & right navigationda tıklanan item fragmentı açılır
            O an açık olan fragment tekrar açılmaz, profile fragment hariç
            Profile fragment'ta eğer o an incelenen profil bir başka kullanıcının profili ise,
            Kullanıcının kendi id'si argüman olarak verilip profileFragment tekrar açılır.
            */
            val navigationListener = NavigationView.OnNavigationItemSelectedListener {

                binding.drawerMain.closeDrawers()

                if (appActivityViewModel.getArrayNavMenuFragment().contains(it.itemId)) {
                    /**
                     * Feed - Search - Newpost - Notification - Profile - Favorite - Interest
                     * ProfileSettings - AppSettings - ContactUs
                     */
                    if (it.itemId == R.id.profileFragment) {
                        if (!sharedViewModel.getShowMyProfile()){
                            /**
                             *  Profil butonuna basıldı
                             *  Bir başka ekran açık || profil fragment açık && bir başka profil inceleniyor
                             *  Kişi kendi profiline gidiyor
                             */

                            goMyProfile(navController, sharedViewModel.getMyUserId())
                        }
                    }
                    else if (it.itemId != navController.currentDestination?.id)
                    {
                        // Tıklanan item {anasayfa/search/notification} şuan açık değil ise aç
                        changeFragment(navController,null,it.itemId)
                    }
                }
                else {

                    /**
                     * Comment -  Invitation  -  Visit Me - More app - Log Out Items
                     */

                    when(it.itemId) {

                        R.id.navigationComment -> inAppReview(false)

                        R.id.navigationInvitation -> {

                            checkContext {

                                val text = getString(R.string.invitation_message)
                                val link = getString(R.string.playstore_app_link)

                                val shareText = "$text\n$link"

                                WorkUtil.openShareMenu(this, shareText)

                            }

                        }

                        R.id.navigationVisit -> checkContext { visitPlayStoreAppPage(this) }

                        R.id.navigationMoreApp -> {
                            try {
                                startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.playstore_developer_link)))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                            catch (anfe : ActivityNotFoundException) {
                                startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:BaseSoftware"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }

                        R.id.navigationLogOut -> {

                            checkContext {

                                Snackbar
                                    .make(this, binding.root, getString(R.string.exit), Snackbar.LENGTH_LONG)
                                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                    .setBackgroundTint(WorkUtil.snackbarColor(this))
                                    .setTextColor(Color.LTGRAY)
                                    .setGestureInsetBottomIgnored(true)
                                    .questionSnackbar()
                                    .setAction(getString(R.string.yes)) { inAppReview(true) }
                                    .setActionTextColor(Color.GREEN)
                                    .widthSettings()
                                    .show()

                            }

                        }

                    }
                }

                true
            }

            leftNavigation.setupWithNavController(navController)
            leftNavigation.itemIconTintList = null
            leftNavigation.setNavigationItemSelectedListener(navigationListener)

            rightNavigation.setupWithNavController(navController)
            rightNavigation.itemIconTintList = null
            rightNavigation.setNavigationItemSelectedListener(navigationListener)

        }


        /*
        Tıklanan item fragmentı açılır
        O an açık olan fragment tekrar açılmaz, profile fragment hariç
        Profile fragment'ta eğer o an incelenen profil bir başka kullanıcının profili ise,
        Kullanıcının kendi id'si argüman olarak verilip profileFragment tekrar açılır.
        */
        binding.bottomNavigation.setOnItemSelectedListener { item ->

            when(item.itemId) {

                R.id.profileFragment -> {

                    /**
                     *  Profil butonuna basıldı
                     *  Bir başka ekran açıksa veya profil ekranı açık ama başka profil inceleniyor ise kendi profiline git
                     *  Eğer profil ekranı açıksa ve kendi profilini inceliyorsa alt menüyü kapat
                     */
                    navController.currentDestination?.let { destination ->

                        if(destination.id == R.id.profileFragment && sharedViewModel.getShowMyProfile()) bottomUiVisible(false)
                        else goMyProfile(navController, sharedViewModel.getMyUserId())

                    }

                }
                else -> {
                    /**
                     * Tıklanan item {anasayfa/search/notification} şuan açık değil ise aç
                     * Eğer açık ise alt menüyü kapat
                     */
                    navController.currentDestination?.let { destination ->
                        if (item.itemId != destination.id) changeFragment(navController,null,item.itemId)
                        else bottomUiVisible(false)
                    }
                }
            }

            false
        }


        binding.floatingBtnNewPost.setOnClickListener {

            /**
             * Eğer newPostFragment açık değil ise aç
             * Açık ise alt menüyü gizle
             */

            navController.currentDestination?.let {
                when(it.id) {
                    R.id.newPostFragment -> bottomUiVisible(false)
                    else -> changeFragment(navController,null, R.id.newPostFragment)
                }
            }

        }


        navController.addOnDestinationChangedListener { _, destination, _ ->

            appActivityViewModel.setBackgroundMessage(false)

            sharedViewModel.deleteBannerRotateCount()

            checkContext { suggestion(this, sharedViewModel) }

            if(destination.id == R.id.notificationFragment) resetNotificationBadgeCount()

            bottomUiVisible(destination.id == R.id.feedFragment)

            hideKeyboard() // Eğer açık ise klavye kapanıyor

            // Profil fragment harici bir fragment açık ise showmyprofile otomatik false yap bug önlemek için
            if(destination.id != R.id.profileFragment) { sharedViewModel.setShowMyProfile(false) }

            // Feed-Search-Notifitication-Profile harici bir fragment açık ise checked[mavi nokta] işaretini kaldır/ekle
            when(appActivityViewModel.getArrayBottomMenuFragment().contains(destination.id)) {
                true -> {
                    binding.bottomNavigation.menu.findItem(destination.id).apply {
                        isCheckable = true
                        isChecked = true
                    }
                }
                else -> {
                    binding.bottomNavigation.menu.findItem(binding.bottomNavigation.selectedItemId).apply {
                        isChecked = false
                        isCheckable = false
                    }
                }

            }

        }

    }



    @SuppressLint("LogConditional")
    private fun onSnapAuthUpdate(document : DocumentSnapshot, updateType : String) {

        val tagOnSnapAuthUpdate = "Main-OnSnapAuthUpdate"
        try {
            CoroutineScope(Dispatchers.IO).launch {

                checkContext {

                    val update = userProfileChangeRequest {
                        displayName = updateType
                        photoUri = Uri.parse(document.getString("userId")+".webp")
                    }

                    auth.currentUser?.let {
                        it
                            .updateProfile(update)
                            .addOnSuccessListener { Log.i(tagOnSnapAuthUpdate,"Auth {$updateType} için güncelleme başarılı") }
                            .addOnFailureListener { Log.e(tagOnSnapAuthUpdate,"Auth {$updateType} için güncelleme başarısız") }
                    }

                }

            }
        }
        catch (e : Exception) { Log.e(tagOnSnapAuthUpdate,"Auth update error") }
    }

    private fun newNotification(icon : Int, text : Int) {

        checkContext {

            sharedViewModel
                .setNotification(NotificationRecyclerModel(icon, getString(text), Timestamp.now().seconds))
                .also { addNotificationBadgeCount() }

        }

    }

    private fun checkNotification(oldModel: UserModel) {

        sharedViewModel.getUser()?.let { newModel ->

            socialNotification(oldModel, newModel)

            profileNotification(oldModel, newModel)

            criticalNotification(oldModel, newModel)

            blockNotification(oldModel, newModel)

        }

    }

    private fun socialNotification(oldModel: UserModel, newModel : UserModel) {

        CoroutineScope(Dispatchers.Default).launch {

            if(newModel.userFollower > oldModel.userFollower) {

                sharedViewModel.setNotification(
                    NotificationRecyclerModel(
                        R.drawable.notification_ico_new_follower,
                        getString(R.string.notification_new_follower) + newModel.userFollower.toString(),
                        Timestamp.now().seconds
                    )
                ).also { addNotificationBadgeCount() }

            }

            if(!oldModel.userFacebook.equals(newModel.userFacebook, true)) {

                newNotification(
                    if(!newModel.userFacebook.matches(Regex("-"))){
                        R.drawable.notification_ico_facebook_add
                    }
                    else R.drawable.notification_ico_facebook_delete,

                    if(!newModel.userFacebook.matches(Regex("-"))){
                        R.string.notification_facebook_add
                    }
                    else R.string.notification_facebook_delete
                )

            }

            if(!oldModel.userTwitter.equals(newModel.userTwitter, true)) {
                newNotification(
                    if(!newModel.userTwitter.matches(Regex("-"))){
                        R.drawable.notification_ico_twitter_add
                    }
                    else R.drawable.notification_ico_twitter_delete,

                    if(!newModel.userTwitter.matches(Regex("-"))){
                        R.string.notification_twitter_add
                    }
                    else R.string.notification_twitter_delete
                )
            }

            if(!oldModel.userInstagram.equals(newModel.userInstagram, true)) {

                newNotification(
                    if(!newModel.userInstagram.matches(Regex("-"))){
                        R.drawable.notification_ico_instagram_add
                    }
                    else R.drawable.notification_ico_instagram_delete,

                    if(!newModel.userInstagram.matches(Regex("-"))){
                        R.string.notification_instagram_add
                    }
                    else R.string.notification_instagram_delete
                )

            }

        }

    }

    private fun profileNotification(oldModel: UserModel, newModel : UserModel) {

        CoroutineScope(Dispatchers.Default).launch {

            val icon = R.drawable.notification_ico_profile_edit

            if(!oldModel.userRealName.equals(newModel.userRealName, true)) {

                newNotification(icon, R.string.notification_edit_realname)

            }

            if(!oldModel.userName.equals(newModel.userName, true)) {

                newNotification(icon, R.string.notification_edit_username)

            }

            if(!oldModel.userBiography.equals(newModel.userBiography, true)) {

                newNotification(icon, R.string.notification_edit_biography)

            }

            if(!oldModel.userMail.equals(newModel.userMail, true)) {

                newNotification(icon, R.string.notification_edit_mail)

            }

            if(!oldModel.userProfilePicture.equals(newModel.userProfilePicture, true)) {

                newNotification(
                    R.drawable.notification_ico_picture_change,
                    R.string.notification_picture_change
                )

            }

            if(oldModel.userAdminMessageDate!!.seconds != newModel.userAdminMessageDate!!.seconds) {

                newNotification(
                    R.drawable.notification_ico_new_admin_message,
                    R.string.notification_new_admin_message
                )

            }

        }

    }

    private fun criticalNotification(oldModel: UserModel, newModel : UserModel) {

        CoroutineScope(Dispatchers.Default).launch {

            if(newModel.userReport > oldModel.userReport) {

                newNotification(
                    R.drawable.notification_ico_new_report,
                    R.string.notification_new_report
                )

            }

            if(oldModel.userEmailConfirm != newModel.userEmailConfirm) {

                newNotification(
                    if(newModel.userEmailConfirm){
                        R.drawable.notification_ico_verify_add
                    }
                    else R.drawable.notification_ico_verify_delete,

                    if(newModel.userEmailConfirm){
                        R.string.notification_verify_add
                    }
                    else R.string.notification_verify_delete
                )

            }

        }

    }

    private fun blockNotification(oldModel: UserModel, newModel : UserModel) {

        CoroutineScope(Dispatchers.Default).launch {

            if(oldModel.userAddPost != newModel.userAddPost && !newModel.userAddPost) {

                newNotification(
                    R.drawable.notification_ico_new_post_block,
                    R.string.notification_new_post_block
                )

            }

            if(oldModel.userAddComment != newModel.userAddComment && !newModel.userAddComment) {

                newNotification(
                    R.drawable.notification_ico_new_comment_block,
                    R.string.notification_new_comment_block
                )

            }

            if(oldModel.userAddReport != newModel.userAddReport && !newModel.userAddReport) {

                newNotification(
                    R.drawable.notification_ico_new_report_block,
                    R.string.notification_new_report_block
                )

            }

            if(oldModel.userAddContact != newModel.userAddContact && !newModel.userAddContact) {

                newNotification(
                    R.drawable.notification_ico_new_contact_block,
                    R.string.notification_new_contact_block
                )

            }

            if(oldModel.userEditProfile != newModel.userEditProfile && !newModel.userEditProfile) {

                newNotification(
                    R.drawable.notification_ico_profile_edit_block,
                    R.string.notification_edit_block
                )

            }

        }

    }



    private fun userError(errorType : String) {


        appActivityViewModel.setUserError(true) // Screen touch kilitleniyor


        //---------- Ayarlar sıfırlanıyor ----------
        sharedViewModel.setShowMyProfile(false)
        sharedViewModel.setMyUserId("null")
        getSharedPreferences(packageName.toString(), Context.MODE_PRIVATE).edit {
            putBoolean("rememberMe",false)
            putString("userId","null")
            commit()
        }
        //---------- Ayarlar sıfırlanıyor ----------


        /**
         * Hata tipine göre snackbar mesajı ayarlanıyor
         * Bağlantı hatası ise snackbar gösterilip login gidecek
         * Kullanıcı sunucudan uzaklaştırıldı ise firebase işlemleri sonrası login gidecek
         */

        // Mesaj ayarlanıyor

        checkContext {

            val errorText = when(errorType) {
                "block" -> getString(R.string.banned)
                else -> getString(R.string.check_internet_connection)
            }


            Snackbar
                .make(this, binding.root, errorText, Snackbar.LENGTH_LONG)
                .settings()
                .widthSettings()
                .setAnchorView(binding.bottomAppBar)
                .addCallback(object : Snackbar.Callback(){
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)

                        when(errorType) {
                            "block" -> userBanned() // Kullanıcı sistemden uzaklaştırıldı
                            else -> goLogin() // Bağlantı hatası mevcut
                        }

                    }
                })
                .show()

        }

    }

    private fun userBanned() {
        CoroutineScope(Dispatchers.IO).launch {

            userSnapshot.remove()

            storage
                .reference
                .child(sharedViewModel.getMyUserId()+".webp")
                .delete()
                .addOnCompleteListener {
                    when(it.isSuccessful) {
                        true -> Log.i("AppActivity-userBanned", "Kullanıcı resmi silindi")
                        else -> {
                            Log.e("AppActivity-userBanned", "Kullanıcı resmi silinemedi")
                            it.exception?.msg()?.let { msg -> Log.e("AppActivity-userBanned", msg) }
                        }
                    }
                    goLogin()
                }

        }
    }



    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {

        appActivityViewModel.apply {

            if (event != null && !getUserError()) {
                when(event.action) {

                    MotionEvent.ACTION_DOWN -> setTouchStartY(event.y) // İlk dokunma anı

                    MotionEvent.ACTION_UP -> {

                        /**
                         * Dokunmayı bıraktı, yukarı veya aşağı yöndeki hareketi kontrol edilecek
                         * Son dokunma anındaki y eksen değeri başlangıçta y eksen değeri ile kontrol ediliyor
                         * Duruma göre yukarı veya aşağı kaydırma tespit ediliyor veya bottom ui şekilleniyor
                         */

                        setTouchEndY(event.y)

                        if(getTouchStartY() != getTouchEndY()) {

                            when(getTouchStartY() > getTouchEndY()) {

                                true -> sharedViewModel.setScrollDown(true)

                                false -> if(getTouchEndY() - getTouchStartY() >= 10) { sharedViewModel.setScrollDown(false) }

                            }
                        }

                    }

                }
            }

        }

        return when(appActivityViewModel.getUserError()) {
            true -> true
            false -> super.dispatchTouchEvent(event)
        }
    }


}
