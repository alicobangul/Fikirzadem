package com.basesoftware.fikirzadem.presentation.ui.editprofile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.FragmentEditProfileBinding
import com.basesoftware.fikirzadem.model.recycler.NotificationRecyclerModel
import com.basesoftware.fikirzadem.presentation.viewmodel.EditProfileViewModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.dataAvailable
import com.basesoftware.fikirzadem.util.ExtensionUtil.downloadFromDrawable
import com.basesoftware.fikirzadem.util.ExtensionUtil.downloadFromUrl
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.ExtensionUtil.exceptionMsg
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.questionSnackbar
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil.snackbarColor
import com.basesoftware.fikirzadem.util.WorkUtil.systemLanguage
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

class EditProfileFragment : Fragment() {

    private var _binding : FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth : FirebaseAuth

    private lateinit var readPermissionLauncher : ActivityResultLauncher<String>
    private lateinit var writePermissionLauncher : ActivityResultLauncher<String>

    private lateinit var galleryImageLauncher : ActivityResultLauncher<Intent>
    private lateinit var cropImageLauncher : ActivityResultLauncher<Intent>


    private val sharedViewModel : SharedViewModel by activityViewModels { SharedViewModel.provideFactory(requireActivity().application, requireActivity()) }
    private val editViewModel : EditProfileViewModel by viewModels()



    override fun onCreateView(inf: LayoutInflater, cont: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(layoutInflater, cont, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launcherResults()

        listener()

        initializeEdit()

    }


    override fun onConfigurationChanged(newConfig: Configuration) {

        super.onConfigurationChanged(newConfig)

    }

    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()

    }



    private fun launcherResults() {

        // Galeriden okuma izni
        readPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->

            when(permission) {
                true -> permissionWriteGallery() // Galeriden okuma izni verildi, yazma izni kontrol ediliyor
                else -> snackbarLog("Edit-registerLauncher", R.string.permission_fail, "error")
            }

        }

        // Galeriye yazma izni
        writePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->

            when(permission) {
                true -> {
                    galleryImageLauncher.launch(
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply { type = "image/*" }
                    )
                }
                else -> snackbarLog("Edit-registerLauncher", R.string.permission_fail, "error")
            }

        }

        // Galeriden resim seçildi mi seçilmedi mi ?
        galleryImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

//            when(result.resultCode == RESULT_OK) {
//
//                true -> {
//
//                    result.data?.data?.let {
//
//                        cropImageLauncher.launch(
//                            CropImage
//                                .activity(it)
//                                .setAspectRatio(1,1)
//                                .setMinCropResultSize(75,75)
//                                .getIntent(requireActivity())
//                        )
//
//                    } ?: snackbarLog("Edit-registerLauncher", R.string.unselected, "error")
//
//                }
//
//                else -> snackbarLog("Edit-registerLauncher", R.string.unselected, "error")
//
//            }

        }

        // ImageCropper ile resim kırpma sonrası alınan uri
        cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

