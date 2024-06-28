package com.basesoftware.fikirzadem.presentation.ui.admin

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.model.BannedBlockModel
import com.basesoftware.fikirzadem.presentation.adapter.ContactUsRecyclerAdapter
import com.basesoftware.fikirzadem.presentation.adapter.ReportRecyclerAdapter
import com.basesoftware.fikirzadem.databinding.BottomDialogPostMoreBinding
import com.basesoftware.fikirzadem.databinding.FragmentAdminBinding
import com.basesoftware.fikirzadem.model.LikeDislikeModel
import com.basesoftware.fikirzadem.model.PostModel
import com.basesoftware.fikirzadem.presentation.viewmodel.AdminViewModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.dataAvailable
import com.basesoftware.fikirzadem.util.ExtensionUtil.gone
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.questionSnackbar
import com.basesoftware.fikirzadem.util.ExtensionUtil.show
import com.basesoftware.fikirzadem.util.ExtensionUtil.toContactModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.toLikeDislikeModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.toPostModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.toReportModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.toSocialModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.toUserModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates
import kotlin.random.Random


class AdminFragment : Fragment() {

    private var _binding : FragmentAdminBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore : FirebaseFirestore

    private lateinit var storage : FirebaseStorage

    private lateinit var clipboardManager : ClipboardManager

    private lateinit var popupCategory : PopupMenu

    private lateinit var popupLike : PopupMenu

    private var greenColor by Delegates.notNull<Int>()

    private var redColor by Delegates.notNull<Int>()

    private lateinit var arrayLayouts : ArrayList<LinearLayout>

    private lateinit var arrayActionUsersLayout : ArrayList<LinearLayout> // Silinen içeriklerin kontrol edildiği layout
    private lateinit var arrayActionUsers : ArrayList<TextView> // Silinen içeriklere beğeni atanların id'lerinin yazıldı textview

    private lateinit var arraySocialUsersLayout : ArrayList<LinearLayout> // Silinen hesapların kontrol edildiği layout
    private lateinit var arraySocialUsers : ArrayList<TextView> // Silinen hesapların social listesinde olanların id'lerinin yazıldığı textview

    private val adminViewModel : AdminViewModel by viewModels()


    override fun onCreateView(inf: LayoutInflater, cont: ViewGroup?, instance: Bundle?): View {

        _binding = FragmentAdminBinding.inflate(inf, cont, false)

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()

        popup()

        listener()

    }


    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


    private fun checkContext(context: Context.() -> Unit) { if (isAdded) { context(requireContext()) } }



    @SuppressLint("DiscouragedPrivateApi")
    private fun initialize() {

        firestore = WorkUtil.firestore()

        storage = FirebaseStorage.getInstance()

        checkContext {

            clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            greenColor = ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)

            redColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)

            binding.recyclerAdminContact.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false)
                adapter = ContactUsRecyclerAdapter(adminViewModel, binding.root)
            }

            binding.recyclerAdminReport.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false)
                adapter = ReportRecyclerAdapter(adminViewModel, binding.root)
            }

            popupLike = PopupMenu(requireContext(),binding.txtAdminPostMode)
            popupLike.inflate(R.menu.newpost_likemode_menu)

            popupCategory = PopupMenu(requireContext(),binding.txtAdminPostCategory, Gravity.END)
            popupCategory.inflate(R.menu.newpost_category_menu_tr)

        }

        try {
            val declared = PopupMenu::class.java.getDeclaredField("mPopup")
            declared.isAccessible = true

            // Like mode popup ayarı
            var mpopup = declared.get(popupLike)
            mpopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(mpopup,true)

            // Category popup ayarı
            mpopup = declared.get(popupCategory)
            mpopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(mpopup,true)

        }
        catch (e : Exception){ Log.e("Admin-Popup","Admin likemode ikon gösterim hatası") }

        binding.apply {

            arrayLayouts = arrayListOf()
            arrayLayouts.add(linearAdminEditProfile)
            arrayLayouts.add(linearAdminUserSearch)
            arrayLayouts.add(linearAdminEditPost)
            arrayLayouts.add(linearAdminEditPostComment)
            arrayLayouts.add(linearAdminSystemMessage)
            arrayLayouts.add(linearDeleteBlockMail)
            arrayLayouts.add(linearDeleteBlock)
            arrayLayouts.add(linearDeleteProfile)

            arraySocialUsersLayout = arrayListOf()
            arraySocialUsersLayout.add(linearSocialAction1)
            arraySocialUsersLayout.add(linearSocialAction2)
            arraySocialUsersLayout.add(linearSocialAction3)
            arraySocialUsersLayout.add(linearSocialAction4)
            arraySocialUsersLayout.add(linearSocialAction5)

            arraySocialUsers = arrayListOf()
            arraySocialUsers.add(txtSocialAction1)
            arraySocialUsers.add(txtSocialAction2)
            arraySocialUsers.add(txtSocialAction3)
            arraySocialUsers.add(txtSocialAction4)
            arraySocialUsers.add(txtSocialAction5)

        }

    }

    private fun popup() {

        try {

            popupLike.menu.findItem(R.id.postPublic).let { item ->
                item.isChecked = true

                binding.txtAdminPostMode.apply {
                    this.text = item.title.toString()
                    this.setCompoundDrawablesWithIntrinsicBounds(item.icon,null,null,null)
                }

            }

            popupCategory.menu.findItem(R.id.category_shop).let { item ->
                item.isChecked = true

                binding.txtAdminPostCategory.apply {
                    this.text = item.title.toString()
                    this.setCompoundDrawablesWithIntrinsicBounds(item.icon,null,null,null)
                }

            }

            Log.i("popup", "AdminFrag - PopupSettings başarılı")
        } catch (e: Exception) {
            Log.e("popup", "AdminFrag - PopupSettings başarısız")
            Log.e("popup", e.msg())
        }

    }


    @SuppressLint("SetTextI18n")
    private fun listener() {


        // Login ve layout clickler

        binding.apply {

            btnAdminLogin.setOnClickListener {
                controlPassword()
                try {
                    checkContext {
                        (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken,0)
                    }
                }
                catch (e : Exception) {
                    Log.w("AdminLogin-hideKeyboard", e.msg())
                }
            }

            txtAdminLogin.setOnEditorActionListener { _, action, _ ->

                if(action == EditorInfo.IME_ACTION_DONE){

                    try {
                        controlPassword()
                        checkContext {
                            (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                                .hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken,0)
                        }

                    }
                    catch (e : Exception) {
                        Log.w("AdminLogin-hideKeyboard", e.msg())
                    }

                }

                return@setOnEditorActionListener true
            }

            val layoutHeightListener = View.OnClickListener {

                val params = when(it.layoutParams.height == WindowManager.LayoutParams.WRAP_CONTENT) {
                    true -> LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
                    else -> LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                params.setMargins(10,25,10,0)
                it.layoutParams = params
            }

            linearAdminEditProfile.setOnClickListener(layoutHeightListener)

            linearAdminUserSearch.setOnClickListener(layoutHeightListener)

            linearAdminEditPost.setOnClickListener(layoutHeightListener)

            linearAdminEditPostComment.setOnClickListener(layoutHeightListener)

            linearAdminSystemMessage.setOnClickListener(layoutHeightListener)

            linearDeleteBlockMail.setOnClickListener(layoutHeightListener)

            linearDeleteBlock.setOnClickListener(layoutHeightListener)

            linearDeleteProfile.setOnClickListener(layoutHeightListener)

        }


        // Profil düzenleme layout

        binding.apply {

            txtAdminEditPp.openBottomSheet()
            txtAdminEditRealName.openBottomSheet()
            txtAdminEditUserName.openBottomSheet()
            txtAdminEditBiography.openBottomSheet()
            txtAdminEditMail.openBottomSheet()
            txtAdminEditInstagram.openBottomSheet()
            txtAdminEditTwitter.openBottomSheet()
            txtAdminEditFacebook.openBottomSheet()
            txtAdminEditFollowing.openBottomSheet()
            txtAdminEditFollower.openBottomSheet()
            txtAdminEditReport.openBottomSheet()


            imgAdminUserIdCopy.setOnClickListener { copyClipBoard(txtAdminUserId.text.toString()) }

            imgAdminEditPpOriginal.setOnClickListener {

                try {
                    if(!user!!.userProfilePicture.matches(Regex("default"))){

                        checkContext {

                            val dialog = Dialog(requireContext())

                            dialog.setContentView(R.layout.profile_picture_dialog)

                            Glide
                                .with(requireContext())
                                .setDefaultRequestOptions(WorkUtil.glideDefault(requireContext()))
                                .load(user!!.userProfilePicture)
                                .into(dialog.findViewById(R.id.imgProfilePictureOrg))

                            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


                            dialog.findViewById<ConstraintLayout>(R.id.constraintPpDialog).isSoundEffectsEnabled = false

                            dialog.findViewById<ConstraintLayout>(R.id.constraintPpDialog)
                                .setOnClickListener { dialog.dismiss() }

                            dialog.show()

                        }

                    }
                }
                catch (e : Exception) { Log.e("AdminFrag-OpenPp","Profil resmi dialog kutusu açılamadı") }
            }

            imgAdminDeletePp.setOnClickListener { txtAdminEditPp.setText("default") }

            imgAdminDeleteRealName.setOnClickListener { txtAdminEditRealName.setText(txtAdminEditUserName.text.toString()) }

            imgAdminDeleteUserName.setOnClickListener {
                val userName = "user" + Random.nextInt(10000,99999).toString()
                txtAdminEditUserName.setText(userName)
                txtAdminEditRealName.setText(userName)
            }

            imgAdminDeleteBiography.setOnClickListener { txtAdminEditBiography.setText("-") }

            imgAdminDeleteInstagram.setOnClickListener { txtAdminEditInstagram.setText("-") }

            imgAdminDeleteTwitter.setOnClickListener { txtAdminEditTwitter.setText("-") }

            imgAdminDeleteFacebook.setOnClickListener { txtAdminEditFacebook.setText("-") }

            imgAdminAddReport.setOnClickListener {

                txtAdminEditReport.setText(
                    if(txtAdminEditReport.length() == 0) "0"
                    else (txtAdminEditReport.text.toString().toInt() + 1).toString()
                )

            }

            imgAdminAddSpamPost.setOnClickListener {

                txtAdminEditSpamPost.setText(
                    if(txtAdminEditSpamPost.length() == 0) "0"
                    else (txtAdminEditSpamPost.text.toString().toInt() + 1).toString()
                )

            }

            imgAdminAddSpamComment.setOnClickListener {

                txtAdminEditSpamComment.setText(
                    if(txtAdminEditSpamComment.length() == 0) "0"
                    else (txtAdminEditSpamComment.text.toString().toInt() + 1).toString()
                )

            }

            imgAdminAddSpamReport.setOnClickListener {

                txtAdminEditSpamReport.setText(
                    if(txtAdminEditSpamReport.length() == 0) "0"
                    else (txtAdminEditSpamReport.text.toString().toInt() + 1).toString()
                )

            }

            imgAdminAddSpamContact.setOnClickListener {

                txtAdminEditSpamContact.setText(
                    if(txtAdminEditSpamContact.length() == 0) "0"
                    else (txtAdminEditSpamContact.text.toString().toInt() + 1).toString()
                )

            }



            val extraOnClick =  View.OnLongClickListener {

                checkContext {

                    (it as TextView).setTextColor( if(it.currentTextColor == greenColor) redColor else greenColor)

                }

                return@OnLongClickListener true
            }

            txtAdminEditAddPost.setOnLongClickListener(extraOnClick)

            txtAdminEditAddComment.setOnLongClickListener(extraOnClick)

            txtAdminEditAddReport.setOnLongClickListener(extraOnClick)

            txtAdminEditAddContact.setOnLongClickListener(extraOnClick)

            txtAdminEditProfile.setOnLongClickListener(extraOnClick)

            txtAdminEditActive.setOnLongClickListener(extraOnClick)

            txtAdminEditConfirm.setOnLongClickListener(extraOnClick)


            btnAdminBlockDeleteUser.setOnLongClickListener {

                txtAdminEditMail.text?.let { mail ->

                    checkContext {

                        Snackbar
                            .make(requireContext(), root, "Kullanıcı silinecek ve blokla ?", Snackbar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                            .setTextColor(Color.LTGRAY)
                            .setGestureInsetBottomIgnored(true)
                            .questionSnackbar()
                            .setAction(getString(R.string.yes)) {

                                CoroutineScope(Dispatchers.IO).launch {

                                    val blockId = UUID.randomUUID().toString()
                                    val blockModel = BannedBlockModel(blockId, mail.toString(), null)
                                    val tag = "adminFrag-AddBlock"

                                    firestore
                                        .collection("Block")
                                        .document(blockId)
                                        .set(blockModel)
                                        .addOnCompleteListener { addBlockTask ->
                                            when(addBlockTask.isSuccessful){
                                                true -> {

                                                    logsnackbar("Kullanıcı engellenenler tablosuna eklendi", "info")

                                                    firestore
                                                        .collection("Users")
                                                        .document(txtAdminUserId.text.toString())
                                                        .update("userIsActive", false)
                                                        .addOnCompleteListener {

                                                            when(it.isSuccessful) {

                                                                true -> {
                                                                    CoroutineScope(Dispatchers.Main).launch {
                                                                        logsnackbar("Hesap silindi", "info")
                                                                        clearProfileUi()
                                                                    }
                                                                }

                                                                else -> {
                                                                    CoroutineScope(Dispatchers.Main).launch {
                                                                        logsnackbar("Hesap silinemedi", "error")
                                                                        it.exception?.msg()?.let { msg ->  logsnackbar(msg, "error") }
                                                                    }
                                                                }

                                                            }

                                                        }

                                                }
                                                else -> {
                                                    logsnackbar("Kullanıcı engellenenler tablosuna eklenemedi", "error")
                                                    addBlockTask.exception?.msg()?.let { Log.e(tag, it) }
                                                }
                                            }
                                        }

                                }

                            }
                            .setActionTextColor(Color.RED)
                            .widthSettings()
                            .show()

                    }

                }

                return@setOnLongClickListener true
            }

            btnAdminDeleteUser.setOnLongClickListener {

                if(txtAdminUserId.text.toString().isNotEmpty()) {

                    checkContext {

                        Snackbar
                            .make(requireContext(), root, "Kullanıcı silinecek ?", Snackbar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                            .setTextColor(Color.LTGRAY)
                            .setGestureInsetBottomIgnored(true)
                            .questionSnackbar()
                            .setAction(getString(R.string.yes)) {

                                // Eğer hesap silerken kullanıcının rapor sayısı 5 ten fazla ise block listesine ekle
                                txtAdminEditReport.text?.let {
                                    if(it.toString().isNotEmpty()) {
                                        if(it.toString().toInt() > 5) {

                                            val blockId = UUID.randomUUID().toString()
                                            val blockModel = BannedBlockModel(blockId, txtAdminEditMail.text.toString(), null)

                                            firestore.collection("Block").document(blockId).set(blockModel)
                                        }
                                    }
                                }

                                CoroutineScope(Dispatchers.IO).launch {

                                    firestore
                                        .collection("Users")
                                        .document(txtAdminUserId.text.toString())
                                        .update("userIsActive", false)
                                        .addOnCompleteListener {

                                            CoroutineScope(Dispatchers.Main).launch {

                                                when(it.isSuccessful) {

                                                    true -> {
                                                        logsnackbar("Hesap silindi", "info")
                                                        clearProfileUi()
                                                    }

                                                    else -> {
                                                        logsnackbar("Hesap silinemedi", "error")
                                                        it.exception?.msg()?.let { msg ->  logsnackbar(msg, "error") }
                                                    }

                                                }

                                            }

                                        }

                                }

                            }
                            .setActionTextColor(Color.RED)
                            .widthSettings()
                            .show()

                    }

                }

                return@setOnLongClickListener true

            }

            btnAdminUpdateSend.setOnLongClickListener {

                if(txtAdminUserId.text.toString().isNotEmpty()) {

                    checkContext {

                        Snackbar
                            .make(requireContext(), root, "Kullanıcı güncellenecek ?", Snackbar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                            .setTextColor(Color.LTGRAY)
                            .setGestureInsetBottomIgnored(true)
                            .questionSnackbar()
                            .setAction(getString(R.string.yes)) {

                                checkContext {

                                    val data = mutableMapOf <String, Any> ()

                                    /**
                                     * Eğer rapor sayısı 5'ten fazla ise
                                     * Block oluştur isActive false yap hesap silinsin
                                     */
                                    txtAdminEditReport.text?.let {
                                        if(it.toString().isNotEmpty()) {
                                            if(it.toString().toInt() > 5) {

                                                val blockId = UUID.randomUUID().toString()
                                                val blockModel = BannedBlockModel(blockId, txtAdminEditMail.text.toString(), null)

                                                firestore.collection("Block").document(blockId).set(blockModel)

                                                chkAdminEditActive.isChecked = true
                                                txtAdminEditActive.setTextColor(redColor)
                                            }
                                        }
                                    }

                                    // Eğer spam içerik sayısı 5'ten fazla ise içerik ekleme false yap
                                    txtAdminEditSpamPost.text?.let {
                                        if(it.toString().isNotEmpty()) {
                                            if(it.toString().toInt() > 5) {
                                                chkAdminEditAddPost.isChecked = true
                                                txtAdminEditAddPost.setTextColor(redColor)
                                            }
                                        }
                                    }

                                    // Eğer spam yorum sayısı 5'ten fazla ise yorum ekleme false yap
                                    txtAdminEditSpamComment.text?.let {
                                        if(it.toString().isNotEmpty()) {
                                            if(it.toString().toInt() > 5) {
                                                chkAdminEditAddComment.isChecked = true
                                                txtAdminEditAddComment.setTextColor(redColor)
                                            }
                                        }
                                    }

                                    // Eğer spam report sayısı 15'ten fazla ise rapor göndermeyi false yap
                                    txtAdminEditSpamReport.text?.let {
                                        if(it.toString().isNotEmpty()) {
                                            if(it.toString().toInt() > 15) {
                                                chkAdminEditAddReport.isChecked = true
                                                txtAdminEditAddReport.setTextColor(redColor)
                                            }
                                        }
                                    }

                                    // Eğer spam admin contact sayısı 15'ten fazla ise admin iletişimi false yap
                                    txtAdminEditSpamContact.text?.let {
                                        if(it.toString().isNotEmpty()) {
                                            if(it.toString().toInt() > 15) {
                                                chkAdminEditAddContact.isChecked = true
                                                txtAdminEditAddContact.setTextColor(redColor)
                                            }
                                        }
                                    }


                                    if(chkAdminEditMessageTr.isChecked || chkAdminEditMessageEn.isChecked) {

                                        data["userAdminMessageDate"] = FieldValue.serverTimestamp()

                                    }


                                    // Kaydetme yapılacak..

                                    if(chkAdminEditPp.isChecked) {

                                        data["userProfilePicture"] = txtAdminEditPp.text.toString()

                                        if(!binding.user!!.userProfilePicture.matches(Regex("default"))){

                                            CoroutineScope(Dispatchers.IO).launch {

                                                storage.reference.child(txtAdminUserId.text.toString() + ".webp")
                                                    .delete()
                                                    .addOnCompleteListener {
                                                        CoroutineScope(Dispatchers.Main).launch {
                                                            when(it.isSuccessful) {
                                                                true -> logsnackbar("Resim silindi", "info")
                                                                else -> {
                                                                    logsnackbar("Resim silinemedi", "error")
                                                                    it.exception?.msg()?.let { logsnackbar(it, "error") }
                                                                }
                                                            }
                                                        }
                                                    }

                                            }

                                        }

                                    }


                                    if(chkAdminEditRealName.isChecked) data["userRealName"] = txtAdminEditRealName.text.safeRealName()

                                    if(chkAdminEditUserName.isChecked) data["userName"] = txtAdminEditUserName.text.safeUserName()

                                    if(chkAdminEditBiography.isChecked) data["userBiography"] = txtAdminEditBiography.text.safeText()

                                    txtAdminEditMail.text?.let {
                                        if(chkAdminEditMail.isChecked && it.toString().isNotEmpty()) data["userMail"] = it.toString()
                                    }


                                    if(chkAdminEditInstagram.isChecked) data["userInstagram"] = txtAdminEditInstagram.text.safeText()

                                    if(chkAdminEditTwitter.isChecked) data["userTwitter"] = txtAdminEditTwitter.text.safeText()

                                    if(chkAdminEditFacebook.isChecked) data["userFacebook"] = txtAdminEditFacebook.text.safeText()


                                    if(chkAdminEditMessageTr.isChecked) data["userAdminMessageTr"] = txtAdminEditMessageTr.text.safeText()

                                    if(chkAdminEditMessageEn.isChecked) data["userAdminMessageEn"] = txtAdminEditMessageEn.text.safeText()


                                    if(chkAdminEditFollowing.isChecked) data["userFollowing"] = txtAdminEditFollowing.text.safeCount()

                                    if(chkAdminEditFollower.isChecked) data["userFollower"] = txtAdminEditFollower.text.safeCount()


                                    if(chkAdminEditReport.isChecked) data["userReport"] = txtAdminEditReport.text.safeCount()


                                    if(chkAdminEditSpamPost.isChecked) data["userSpamPost"] = txtAdminEditSpamPost.text.safeCount()

                                    if(chkAdminEditSpamComment.isChecked) data["userSpamComment"] = txtAdminEditSpamComment.text.safeCount()

                                    if(chkAdminEditSpamReport.isChecked) data["userSpamReport"] = txtAdminEditSpamReport.text.safeCount()

                                    if(chkAdminEditSpamContact.isChecked) data["userSpamContact"] = txtAdminEditSpamContact.text.safeCount()



                                    if(chkAdminEditAddPost.isChecked) data["userAddPost"] = txtAdminEditAddPost.currentTextColor == greenColor

                                    if(chkAdminEditAddComment.isChecked) data["userAddComment"] = txtAdminEditAddComment.currentTextColor == greenColor

                                    if(chkAdminEditAddReport.isChecked) data["userAddReport"] = txtAdminEditAddReport.currentTextColor == greenColor

                                    if(chkAdminEditAddContact.isChecked) data["userAddContact"] = txtAdminEditAddContact.currentTextColor == greenColor


                                    if(chkAdminEditProfile.isChecked) data["userEditProfile"] = txtAdminEditProfile.currentTextColor == greenColor

                                    if(chkAdminEditConfirm.isChecked) data["userEmailConfirm"] = txtAdminEditConfirm.currentTextColor == greenColor

                                    if(chkAdminEditActive.isChecked) data["userIsActive"] = txtAdminEditActive.currentTextColor == greenColor



                                    firestore
                                        .collection("Users")
                                        .document(txtAdminUserId.text.toString())
                                        .update(data)
                                        .addOnCompleteListener {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                when(it.isSuccessful) {
                                                    true -> {
                                                        logsnackbar("Profil güncellendi", "info")
                                                        clearProfileUi()
                                                    }
                                                    else -> {
                                                        logsnackbar("Profil güncellenemedi", "error")
                                                        it.exception?.msg()?.let { logsnackbar(it, "error") }
                                                    }
                                                }
                                            }
                                        }

                                }

                            }
                            .setActionTextColor(Color.GREEN)
                            .widthSettings()
                            .show()

                    }

                }
                return@setOnLongClickListener true
            }

        }


        // Kullanıcı arama layout

        binding.apply {

            val searchArrayUi : ArrayList<TextInputEditText> = arrayListOf()
            searchArrayUi.add(txtAdminSearchId)
            searchArrayUi.add(txtAdminSearchRealName)
            searchArrayUi.add(txtAdminSearchUserName)
            searchArrayUi.add(txtAdminSearchMail)

            for (ui : TextInputEditText in searchArrayUi) {
                ui.setOnEditorActionListener { _, action, _ ->
                    if(action == EditorInfo.IME_ACTION_DONE){

                        try {

                            checkContext {

                                btnAdminSearchUser.performClick()
                                (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                                    .hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken,0)

                            }

                        }
                        catch (e : Exception) { Log.w("AdminLogin-hideKeyboard", e.msg()) }

                    }
                    return@setOnEditorActionListener true
                }
            }

            btnAdminSearchUser.setOnClickListener {

                for (ui : TextInputEditText in searchArrayUi) {

                    if (ui.length() > 0) {

                        val dataTitle = when(ui) {
                            txtAdminSearchId -> "userId"
                            txtAdminSearchRealName -> "userRealName"
                            txtAdminSearchUserName -> "userName"
                            else -> "userMail"
                        }

                        CoroutineScope(Dispatchers.IO).launch {

                            firestore
                                .collection("Users")
                                .whereEqualTo(dataTitle, ui.text.toString())
                                .limit(1)
                                .get(Source.SERVER)
                                .addOnCompleteListener {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        when(it.isSuccessful && it.result.documents.isNotEmpty()) {
                                            true -> {
                                                binding.apply {
                                                    user = it.result.documents[0].toUserModel()
                                                    hideLayoutsAndOpen(linearAdminEditProfile)
                                                }
                                                logsnackbar("Kullanıcı getirildi", "info")
                                            }
                                            else -> {
                                                logsnackbar("Kullanıcı getirilemedi", "error")
                                                it.exception?.msg()?.let { msg -> logsnackbar(msg, "error") }
                                            }
                                        }
                                    }
                                }

                        }

                        break
                    }

                }

            }

        }


        // İçerik düzenleme layout

        binding.apply {

            txtAdminEditPostContent.openBottomSheet()

            txtAdminPostId.setOnClickListener { copyClipBoard(txtAdminPostId.text.toString()) }

            imgAdminPostIdCopy.setOnClickListener { copyClipBoard(txtAdminPostId.text.toString()) }

            txtAdminPostUserId.setOnClickListener { copyClipBoard(txtAdminPostUserId.text.toString()) }

            imgAdminPostUserIdCopy.setOnClickListener { adminViewModel.setReviewUser(txtAdminPostUserId.text.toString()) }

            txtAdminPostCategory.setOnClickListener { popupCategory.show() }

            txtAdminPostMode.setOnClickListener { popupLike.show() }

            btnAdminDeletePost.setOnLongClickListener {

                checkContext {

                    Snackbar
                        .make(requireContext(), root, "İçerik silinecek ?", Snackbar.LENGTH_LONG)
                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                        .setTextColor(Color.LTGRAY)
                        .setGestureInsetBottomIgnored(true)
                        .questionSnackbar()
                        .setAction(getString(R.string.yes)) {

                            CoroutineScope(Dispatchers.IO).launch {
                                firestore
                                    .collection("Posts")
                                    .document(txtAdminPostId.text.toString())
                                    .delete()
                                    .addOnCompleteListener {
                                        CoroutineScope(Dispatchers.Main).launch {

                                            when(it.isSuccessful) {
                                                true -> {
                                                    logsnackbar("İçerik silindi", "info")
                                                    clearPostUi()
                                                }
                                                else -> {
                                                    logsnackbar("İçerik silinemedi", "error")
                                                    logsnackbar(it.exception?.msg()!!, "error")
                                                }
                                            }

                                        }
                                    }
                            }

                        }
                        .setActionTextColor(Color.RED)
                        .widthSettings()
                        .show()

                }

                return@setOnLongClickListener true
            }

            btnAdminUpdatePost.setOnLongClickListener {

                if(txtAdminPostId.text.toString().isNotEmpty()) {

                    checkContext {

                        Snackbar
                            .make(requireContext(), root, "İçerik güncellenecek ?", Snackbar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                            .setTextColor(Color.LTGRAY)
                            .setGestureInsetBottomIgnored(true)
                            .questionSnackbar()
                            .setAction(getString(R.string.yes)) {

                                checkContext {

                                    val updateData = mutableMapOf <String, Any> ()

                                    if(chkAdminEditCategory.isChecked) {
                                        updateData["postCategoryId"] = resources
                                            .getStringArray(R.array.arrayCategoryText)
                                            .indexOf(binding.txtAdminPostCategory.text.toString())
                                    }

                                    if(chkAdminEditPostMode.isChecked) {
                                        updateData["postLikePublic"] = txtAdminPostMode
                                            .text
                                            .toString()
                                            .matches(Regex(getString(R.string.likemode_on)))
                                    }

                                    if(chkAdminEditPostContent.isChecked) updateData["postContent"] = txtAdminEditPostContent.text.safeText()


                                    firestore
                                        .collection("Posts")
                                        .document(txtAdminPostId.text.toString())
                                        .update(updateData)
                                        .addOnCompleteListener {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                when(it.isSuccessful) {
                                                    true -> {
                                                        logsnackbar("İçerik güncellendi", "info")
                                                        clearPostUi()
                                                    }
                                                    else -> {
                                                        logsnackbar("İçerik güncellenemedi", "error")
                                                        logsnackbar(it.exception?.msg()!!, "error")
                                                    }
                                                }
                                            }
                                        }

                                }

                            }
                            .setActionTextColor(Color.GREEN)
                            .widthSettings()
                            .show()

                    }

                }

                return@setOnLongClickListener true
            }

        }


        // Yorum düzenleme layout

        binding.apply {

            txtAdminPostCommentId.setOnClickListener { copyClipBoard(txtAdminPostCommentId.text.toString()) }

            imgAdminPostCommentIdCopy.setOnClickListener { adminViewModel.setReviewPost(txtAdminPostCommentId.text.toString()) }

            txtAdminPostCommentUserId.setOnClickListener { copyClipBoard(txtAdminPostCommentUserId.text.toString()) }

            imgAdminPostCommentUserIdCopy.setOnClickListener { adminViewModel.setReviewUser(txtAdminPostCommentUserId.text.toString()) }

            btnAdminDeletePostComment.setOnLongClickListener {

                checkContext {

                    Snackbar
                        .make(requireContext(), root, "Yorum silinecek ?", Snackbar.LENGTH_LONG)
                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                        .setTextColor(Color.LTGRAY)
                        .setGestureInsetBottomIgnored(true)
                        .questionSnackbar()
                        .setAction(getString(R.string.yes)) {

                            CoroutineScope(Dispatchers.IO).launch {

                                val postRef = firestore.collection("Posts").document(txtAdminPostCommentId.text.toString())

                                val actionRef = postRef.collection("PostAction").document(txtAdminPostCommentUserId.text.toString())

                                actionRef
                                    .update(mutableMapOf<String, Any?>("postCommentExist" to false))
                                    .addOnCompleteListener { updateTask ->
                                        if(updateTask.isSuccessful) logsnackbar("Yorum silindi", "info").also { clearCommentUi() }
                                        else {

                                            actionRef
                                                .get(Source.SERVER)
                                                .addOnCompleteListener { actionTask ->

                                                    when(actionTask.isSuccessful) {

                                                        true -> {

                                                            if(actionTask.result.exists()) logsnackbar("Yorum silinemedi", "error")
                                                            else {
                                                                postRef
                                                                    .get(Source.SERVER)
                                                                    .addOnSuccessListener { postTask ->
                                                                        if(postTask.exists()) logsnackbar("Kullanıcı silinmiş", "error")
                                                                        else logsnackbar("İçerik silinmiş", "error")
                                                                    }
                                                                    .addOnFailureListener { logsnackbar("Yorum silinemedi", "error") }
                                                            }

                                                        }

                                                        else -> logsnackbar("Yorum silinemedi", "error")

                                                    }

                                                }

                                        }

                                    }

                            }

                        }
                        .setActionTextColor(Color.RED)
                        .widthSettings()
                        .show()

                }

                return@setOnLongClickListener true
            }

            btnAdminDeletePostLike.setOnLongClickListener {

                checkContext {

                    Snackbar
                        .make(requireContext(), root, "Beğeni silinecek ?", Snackbar.LENGTH_LONG)
                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                        .setTextColor(Color.LTGRAY)
                        .setGestureInsetBottomIgnored(true)
                        .questionSnackbar()
                        .setAction(getString(R.string.yes)) {

                            CoroutineScope(Dispatchers.IO).launch {

                                val postRef = firestore.collection("Posts").document(txtAdminPostCommentId.text.toString())

                                val actionRef = postRef.collection("PostAction").document(txtAdminPostCommentUserId.text.toString())

                                actionRef
                                    .delete()
                                    .addOnCompleteListener { updateTask ->
                                        if(updateTask.isSuccessful) logsnackbar("Beğeni silindi", "info").also { clearCommentUi() }
                                        else {

                                            actionRef
                                                .get(Source.SERVER)
                                                .addOnCompleteListener { actionTask ->

                                                    when(actionTask.isSuccessful) {

                                                        true -> {

                                                            if(actionTask.result.exists()) logsnackbar("Beğeni silinemedi", "error")
                                                            else {
                                                                postRef
                                                                    .get(Source.SERVER)
                                                                    .addOnSuccessListener { postTask ->
                                                                        if(postTask.exists()) logsnackbar("Kullanıcı silinmiş", "error")
                                                                        else logsnackbar("İçerik silinmiş", "error")
                                                                    }
                                                                    .addOnFailureListener { logsnackbar("Beğeni silinemedi", "error") }
                                                            }

                                                        }

                                                        else -> logsnackbar("Beğeni silinemedi", "error")

                                                    }

                                                }

                                        }

                                    }

                            }

                        }
                        .setActionTextColor(Color.RED)
                        .widthSettings()
                        .show()

                }

                return@setOnLongClickListener true
            }

        }


        // Sistem mesajı layout

        binding.apply {

            txtAdminSystemMessageTr.openBottomSheet()
            txtAdminSystemMessageEn.openBottomSheet()

            btnAdminSendSystemMessage.setOnLongClickListener {

                if(txtAdminSystemMessageTr.length() > 0 || txtAdminSystemMessageEn.length() > 0) {

                    checkContext {

                        Snackbar
                            .make(requireContext(), root, "Sistem mesajı gönder ?", Snackbar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                            .setTextColor(Color.LTGRAY)
                            .setGestureInsetBottomIgnored(true)
                            .questionSnackbar()
                            .setAction(getString(R.string.yes)) {

                                val updateData = mutableMapOf <String, Any?> ()

                                if(txtAdminSystemMessageTr.length() > 0) {
                                    updateData["message_tr"] = txtAdminSystemMessageTr.text.toString()
                                }

                                if(txtAdminSystemMessageEn.length() > 0) {
                                    updateData["message_en"] = txtAdminSystemMessageEn.text.toString()
                                }

                                updateData["messageDate"] = FieldValue.serverTimestamp()

                                firestore
                                    .collection("Notification")
                                    .document("Notification")
                                    .update(updateData)
                                    .addOnCompleteListener {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            when(it.isSuccessful) {
                                                true -> {
                                                    logsnackbar("Sistem mesajı güncellendi", "info")
                                                    txtAdminSystemMessageTr.text?.clear()
                                                    txtAdminSystemMessageEn.text?.clear()
                                                }
                                                else -> {
                                                    logsnackbar("Sistem mesajı güncellenemedi", "error")
                                                    logsnackbar(it.exception?.msg()!!, "error")
                                                }
                                            }
                                        }
                                    }


                            }
                            .setActionTextColor(Color.GREEN)
                            .widthSettings()
                            .show()

                    }

                }

                return@setOnLongClickListener true
            }

        }


        // E-mail Block kaldırma layout

        binding.apply {

            btnDeleteBlockMail.setOnLongClickListener {

                checkContext {

                    txtDeleteBlockMail.text?.let { mail ->

                        Snackbar
                            .make(requireContext(), root, "Engeli kaldır ?", Snackbar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                            .setTextColor(Color.LTGRAY)
                            .setGestureInsetBottomIgnored(true)
                            .questionSnackbar()
                            .setAction(getString(R.string.yes)) {

                                CoroutineScope(Dispatchers.IO).launch {

                                    firestore
                                        .collection("Block")
                                        .whereEqualTo("blockMail", mail.toString())
                                        .get(Source.SERVER)
                                        .addOnCompleteListener { task ->
                                            when(task.isSuccessful) {

                                                true -> {
                                                    when(task.dataAvailable()) {
                                                        true ->  {
                                                            val blockDoc = task.result.documents[0].getString("blockId") ?: "-"
                                                            firestore
                                                                .collection("Block")
                                                                .document(blockDoc)
                                                                .delete()
                                                                .addOnCompleteListener { delete ->

                                                                    when(delete.isSuccessful) {
                                                                        true -> logsnackbar("Engel kaldırıldı", "info")
                                                                        else -> {
                                                                            logsnackbar("Engel kaldırılamadı", "error")
                                                                            delete.exception?.msg()?.let {
                                                                                logsnackbar(it, "error")
                                                                            }
                                                                        }
                                                                    }

                                                                }

                                                        }
                                                        else -> logsnackbar("Engelli mail bulunamadı", "error")
                                                    }
                                                }

                                                else -> {
                                                    logsnackbar("Engel kaldırılamadı", "error")
                                                    task.exception?.msg()?.let { logsnackbar(it, "error") }
                                                }

                                            }
                                        }

                                }


                            }
                            .setActionTextColor(Color.GREEN)
                            .widthSettings()
                            .show()

                    }

                }

                return@setOnLongClickListener false
            }

        }


        // Block kaldırma layout

        binding.apply {

            btnGetBlock.setOnClickListener {

                firestore
                    .collection("Block")
                    .orderBy("blockDate", Query.Direction.ASCENDING)
                    .limit(1)
                    .get(Source.SERVER)
                    .addOnCompleteListener { blockTask ->

                        when(blockTask.isSuccessful) {

                            true -> when(blockTask.result.documents.isNotEmpty()) {

                                true -> linearDeleteBlockDate.show().also {
                                    txtDeleteBlock.show()
                                    btnDeleteBlock.show()
                                    txtDeleteBlock.text = blockTask.result.documents[0].getString("blockId") ?: "-"
                                    blockDate = blockTask.result.documents[0].getTimestamp("blockDate") ?: Timestamp.now()
                                    blockCalculateDate = (blockTask.result.documents[0].getTimestamp("blockDate") ?: Timestamp.now()).seconds
                                }

                                else -> logsnackbar("Block listesi boş", "info").also {
                                    txtDeleteBlock.gone()
                                    linearDeleteBlockDate.gone()
                                    btnDeleteBlock.gone()
                                }
                            }

                            false -> logsnackbar("Block listesi kontrol edilemedi", "error").also {
                                txtDeleteBlock.gone()
                                linearDeleteBlockDate.gone()
                                btnDeleteBlock.gone()
                            }

                        }

                    }

            }

            btnDeleteBlock.setOnLongClickListener {

                checkContext {

                    Snackbar
                        .make(requireContext(), root, "Engeli kaldır ?", Snackbar.LENGTH_LONG)
                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                        .setTextColor(Color.LTGRAY)
                        .setGestureInsetBottomIgnored(true)
                        .questionSnackbar()
                        .setAction(getString(R.string.yes)) {

                            CoroutineScope(Dispatchers.IO).launch {

                                firestore
                                    .collection("Block")
                                    .document(txtDeleteBlock.text.toString())
                                    .delete()
                                    .addOnCompleteListener { delete ->

                                        when(delete.isSuccessful) {

                                            true -> logsnackbar("Engel kaldırıldı", "info").also {
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    txtDeleteBlock.gone()
                                                    linearDeleteBlockDate.gone()
                                                    btnDeleteBlock.gone()
                                                }
                                            }
                                            else -> {
                                                logsnackbar("Engel kaldırılamadı", "error")
                                                delete.exception?.msg()?.let { logsnackbar(it, "error") }
                                            }

                                        }

                                    }

                            }


                        }
                        .setActionTextColor(Color.GREEN)
                        .widthSettings()
                        .show()

                }

                return@setOnLongClickListener true
            }

        }


        // Silinen profilleri sunucudan kaldırma

        binding.apply {

            txtDeleteAccount.openBottomSheet()


            btnGetDisableAccount.setOnClickListener {

                CoroutineScope(Dispatchers.IO).launch {

                    firestore
                        .collection("Users")
                        .whereEqualTo("userIsActive", false)
                        .orderBy("userRegisterDate", Query.Direction.ASCENDING)
                        .limit(1)
                        .get(Source.SERVER)
                        .addOnCompleteListener {
                            CoroutineScope(Dispatchers.Main).launch {

                                when(it.isSuccessful) {

                                    true -> when(it.result.documents.size) {
                                        0 -> logsnackbar("Kullanıcı bulunamadı", "info")
                                        else -> {
                                            btnCheckSocial.show()
                                            txtDeleteAccount.setText(it.result.documents[0].toUserModel().userId)
                                        }
                                    }

                                    else -> logsnackbar("Kullanıcı arama başarısız", "error").also { btnCheckSocial.gone() }

                                }

                            }
                        }

                }

            }

            btnCheckSocial.setOnClickListener {

                txtDeleteAccount.text?.let { userId ->

                    val userRef = firestore.collection("Users").document(userId.toString())

                    val followerRef = userRef
                        .collection("UserFollower")
                        .orderBy("actionDate", Query.Direction.ASCENDING)
                        .limit(5)

                    val followingRef = userRef
                        .collection("UserFollowing")
                        .orderBy("actionDate", Query.Direction.ASCENDING)
                        .limit(5)

                    CoroutineScope(Dispatchers.IO).launch {

                        followerRef
                            .get(Source.SERVER)
                            .addOnSuccessListener { followerTask ->

                                socialUsers()

                                clearSocialUsers()

                                when(followerTask.documents.size) {

                                    0 -> {

                                        followingRef
                                            .get(Source.SERVER)
                                            .addOnSuccessListener { followingTask ->

                                                when(followingTask.documents.size) {
                                                    0 -> {
                                                        logsnackbar("Social bulunamadı", "info")
                                                        CoroutineScope(Dispatchers.Main).launch { btnDeleteProfilePicture.show() }
                                                    }
                                                    else -> writeSocialUsers(followingTask.documents, "following")
                                                }

                                            }
                                            .addOnFailureListener {
                                                logsnackbar("Social (Following) bilgilerine ulaşılamadı", "error")
                                                socialUsers()
                                                clearSocialUsers()
                                            }

                                    }

                                    else -> writeSocialUsers(followerTask.documents, "follower")

                                }
                            }
                            .addOnFailureListener {
                                logsnackbar("Social (Follower) bilgilerine ulaşılamadı", "error")
                                socialUsers()
                                clearSocialUsers()
                            }


                    }

                }

            }

            btnDeleteProfilePicture.setOnLongClickListener {

                storage
                    .reference
                    .child(txtDeleteAccount.text.toString() + ".webp")
                    .delete()
                    .addOnCompleteListener { deleteTask ->
                        CoroutineScope(Dispatchers.Main).launch {
                            when(deleteTask.isSuccessful) {
                                true -> {
                                    logsnackbar("Resim silindi", "info")
                                    btnDeleteProfilePicture.gone()
                                    btnDeleteProfile.show()
                                }
                                else -> {
                                    deleteTask.exception?.let { exception ->  
                                        if((exception as StorageException).errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                                            logsnackbar("Resim mevcut değil", "info")
                                            btnDeleteProfilePicture.gone()
                                            btnDeleteProfile.show()
                                        }
                                        else logsnackbar("Resim silinemedi", "error")
                                    }
                                }
                            }
                        }
                    }

                return@setOnLongClickListener true
            }

            btnDeleteProfile.setOnLongClickListener {

                txtDeleteAccount.text?.let { userId ->

                    checkContext {

                        Snackbar
                            .make(requireContext(), root, "Profil silinecek ?", Snackbar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                            .setTextColor(Color.LTGRAY)
                            .setGestureInsetBottomIgnored(true)
                            .questionSnackbar()
                            .setAction(getString(R.string.yes)) {

                                CoroutineScope(Dispatchers.IO).launch {

                                    firestore
                                        .collection("Users")
                                        .document(userId.toString())
                                        .delete()
                                        .addOnCompleteListener {
                                            CoroutineScope(Dispatchers.Main).launch {

                                                when(it.isSuccessful) {
                                                    true -> {
                                                        logsnackbar("Profil silindi", "info")
                                                        txtDeleteAccount.text?.clear()
                                                        btnCheckSocial.gone()
                                                        btnDeleteProfile.gone()
                                                    }
                                                    else -> {
                                                        logsnackbar("Profil silinemedi", "error")
                                                    }
                                                }

                                            }
                                        }

                                }

                            }
                            .setActionTextColor(Color.RED)
                            .widthSettings()
                            .show()

                    }

                }

                return@setOnLongClickListener true

            }


            val socialListener = View.OnClickListener {

                txtDeleteAccount.text?.let { userId ->

                    val selectedId = when(it.id) {
                        btnSocialAction1.id -> txtSocialAction1
                        btnSocialAction2.id -> txtSocialAction2
                        btnSocialAction3.id -> txtSocialAction3
                        btnSocialAction4.id -> txtSocialAction4
                        else -> txtSocialAction5
                    }

                    val deleteSocialRef = firestore
                        .collection("Users")
                        .document(userId.toString())
                        .collection(if(selectedId.currentTextColor == greenColor) "UserFollower" else "UserFollowing")
                        .document(selectedId.text.toString())

                    CoroutineScope(Dispatchers.IO).launch {

                        deleteSocialRef
                            .delete()
                            .addOnCompleteListener { deleteTask ->
                                when(deleteTask.isSuccessful) {
                                    true -> {
                                        logsnackbar("Social silindi", "info")
                                        CoroutineScope(Dispatchers.Main).launch {
                                            when(it.id) {
                                                btnSocialAction1.id -> linearSocialAction1.gone()
                                                btnSocialAction1.id -> linearSocialAction2.gone()
                                                btnSocialAction1.id -> linearSocialAction3.gone()
                                                btnSocialAction1.id -> linearSocialAction4.gone()
                                                else -> linearSocialAction5.gone()
                                            }
                                        }
                                    }
                                    else -> logsnackbar("Social silinemedi", "error")
                                }
                            }

                    }

                }

            }

            btnSocialAction1.setOnClickListener(socialListener)

            btnSocialAction2.setOnClickListener(socialListener)

            btnSocialAction3.setOnClickListener(socialListener)

            btnSocialAction4.setOnClickListener(socialListener)

            btnSocialAction5.setOnClickListener(socialListener)

        }


        popupLike.setOnMenuItemClickListener {

            binding.txtAdminPostMode.apply {
                text = it.title.toString()
                setCompoundDrawablesWithIntrinsicBounds(it.icon,null,null,null)
            }

            return@setOnMenuItemClickListener true
        }

        popupCategory.setOnMenuItemClickListener {

            binding.txtAdminPostCategory.apply {
                text = it.title.toString()
                setCompoundDrawablesWithIntrinsicBounds(it.icon,null,null,null)
            }

            return@setOnMenuItemClickListener true
        }


        adminViewModel.getReviewUser().observe(viewLifecycleOwner, {

            // Yeni kullanıcı inceleme

            CoroutineScope(Dispatchers.IO).launch {

                firestore
                    .collection("Users")
                    .whereEqualTo("userId", it)
                    .limit(1)
                    .get(Source.SERVER)
                    .addOnCompleteListener {
                        CoroutineScope(Dispatchers.Main).launch {
                            when(it.dataAvailable()) {
                                true -> {
                                    binding.apply {
                                        user = it.result.documents[0].toUserModel()
                                        hideLayoutsAndOpen(linearAdminEditProfile)
                                    }
                                    logsnackbar("Kullanıcı getirildi", "info")
                                }
                                else -> {
                                    logsnackbar("Kullanıcı getirilemedi", "error")
                                    it.exception?.msg()?.let { msg -> logsnackbar(msg, "error") }
                                }
                            }
                        }
                    }

            }

        })

        adminViewModel.getReviewPost().observe(viewLifecycleOwner, {

            // Yeni post inceleme

            CoroutineScope(Dispatchers.IO).launch {

                firestore
                    .collection("Posts")
                    .whereEqualTo("postId", it)
                    .limit(1)
                    .get(Source.SERVER)
                    .addOnCompleteListener {
                        CoroutineScope(Dispatchers.Main).launch {
                            when(it.dataAvailable()) {
                                true -> {
                                    binding.apply {

                                        post = it.result.documents[0].toPostModel()

                                        getPopup(
                                            it.result.documents[0].toPostModel().postCategoryId,
                                            it.result.documents[0].toPostModel().postLikePublic
                                        )

                                        hideLayoutsAndOpen(linearAdminEditPost)

                                    }
                                    logsnackbar("İçerik getirildi", "info")
                                }
                                else -> {
                                    logsnackbar("İçerik getirilemedi", "error")
                                    it.exception?.msg()?.let { logsnackbar(it, "error") }
                                }
                            }
                        }
                    }

            }

        })

        adminViewModel.getReviewComment().observe(viewLifecycleOwner, { commentData ->

            // Yorum inceleme

            commentData?.let {

                CoroutineScope(Dispatchers.IO).launch {

                    firestore
                        .collection("Posts")
                        .document(it["postId"]!!)
                        .collection("PostAction")
                        .document(it["actionUserId"]!!)
                        .get(Source.SERVER)
                        .addOnCompleteListener {
                            CoroutineScope(Dispatchers.Main).launch {

                                when(it.dataAvailable()) {
                                    true -> {
                                        binding.apply {
                                            comment = it.result.toLikeDislikeModel()

                                            hideLayoutsAndOpen(linearAdminEditPostComment)
                                        }
                                        logsnackbar("Yorum getirildi", "info")
                                    }
                                    else -> {
                                        logsnackbar("Yorum getirilemedi", "error")
                                        it.exception?.msg()?.let { logsnackbar(it, "error") }
                                    }
                                }

                            }
                        }

                }

            }

        })

    }



    @SuppressLint("NotifyDataSetChanged")
    private fun getContactAndReport() {

        CoroutineScope(Dispatchers.IO).launch {

            firestore
                .collection("Contact")
                .orderBy("contactDate", Query.Direction.ASCENDING)
                .limit(1)
                .get(Source.SERVER)
                .addOnSuccessListener { query ->

                    if (!query.documents.isNullOrEmpty()) {

                        query.documents[0]?.let {
                            adminViewModel.setContact(it.toContactModel())
                            binding.recyclerAdminContact.adapter?.notifyDataSetChanged()
                        }

                    }

                }

            firestore
                .collection("Reports")
                .orderBy("reportDate", Query.Direction.ASCENDING)
                .limit(1)
                .get(Source.SERVER)
                .addOnSuccessListener { query ->

                    if (!query.documents.isNullOrEmpty()) {

                        query.documents[0]?.let {
                            adminViewModel.setReport(it.toReportModel())
                            binding.recyclerAdminReport.adapter?.notifyDataSetChanged()
                        }

                    }

                }

        }

    }



    private fun controlPassword() {

        CoroutineScope(Dispatchers.IO).launch {

            firestore
                .collection("System")
                .document("Login")
                .get(Source.SERVER)
                .addOnFailureListener {
                    logsnackbar("Giriş yapılamadı", "error")
                    logsnackbar(it.msg(), "error")
                }
                .addOnSuccessListener {

                    var status = false

                    checkContext {

                        for (i in 1..5) {

                            if((it.getString("password_$i") ?: "-").matches(Regex(binding.txtAdminLogin.text.toString()))) {

                                status = true

                                getContactAndReport()

                                CoroutineScope(Dispatchers.Main).launch {

                                    binding.linearAdminLogin.gone()

                                    binding.recyclerAdminReport.show()

                                    binding.recyclerAdminContact.show()

                                    for (ui : LinearLayout in arrayLayouts) ui.show()

                                    hideLayoutsAndOpen(null)

                                }

                                break

                            }

                        }

                    }

                    CoroutineScope(Dispatchers.Main).launch {

                        when(status) {
                            true -> logsnackbar("Giriş yapıldı", "info")
                            else -> logsnackbar("Giriş yapılamadı", "error")
                        }

                        binding.txtAdminLogin.text?.clear()

                    }

                }

        }

    }

    private fun copyClipBoard(value : String) {

        CoroutineScope(Dispatchers.Main).launch {

            clipboardManager.setPrimaryClip(ClipData.newPlainText(value, value))

            checkContext { Toast.makeText(requireContext(), "Panoya Kopyalandı", Toast.LENGTH_SHORT).show() }

        }

    }



    private fun logsnackbar(msg : String, type : String) {

        CoroutineScope(Dispatchers.Main).launch {

            if (type.matches(Regex("info"))) Log.i("AdminFrag", msg)
            else Log.e("AdminFrag", msg)

            checkContext {

                Snackbar
                    .make(requireContext(), binding.root, msg, Snackbar.LENGTH_SHORT)
                    .setDuration(1150)
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                    .setTextColor(
                        if (type.matches(Regex("info"))) Color.GREEN
                        else Color.RED
                    )
                    .setGestureInsetBottomIgnored(true)
                    .widthSettings()
                    .show()

            }


        }

    }


    private fun clearProfileUi() {

        binding.apply {

            chkAdminEditPp.isChecked = false
            chkAdminEditRealName.isChecked = false
            chkAdminEditUserName.isChecked = false
            chkAdminEditBiography.isChecked = false
            chkAdminEditMail.isChecked = false
            chkAdminEditInstagram.isChecked = false
            chkAdminEditTwitter.isChecked = false
            chkAdminEditFacebook.isChecked = false
            chkAdminEditFollowing.isChecked = false
            chkAdminEditFollower.isChecked = false
            chkAdminEditReport.isChecked = false
            chkAdminEditSpamPost.isChecked = false
            chkAdminEditSpamComment.isChecked = false
            chkAdminEditSpamReport.isChecked = false
            chkAdminEditSpamContact.isChecked = false
            chkAdminEditAddPost.isChecked = false
            chkAdminEditAddComment.isChecked = false
            chkAdminEditAddReport.isChecked = false
            chkAdminEditAddContact.isChecked = false
            chkAdminEditProfile.isChecked = false
            chkAdminEditConfirm.isChecked = false
            chkAdminEditActive.isChecked = false

        }

    }

    private fun clearPostUi() {

        binding.apply {

            post = PostModel(
                0,
                "-",
                postLikePublic = true,
                "-",
                "-",
                null,
            )

            chkAdminEditCategory.isChecked = false
            chkAdminEditPostMode.isChecked = false
            chkAdminEditPostContent.isChecked = false

            getPopup(0,true)

        }

    }

    private fun clearCommentUi() {

        CoroutineScope(Dispatchers.Main).launch {

            binding.apply {

                comment = LikeDislikeModel(
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    false,
                    null
                )

            }

        }

    }



    private fun hideLayoutsAndOpen(selectLayout : LinearLayout?) {

        val width = LinearLayout.LayoutParams.MATCH_PARENT

        for (ui : LinearLayout in arrayLayouts) {

            ui.let {

                when (ui) {

                    selectLayout -> {

                        if (ui.layoutParams.height != WindowManager.LayoutParams.WRAP_CONTENT) {
                            val params = LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT)
                            params.setMargins(10,25,10,0)
                            ui.layoutParams = params
                        }

                    }

                    else -> {
                        if (ui.layoutParams.height == WindowManager.LayoutParams.WRAP_CONTENT) {
                            val params = LinearLayout.LayoutParams(width, 150)
                            params.setMargins(10,25,10,0)
                            ui.layoutParams = params
                        }

                    }

                }

            }

        }

        binding.scrollAdmin.smoothScrollTo(0,0)

    }



    private fun writeSocialUsers(socialList : List<DocumentSnapshot>, type : String) {

        CoroutineScope(Dispatchers.Main).launch {

            binding.apply {

                for(document : DocumentSnapshot in socialList) {
                    arraySocialUsersLayout[socialList.indexOf(document)].show()
                    arraySocialUsers[socialList.indexOf(document)].apply {
                        text = document.toSocialModel().userId
                        setTextColor(if(type.matches(Regex("follower"))) greenColor else redColor)
                    }
                }

            }

        }
    }

    private fun socialUsers() {
        CoroutineScope(Dispatchers.Main).launch {

            for (layout : LinearLayout in arraySocialUsersLayout) layout.gone()

        }
    }

    private fun clearSocialUsers() {
        CoroutineScope(Dispatchers.Main).launch { for(textView : TextView in arraySocialUsers) textView.text = "-" }
    }


    private fun TextInputEditText.openBottomSheet() {
        setOnLongClickListener {

            checkContext {

                text?.let {

                    BottomSheetDialog(requireActivity(), R.style.BottomSheetDialogTheme).apply {
                        val bindingBottom = BottomDialogPostMoreBinding
                            .inflate(LayoutInflater.from(requireActivity()), null, false)
                        bindingBottom.content = it.toString()
                        setContentView(bindingBottom.root)
                        show()
                    }

                }

            }

            return@setOnLongClickListener false
        }
    }

    private fun Editable?.safeRealName() : String = if(toString().isEmpty()) "user" else toString()

    private fun Editable?.safeUserName() : String = when(toString().isEmpty()) {
        true -> "user" + Random.nextInt(10000,99999).toString()
        else -> toString()
    }

    private fun Editable?.safeText() : String = if(toString().isEmpty()) "-" else toString()

    private fun Editable?.safeCount() : Int = if(toString().isEmpty()) 0 else toString().toInt()

    private fun getPopup(category : Int, likePublic : Boolean) {

        popupCategory.menu.getItem(category)
            .let { item ->
                item.isChecked = true
                binding.txtAdminPostCategory.apply {
                    text = item.title.toString()
                    setCompoundDrawablesWithIntrinsicBounds(item.icon, null, null, null)
                }
            }

        popupLike.menu.getItem(if(likePublic) 0 else 1)
            .let { item ->
                item.isChecked = true
                binding.txtAdminPostMode.apply {
                    text = item.title.toString()
                    setCompoundDrawablesWithIntrinsicBounds(item.icon, null, null, null)
                }
            }

    }


}