//            when(result.resultCode == RESULT_OK) {
//
//                true -> when(result.data != null) {
//                    true -> {
//                        CropImage.getActivityResult(result.data!!)?.uri?.let {
//                            uriImageToWebp(it)
//                            binding.imgEditPpOriginal.downloadFromUri(it)
//                            binding.imgEditPpMedium.downloadFromUri(it)
//                            binding.imgEditPpSmall.downloadFromUri(it)
//                        }
//                    }
//                    else -> snackbarLog("Edit-registerLauncher", R.string.unselected, "error")
//                }
//
//                else -> snackbarLog("Edit-registerLauncher", R.string.unselected, "error")
//
//            }

        }


    }

    private fun initializeEdit() {

        // SharedMVVM kontrol et eğer editprofile false ise profil editleme yasaklı güncelleme yapamaz.

        firestore = WorkUtil.firestore()
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()

        editViewModel.getUiEnable().observe(viewLifecycleOwner) { binding.edit = editViewModel }

        editViewModel.getReAuth().observe(viewLifecycleOwner) {

            binding.edit = editViewModel
            binding.shared = sharedViewModel

            when (editViewModel.getNowImgStatus()) {
                "myProfile" -> {
                    binding.imgEditPpOriginal.downloadFromUrl(sharedViewModel.getUser()?.userProfilePicture.toString())
                    binding.imgEditPpMedium.downloadFromUrl(sharedViewModel.getUser()?.userProfilePicture.toString())
                    binding.imgEditPpSmall.downloadFromUrl(sharedViewModel.getUser()?.userProfilePicture.toString())
                }
                "default" -> {
                    checkContext {
                        val drawable = AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.login_user_ico
                        )!!
                        binding.imgEditPpOriginal.downloadFromDrawable(drawable)
                        binding.imgEditPpMedium.downloadFromDrawable(drawable)
                        binding.imgEditPpSmall.downloadFromDrawable(drawable)
                    }
                }
                "newImage" -> {
                    checkContext {
                        val resources = requireContext().resources
                        val array = editViewModel.getSelectImgByteArray()!!
                        val drawable = BitmapDrawable(
                            resources,
                            BitmapFactory.decodeByteArray(array, 0, array.size)
                        )
                        binding.imgEditPpOriginal.downloadFromDrawable(drawable)
                        binding.imgEditPpMedium.downloadFromDrawable(drawable)
                        binding.imgEditPpSmall.downloadFromDrawable(drawable)
                    }
                }
            }

            if (it) {
                chkVerify()
            }

        }

        /**
         * Yarım kalan bir işlem var ise devam ediliyor
         * Eğer herhangi bir işlem yok ise ui enable ile varsayılan konuma alınıyor
         *
         */
        when(editViewModel.getCurrentFunction().isNullOrEmpty()) {

            true -> editViewModel.setUiEnable(true)

            false -> {

                editViewModel.getCurrentFunction()?.let {
                    when(it) {

                        "checkData" -> checkData()
                        "deleteAccount" -> deleteAccount()
                        "editInput" -> editInput()
                        "imageUpdateAndCreateUrl" -> imageUpdateAndCreateUrl()
                        "login" -> login()
                        "profileUpdate" -> profileUpdate()
                        "reAuthenticate" -> reAuthenticate(false)
                        "sendUpdate" -> sendUpdate()
                        "sendVerifyMail" -> sendVerifyMail()
                        "updateMail" -> updateMail()
                        "updateMailCheckBlockCache" -> updateMailCheckBlockCache()
                        "updateMailCheckBlockServer" -> updateMailCheckBlockServer()
                        "updateMailWriteProfile" -> updateMailWriteProfile()
                        "updatePassword" -> updatePassword()

                    }
                }

            }
        }


    }


    private fun listener() {

        binding.apply {

            btnEditLogin.setOnClickListener { login() }


            imgEditRealName.setOnClickListener { txtEditRealName.setText(shared?.getUser()?.userRealName.toString()) }
            imgEditUserName.setOnClickListener { txtEditUserName.setText(shared?.getUser()?.userName.toString()) }
            imgEditBiography.setOnClickListener { txtEditBiography.setText(shared?.getUser()?.userBiography.toString()) }
            imgEditInstagram.setOnClickListener { txtEditInstagram.setText("-") }
            imgEditTwitter.setOnClickListener { txtEditTwitter.setText("-") }
            imgEditFacebook.setOnClickListener { txtEditFacebook.setText("-") }


            btnSendVerifyMail.setOnClickListener { sendVerifyMail() }

            btnChkVerify.setOnClickListener { reAuthenticate(false) }



            val imgButtonListener = View.OnClickListener {
                when(!shared?.getUser()?.userEmailConfirm!!) {
                    true -> {
                        when(it) {
                            btnDeleteNewPp -> {
                                editViewModel.apply {
                                    setNowImgStatus("default")
                                    setNewPpLink("default")
                                }
                                checkContext {
                                    val drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.login_user_ico)!!
                                    imgEditPpOriginal.downloadFromDrawable(drawable)
                                    imgEditPpMedium.downloadFromDrawable(drawable)
                                    imgEditPpSmall.downloadFromDrawable(drawable)
                                }
                            }
                            else -> snackbarLog("Edit-imgButtonListener", R.string.email_confirm,"info")
                        }
                    }
                    false -> {
                        when(it) {
                            btnSelectNewPp -> selectImage()
                            btnDeleteNewPp -> {
                                editViewModel.apply {
                                    setNowImgStatus("default")
                                    setNewPpLink("default")
                                }
                            }
                            btnCancelNewPp -> {
                                editViewModel.apply {
                                    setNowImgStatus("myProfile")
                                    setNewPpLink(shared?.getUser()?.userProfilePicture.toString())
                                }
                            }
                        }
                        if(it != btnSelectNewPp) {
                            editViewModel.setSelectImgByteArray(null)
                            imgEditPpOriginal.downloadFromUrl(editViewModel.getNewPpLink())
                            imgEditPpMedium.downloadFromUrl(editViewModel.getNewPpLink())
                            imgEditPpSmall.downloadFromUrl(editViewModel.getNewPpLink())
                        }
                    }
                }
            }

            btnSelectNewPp.setOnClickListener(imgButtonListener)

            btnDeleteNewPp.setOnClickListener(imgButtonListener)

            btnCancelNewPp.setOnClickListener(imgButtonListener)



            btnUpdateSend.setOnClickListener {

                CoroutineScope(Dispatchers.IO).launch {

                    editViewModel.setUiEnable(false)

                    when (shared?.getUser()!!.userEditProfile) {

                        true -> editInput() // Inputlar düzenlenip kaydetmeye işlemi başlatılacak

                        else -> {

                            if(
                                sharedViewModel.getUser()?.userProfilePicture?.matches(Regex("default")) == false
                                && editViewModel.getNewPpLink().matches(Regex("default")))
                            {
                                storage.reference.child(shared?.getMyUserId()+".webp")
                                    .delete()
                                    .addOnCompleteListener {

                                        firestore
                                            .collection("Users")
                                            .document(sharedViewModel.getMyUserId())
                                            .update("userProfilePicture","default")
                                            .addOnCompleteListener {

                                                editViewModel.apply {
                                                    setCurrentFunction(null)
                                                    setUiEnable(true)
                                                    setReAuth(false)
                                                }

                                                when(it.isSuccessful) {
                                                    true -> {
                                                        snackbarLog(
                                                            "Edit-listener",
                                                            R.string.profile_update_success,
                                                            "info"
                                                        )
                                                    }
                                                    else -> {
                                                        snackbarLog(
                                                            "Edit-listener",
                                                            R.string.profile_update_fail,
                                                            "error"
                                                        )
                                                    }
                                                }
                                            }

                                    }
                            }
                            else {
                                snackbarLog("Edit-BtnUpdateSend", R.string.user_edit_banned,"info")
                                editViewModel.setUiEnable(true)
                            }

                        }

                    }

                }

            }

            btnChangeMail.setOnClickListener {

                editViewModel.setUiEnable(false)

                if(txtEditMail.length() > 3) {

                    editViewModel.setNewMail(txtEditMail.text.toString())

                    when(editViewModel.getNewMail().matches(Regex(shared?.getUser()?.userMail.toString()))) {

                        true -> {
                            snackbarLog("EditProfile-ChangeMail", R.string.new_mail_change_fail, "info")
                            editViewModel.setUiEnable(true)
                        }

                        false -> {

                            val mail = txtEditMail.text.toString().indexOf("@")
                            when (mail < 0) {
                                // Eğer @ işareti yoksa otomatik hata verecek
                                true -> {
                                    snackbarLog("EditProfile-ChangeMail", R.string.email_error, "error")
                                    editViewModel.setUiEnable(true)
                                }
                                false -> updateMailCheckBlockCache()
                                // Eğer e-mail sorunsuz ise txtEmail değişkenine ver
                            }

                        }

                    }

                }

                else {
                    snackbarLog("EditProfile-ChangeMail", R.string.email_error, "error")
                    editViewModel.setUiEnable(true)
                }

                txtEditMail.setText("")

            }

            btnChangePassword.setOnClickListener {
                editViewModel.setUiEnable(false)

                if (txtNewPass.length() >= 6) {
                    editViewModel.setNewPass(txtNewPass.text.toString())

                    when(editViewModel.getNewPass().matches(Regex(editViewModel.getCurrentPassword()))) {
                        true -> {
                            snackbarLog("EditProfile-ChangePass", R.string.new_pass_change_fail, "info")
                            editViewModel.setUiEnable(true)
                        }
                        false -> updatePassword()
                    }

                }

                else {
                    snackbarLog("EditProfile-ChangePass", R.string.password_error, "error")
                    editViewModel.setUiEnable(true)
                }


                txtNewPass.setText("")

            }

            btnDeleteAccount.setOnClickListener {

                editViewModel.setUiEnable(false)

                deleteAccount()

            }

        }

    }




    @SuppressLint("LogConditional")
    private fun snackbarLog(tag : String, message : Int, type: String) {

        CoroutineScope(Dispatchers.Main).launch {

            checkContext {

                when(type)
                {
                    "error" -> Log.e(tag, getString(message))
                    "warning" -> Log.w(tag, getString(message))
                    "info" -> Log.i(tag, getString(message))
                }

                if(lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    Snackbar
                        .make(requireContext(), binding.root, getString(message), Snackbar.LENGTH_SHORT)
                        .settings()
                        .widthSettings()
                        .show()
                }

            }

        }

    }

    private fun chkVerify() {

        CoroutineScope(Dispatchers.IO).launch {

            if(auth.currentUser!!.isEmailVerified != sharedViewModel.getUser()?.userEmailConfirm) {
                // Eğer profildeki mail onay durumu auth ile aynı değil ise güncelle
                firestore
                    .collection("Users")
                    .document(sharedViewModel.getMyUserId())
                    .update("userEmailConfirm", auth.currentUser!!.isEmailVerified)
                    .addOnSuccessListener {
                        checkContext {
                            editViewModel.setReAuth(false)
                            Log.i("Edit-chkVerify", "Mail adresi onaylandı")
                        }
                    }
                    .addOnFailureListener { checkContext { Log.i("Edit-chkVerify","Mail adresi onaylanamadı") } }
            }

        }

    }

    private fun deleteWhiteSpace(value : String) : String {

        return Pattern.compile("[\\s]").matcher(value).replaceAll("")

    }



    private fun login() {

        editViewModel.apply {
            setCurrentFunction("login")
            setUiEnable(false)
        }

        when(binding.txtReAuthPass.length() >= 6) {
            true -> {

                try {
                    checkContext {
                        binding.linearEditLogin.requestFocus()
                        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken,0)
                    }
                }
                catch (e : Exception) { Log.w("EditProfile-ChkLogin", e.msg()) }

                // Eğer şifre sorunsuz ise txtpassword değişkenine ver

                editViewModel.apply {
                    setCurrentPassword(binding.txtReAuthPass.text.toString())
                    setCurrentFunction(null)
                }
                reAuthenticate(true)

            }

            false -> {
                // Firebase şifre için minimum 6 karakter istiyor, eğer 6 karakter mevcut değilse hata verecek
                snackbarLog("EditProfile-ChkLogin", R.string.password_error, "error")
                editViewModel.apply {
                    setCurrentFunction(null)
                    setUiEnable(true)
                }
            }
        }

        binding.txtReAuthPass.setText("")

    }

    private fun reAuthenticate(adShow : Boolean) {

        editViewModel.setCurrentFunction("reAuthenticate")

        CoroutineScope(Dispatchers.IO).launch {

            val tryUser = auth.currentUser!!
            val saveUser = auth.currentUser!!
            val tryLogin = EmailAuthProvider.getCredential(sharedViewModel.getUser()?.userMail.toString(), editViewModel.getCurrentPassword())

            tryUser
                .reauthenticate(tryLogin)
                .addOnCompleteListener {
                    editViewModel.apply {
                        setCurrentFunction(null)
                        setUiEnable(true)
                    }
                }
                .addOnSuccessListener {
                    auth.updateCurrentUser(tryUser)
                    /**
                     *  Giriş yapılan kullanıcı auth'da güncellendi
                     *  Böylece mail değişikliğinde verify kontrolü tekrar sağlanabilecek
                     */
                    if(adShow) {
                    }

                    editViewModel.setReAuth(true)
                }
                .addOnFailureListener {
                    /**
                     * Başarısız şifre denemelerinde kullanıcıyı oturumdan atmamak için currentUser kullanılıyor
                     * Giriş başarılı olursa kullanıcı yedeklemesi yapılıyor
                     */
                    auth.updateCurrentUser(saveUser)
                    editViewModel.setReAuth(false)
                    Log.e("editProfile-ReLoginErr",it.msg())
                }

        }

    }


    private fun sendVerifyMail() {

        editViewModel.apply {
            setUiEnable(false)
            setCurrentFunction("sendVerifyMail")
        }

        CoroutineScope(Dispatchers.IO).launch {
            // E-mail onaylama yollandı
            auth.setLanguageCode(systemLanguage())

            auth.currentUser!!.sendEmailVerification()
                .addOnCompleteListener {

                    when(it.isSuccessful) {
                        true -> snackbarLog("Edit-sendVerifyMail", R.string.check_mailbox, "info")
                        else -> {
                            snackbarLog("Edit-sendVerifyMail", R.string.check_mailbox_error, "error")
                            it.exception?.msg()?.let { msg -> Log.e("Edit-sendVerifyMail", msg) }
                        }
                    }

                    editViewModel.apply {
                        setCurrentFunction(null)
                        setUiEnable(true)
                    }

                }
        }

    }


    private fun updatePassword() {

        editViewModel.setCurrentFunction("updatePassword")

        CoroutineScope(Dispatchers.IO).launch {

            val tryUpdatePassUser = auth.currentUser!!
            val saveUpdatePassUser = auth.currentUser!!

            tryUpdatePassUser.updatePassword(editViewModel.getNewPass())
                .addOnCompleteListener {
                    editViewModel.apply {
                        setReAuth(false)
                        setCurrentFunction(null)
                    }
                }
                .addOnSuccessListener {
                    editViewModel.apply {
                        setCurrentPassword(getNewPass())
                        setUiEnable(true)
                    }
                    checkContext {
                        CoroutineScope(Dispatchers.Main).launch {
                            sharedViewModel.setNotification(
                                NotificationRecyclerModel(
                                    R.drawable.notification_ico_profile_edit,
                                    getString(R.string.notification_edit_password),
                                    Timestamp.now().seconds
                                )
                            )
                        }
                        snackbarLog("EditProfile-ChangePass", R.string.new_pass_ok, "info")
                    }

                }
                .addOnFailureListener {
                    auth.updateCurrentUser(saveUpdatePassUser)
                    snackbarLog("EditProfile-ChangePass", R.string.new_pass_fail, "error")
                    Log.e("EditProfile-ChangePass", it.msg())
                    editViewModel.setUiEnable(true)
                }
        }
    }

    private fun deleteAccount() {

        editViewModel.setCurrentFunction("deleteAccount")

        checkContext {

            Snackbar
                .make(requireContext(), binding.root, getString(R.string.delete_account_text), Snackbar.LENGTH_LONG)
                .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                .setBackgroundTint(snackbarColor(requireContext()))
                .setTextColor(Color.LTGRAY)
                .setGestureInsetBottomIgnored(true)
                .questionSnackbar()
                .setAction(getString(R.string.yes)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        firestore
                            .collection("Users")
                            .document(sharedViewModel.getMyUserId())
                            .update("userIsActive", false)
                            .addOnCompleteListener {
                                when(it.isSuccessful) {
                                    true -> Log.i("Edit-DeleteAccount","Hesap silindi")
                                    else -> {
                                        snackbarLog("Edit-DeleteAccount", R.string.delete_account_fail, "error")
                                        it.exception?.msg()?.let { msg -> Log.e("Edit-DeleteAccount", msg) }
                                    }
                                }
                                editViewModel.setCurrentFunction(null)
                            }
                    }
                }
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        editViewModel.apply {
                            setUiEnable(true)
                            setCurrentFunction(null)
                        }
                    }
                })
                .setActionTextColor(Color.GREEN)
                .widthSettings()
                .show()

        }

    }



    private fun editInput() {

        CoroutineScope(Dispatchers.Main).launch {

            editViewModel.setCurrentFunction("editInput")

            binding.apply {

                editViewModel.apply {

                    txtEditRealName.apply {
                        setText(when(text.toString().isEmpty()){
                            true -> shared?.getUser()?.userRealName.toString()
                            else -> text.toString().replace("\\s+".toRegex(), " ").trim()
                        })
                    }

                    txtEditBiography.apply {
                        setText(when(text.toString().isEmpty()){
                            true -> shared?.getUser()?.userBiography.toString()
                            else -> text.toString().replace("\\s+".toRegex(), " ").trim()
                        })
                    }

                    txtEditUserName.apply {
                        setText(when(text.toString().isEmpty()) {
                            true -> shared?.getUser()?.userName.toString().lowercase()
                            else -> deleteWhiteSpace(text.toString()).lowercase()
                        })
                    }


                    txtEditInstagram.apply {
                        setText(when(text.toString().isEmpty()) {
                            true -> shared?.getUser()?.userInstagram.toString()
                            else -> deleteWhiteSpace(text.toString())
                        })
                    }

                    txtEditTwitter.apply {
                        setText(when(text.toString().isEmpty()) {
                            true -> shared?.getUser()?.userTwitter.toString()
                            else -> deleteWhiteSpace(text.toString())
                        })
                    }

                    txtEditFacebook.apply {
                        setText(when(text.toString().isEmpty()) {
                            true -> shared?.getUser()?.userFacebook.toString()
                            else -> deleteWhiteSpace(text.toString())
                        })
                    }

                }

                checkData()
                editViewModel.setCurrentFunction(null)

            }

            /**
             * Realname ve biography gereksiz boşluklar silindi .. -> yazı___yazı__yazı -> yazı_yazı_yazı
             * Username-instagram-twitter-facebook boşluk silindi .. -> _yazı_yazı_ -> yazıyazı
             * Eğer sosyal medya hesaplarında bilgi yok ise -(sil/boş) yazılıyor
             */

        }

    }

    private fun checkData() {

        editViewModel.setCurrentFunction("checkData")

        CoroutineScope(Dispatchers.Default).launch {

            binding.apply {

                editViewModel.apply {

                    when(
                        (txtEditRealName.text.toString() == shared?.getUser()?.userRealName.toString()) &&
                                (txtEditUserName.text.toString() == shared?.getUser()?.userName.toString()) &&
                                (txtEditBiography.text.toString() == shared?.getUser()?.userBiography.toString()) &&
                                (txtEditInstagram.text.toString() == shared?.getUser()?.userInstagram.toString()) &&
                                (txtEditTwitter.text.toString() == shared?.getUser()?.userTwitter.toString()) &&
                                (txtEditFacebook.text.toString() == shared?.getUser()?.userFacebook.toString()) &&
                                getSelectImgByteArray() == null &&
                                getNowImgStatus().matches(Regex("myProfile"))
                    ) {
                        true -> {
                            snackbarLog("Edit-chkData", R.string.send_data_update_fail,"info")
                            CoroutineScope(Dispatchers.Main).launch { editViewModel.setUiEnable(true) }
                        }
                        false -> sendUpdate()
                    }

                    editViewModel.setCurrentFunction(null)
                }

            }

        }

    }

    private fun sendUpdate() {

        editViewModel.setCurrentFunction("sendUpdate")

        CoroutineScope(Dispatchers.Default).launch {

            binding.apply {

                /**
                 * Eğer kullanıcı resmi iptal etti ise şuanki resim link olarak geri aktarılıyor
                 * Eğer kullanıcı resmi kaldırmayı seçti ise resim linki default olarak ayarlanıyor
                 * Ve CloudStorage'da resmi var ise siliniyor...
                 */
                when (editViewModel.getNowImgStatus()) {
                    "myProfile" -> editViewModel.setNewPpLink(shared?.getUser()!!.userProfilePicture)
                    "default" -> {
                        editViewModel.setNewPpLink("default")
                        storage.reference.child(shared?.getMyUserId()+".webp")
                            .delete()
                            .addOnSuccessListener { Log.i("Edit-sendUpdate","Kullanıcı resmi default seçildi mevcut resim silindi") }
                            .addOnFailureListener {
                                Log.e("Edit-sendUpdate","Kullanıcı resmi default seçildi ama mevcut resim silinemedi")
                                Log.e("Edit-sendUpdate",it.msg())
                            }
                    }
                }

                val realNameChk = txtEditRealName.text.toString()
                editViewModel.setNewRealName(when(realNameChk.trim().isNotEmpty()) {
                    true -> realNameChk.replace("\\s+".toRegex(), " ").trim()
                    else -> shared?.getUser()?.userRealName.toString()
                })

                val userNameChk = txtEditUserName.text.toString()
                editViewModel.setNewUserName(when(userNameChk.trim().isNotEmpty() && userNameChk.length > 3) {
                    true -> deleteWhiteSpace(userNameChk)
                    else -> shared?.getUser()?.userName.toString()
                })

                val biographyChk = txtEditBiography.text.toString()
                editViewModel.setNewBiography(when(biographyChk.trim().isNotEmpty()) {
                    true -> deleteWhiteSpace(biographyChk)
                    else -> "-"
                })

                val instagramChk = txtEditInstagram.text.toString()
                editViewModel.setNewInstagram(when(instagramChk.trim().isNotEmpty()) {
                    true -> deleteWhiteSpace(instagramChk)
                    else -> "-"
                })

                val twitterChk = txtEditTwitter.text.toString()
                editViewModel.setNewTwitter(when(twitterChk.trim().isNotEmpty()) {
                    true -> deleteWhiteSpace(twitterChk)
                    else -> "-"
                })

                val facebookChk = txtEditFacebook.text.toString()
                editViewModel.setNewFacebook(when(facebookChk.trim().isNotEmpty()) {
                    true -> deleteWhiteSpace(facebookChk)
                    else -> "-"
                })


                // Kullanıcı adı değiştirilmek isteniyor mu?
                when(userNameChk == shared?.getUser()?.userName.toString()) {
                    true -> {
                        when(editViewModel.getSelectImgByteArray() == null) {
                            true -> profileUpdate() // Profil güncelleniyor
                            false -> imageUpdateAndCreateUrl() // Resim update ediliyor daha sonra profil güncelleniyor
                        }
                        editViewModel.setCurrentFunction(null)
                    }
                    false -> {
                        // Kullanıcı yeni bir kullanıcı adı almak istiyor kontrol et
                        firestore
                            .collection("Users")
                            .whereEqualTo("userName", userNameChk)
                            .get(Source.SERVER)
                            .addOnCompleteListener { editViewModel.setCurrentFunction(null) }
                            .addOnSuccessListener {
                                when(it.documents.isNotEmpty()) {
                                    true -> {
                                        snackbarLog("Edit-sendUpdate", R.string.user_name_available,"error")
                                        editViewModel.setUiEnable(true)
                                    }

                                    false -> {
                                        when(editViewModel.getSelectImgByteArray() == null) {
                                            true -> profileUpdate() // Profil güncelleniyor
                                            false -> imageUpdateAndCreateUrl() // Resim update ediliyor daha sonra profil güncelleniyor
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                editViewModel.setUiEnable(true)
                                snackbarLog("Edit-sendUpdate", R.string.profile_update_fail,"error")
                                Log.e("Edit-sendUpdate", it.msg())
                            }
                    }
                }

            }

        }


    }

    private fun imageUpdateAndCreateUrl() {

        editViewModel.setCurrentFunction("imageUpdateAndCreateUrl")

        CoroutineScope(Dispatchers.IO).launch {

            val imgRef = storage.reference.child(sharedViewModel.getMyUserId()+".webp")
            val imgUpdate = imgRef.putBytes(editViewModel.getSelectImgByteArray()!!)

            imgUpdate
                .continueWithTask { task ->

                    when(task.isSuccessful) {
                        true -> imgRef.downloadUrl
                        else -> {
                            editViewModel.apply {
                                setUiEnable(true)
                                setCurrentFunction(null)
                            }
                            task.exception.let { throw it!!.suppressed[0] }
                        }
                    }

                }
                .addOnCompleteListener {

                    when(it.isSuccessful) {
                        true -> {
                            checkContext {
                                editViewModel.setNewPpLink(it.result.toString())
                                Log.i("Edit-ImgUpdateNewUrl", "Resim yüklendi")
                            }
                            profileUpdate()
                        }
                        else -> {
                            editViewModel.setUiEnable(true)
                            Log.e("Edit-ImgUpdateNewUrl","Resim yüklenemedi")
                            snackbarLog("Edit-ImgUpdateNewUrl", R.string.profile_update_fail,"error")
                            it.exception?.msg()?.let { msg -> Log.e("Edit-ImgUpdateNewUrl", msg) }
                        }
                    }
                    editViewModel.setCurrentFunction(null)
                }
        }

    }

    private fun profileUpdate() {
        editViewModel.setCurrentFunction("profileUpdate")

        CoroutineScope(Dispatchers.Default).launch {
            val updateMap = mutableMapOf<String,Any?>(
                "userName" to editViewModel.getNewUserName(),
                "userRealName" to editViewModel.getNewRealName(),
                "userBiography" to editViewModel.getNewBiography(),
                "userProfilePicture" to editViewModel.getNewPpLink(),
                "userTwitter" to editViewModel.getNewTwitter(),
                "userFacebook" to editViewModel.getNewFacebook(),
                "userInstagram" to editViewModel.getNewInstagram()
            )

            withContext(Dispatchers.IO) {
                firestore
                    .collection("Users")
                    .document(sharedViewModel.getMyUserId())
                    .update(updateMap)
                    .addOnCompleteListener {

                        editViewModel.apply {
                            setCurrentFunction(null)
                            setUiEnable(true)
                            setReAuth(false)
                        }

                        when(it.isSuccessful) {
                            true -> {
                                snackbarLog("Edit-ProfileUpdate", R.string.profile_update_success,"info")

                                binding.apply {
                                    txtEditRealName.setText("")
                                    txtEditUserName.setText("")
                                    txtEditBiography.setText("")
                                    txtEditInstagram.setText("")
                                    txtEditTwitter.setText("")
                                    txtEditFacebook.setText("")
                                }
                            }

                            else -> {
                                snackbarLog("Edit-ProfileUpdate", R.string.profile_update_fail,"error")
                                it.exception?.msg()?.let { msg -> Log.e("Edit-ProfileUpdate", msg) }
                            }
                        }

                    }
            }
        }
    }



    private fun updateMailCheckBlockCache() {

        editViewModel.setCurrentFunction("updateMailCheckBlockCache")

        /**
         * Mail block listte cache/server aranacak
         * Daha sonra auth ile güncellenecek
         * sonrada veritabanında profile yazdırılacak
         * daha sonra auth ile tekrar giriş yaptırılacak
         * updatemail kısmı (buradaki local değişken) güncellenecek
         */

        CoroutineScope(Dispatchers.IO).launch {

            firestore
                .collection("Block")
                .whereEqualTo("blockMail", editViewModel.getNewMail())
                .get(Source.CACHE)
                .addOnCompleteListener {

                    when(it.dataAvailable()) {
                        true -> {
                            snackbarLog("Edit-ChkBanList-Cache", R.string.register_banneduser_exists, "error")
                            editViewModel.setUiEnable(true)
                        }
                        else -> {
                            Log.e("Edit-ChkBanList-Cache", it.exceptionMsg())
                            updateMailCheckBlockServer()
                        }
                    }

                    editViewModel.setCurrentFunction(null)
                }


        }

    }

    private fun updateMailCheckBlockServer() {

        editViewModel.setCurrentFunction("updateMailCheckBlockServer")

        firestore
            .collection("Block")
            .whereEqualTo("blockMail", editViewModel.getNewMail())
            .get(Source.SERVER)
            .addOnCompleteListener {

                if(it.isSuccessful) {
                    if(it.result.isEmpty) {
                        checkContext {
                            Log.i("Edit-ChkBanList-Server", "Mail adresi yasaklı değil")
                        }
                        updateMail()
                    }
                    else {
                        snackbarLog("Edit-ChkBanList-Server", R.string.register_banneduser_exists, "error")
                        editViewModel.setUiEnable(true)
                    }
                }

                else {
                    snackbarLog("Edit-ChkBanList-Server", R.string.new_mail_fail,"error")
                    editViewModel.setUiEnable(true)
                }

                editViewModel.setCurrentFunction(null)

            }

    }

    private fun updateMail() {

        editViewModel.setCurrentFunction("updateMail")

        CoroutineScope(Dispatchers.IO).launch {

            val tryUpdateUser = auth.currentUser!!
            val saveUpdateUser = auth.currentUser!!

            tryUpdateUser.updateEmail(editViewModel.getNewMail())
                .addOnCompleteListener { editViewModel.setCurrentFunction(null) }
                .addOnSuccessListener {
                    checkContext { Log.i("EditProfile-updateMail", "Mail auth değiştirildi") }

                    updateMailWriteProfile()
                }
                .addOnFailureListener {
                    auth.updateCurrentUser(saveUpdateUser)
                    snackbarLog("EditProfile-updateMail", R.string.new_mail_fail, "error")
                    Log.e("EditProfile-updateMail", it.msg())
                    editViewModel.setUiEnable(true)
                }

        }

    }

    private fun updateMailWriteProfile() {

        editViewModel.setCurrentFunction("updateMailWriteProfile")

        val updateMap = mapOf<String,Any?>("userMail" to editViewModel.getNewMail(), "userEmailConfirm" to false)

        firestore
            .collection("Users")
            .document(sharedViewModel.getMyUserId())
            .update(updateMap)
            .addOnCompleteListener {

                when(it.isSuccessful) {
                    true -> snackbarLog( "Edit-updateMailWrite", R.string.new_mail_ok,"info")
                    false -> {
                        snackbarLog( "Edit-updateMailWrite", R.string.new_mail_fail,"info")
                        it.exception?.msg()?.let { msg -> Log.e( "Edit-updateMailWrite", msg) }
                    }
                }

                editViewModel.apply {
                    setCurrentFunction(null)
                    setUiEnable(true)
                    setReAuth(false)
                }

            }

    }




    private fun selectImage() {

        // Galeriden okuma izni kontrol ediliyor

        checkContext {

            if(ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    checkContext {
                        Snackbar
                            .make(requireContext(), binding.root, getString(R.string.permission_gallery), Snackbar.LENGTH_INDEFINITE)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setBackgroundTint(snackbarColor(requireContext()))
                            .setTextColor(Color.LTGRAY)
                            .questionSnackbar()
                            .setAction(getString(R.string.give_permission)) {
                                readPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE) // Galeriye erişim sorusu
                            }
                            .setActionTextColor(Color.RED)
                            .widthSettings()
                            .show()
                    }

                }

                else { readPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE) } // Galeriye erişim sorusu

            }

            else { permissionWriteGallery() } // İzin verildi, galeriye yazma kontrol edilecek

        }

    }

    private fun permissionWriteGallery() {

        checkContext {

            if(ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    checkContext {

                        Snackbar
                            .make(requireContext(), binding.root, getString(R.string.permission_gallery), Snackbar.LENGTH_INDEFINITE)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setBackgroundTint(snackbarColor(requireContext()))
                            .setTextColor(Color.LTGRAY)
                            .questionSnackbar()
                            .setAction(getString(R.string.give_permission)) {
                                writePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) // Veri yazma sorusu
                            }
                            .setActionTextColor(Color.RED)
                            .show()

                    }

                }
                else { writePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) } // Veri yazma sorusu
            }
            else { galleryImageLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply { type = "image/*" }) }

        }

    }

    private fun uriImageToWebp(uri : Uri) {

        checkContext {

            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val drawable = Drawable.createFromStream(inputStream, uri.toString())

            editViewModel.setNowImgStatus("newImage")
            editViewModel.setSelectImgByteArray(drawable?.let { drawableToByteArray(it) })

        }

    }


    private fun drawableToByteArray(drawable: Drawable) : ByteArray {

        val bitmap = drawable.toBitmap(500,500,null)
        val baos = ByteArrayOutputStream()
        if(Build.VERSION.SDK_INT >= 30) { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY,50, baos) }
        else { bitmap.compress(Bitmap.CompressFormat.WEBP,50, baos) }

        return baos.toByteArray()

    }



    private fun checkContext(context: Context.() -> Unit) { if (isAdded) { context(requireContext()) } }

}