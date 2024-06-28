package com.basesoftware.fikirzadem.presentation.ui.login

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.BottomDialogRegisterContractBinding
import com.basesoftware.fikirzadem.databinding.BottomDialogResetPasswordBinding
import com.basesoftware.fikirzadem.databinding.FragmentLoginBinding
import com.basesoftware.fikirzadem.domain.use_case.auth.SignUpUseCase
import com.basesoftware.fikirzadem.model.UserModel
import com.basesoftware.fikirzadem.presentation.viewmodel.LoginViewModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.gone
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.hideStatusBar
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.show
import com.basesoftware.fikirzadem.util.ExtensionUtil.toUserModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil.changeFragment
import com.basesoftware.fikirzadem.util.WorkUtil.systemLanguage
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.prefs.Preferences
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.random.Random

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding : FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth : FirebaseAuth
    private lateinit var firestore : FirebaseFirestore

    private lateinit var snackbar : Snackbar
    private lateinit var snackList: ArrayList<Snackbar>

    @Inject lateinit var signUpUseCase: SignUpUseCase

    private val loginViewModel : LoginViewModel by viewModels()

    lateinit var datastore : DataStore<Preferences>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        _binding = FragmentLoginBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        binding.viewmodel = loginViewModel

        lifecycleScope.launch {

            loginViewModel.fragmentChangeFlow.collect {
                if (it) changeFragment(Navigation.findNavController(binding.root), null, R.id.feedFragment)
            }

        }

        lifecycleScope.launch { loginViewModel.errorFlow.collect { it.let { Toast.makeText(requireActivity(), it, Toast.LENGTH_SHORT).show() } } }


        lifecycleScope.launch {

        }


//        listener()
//
//        variableSettings()

        /**
         * İlk önce currentuser null mı kontrolü yapacaksın
         * eğer currentuser null değil ve remember true ise feed git
         * eğer currentuser null değil ama remember false o zaman aktif işlem var mı bak
         * eğer currentuser null ise yine işlem var mı bak
         *
         * İlk önce viewmodelda gelen veriyi kontrol et regex ile
         * email uygun mu şifre 6 haneli mi şeklinde
         * daha sonra kayıt olmaya gönder
         * gelen exception'ı sınıflara ayırarak hatayı ver
         */


//        CoroutineScope(Dispatchers.IO).launch {
//
//            signUpUseCase.invoke("ZAZA@gmail.com", "dene").collect {
//                when(it){
//                    is Response.Success -> {
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(requireActivity(), "KAYIT TAMAM", Toast.LENGTH_SHORT).show()
//                        }
//
//                    }
//                    is Response.Loading -> {
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(requireActivity(), "KAYIT DENENİYOT", Toast.LENGTH_SHORT).show()
//                        }
//
//                    }
//                    is Response.Error -> {
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(requireActivity(), "KAYIT BAŞARISIZ", Toast.LENGTH_SHORT).show()
//                            Toast.makeText(requireActivity(), it.message, Toast.LENGTH_SHORT).show()
//                        }
//
//                    }
//                }
//            }
//
//        }

    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {

        super.onResume()

        try { Handler(Looper.getMainLooper()).postDelayed({requireActivity().window.hideStatusBar()}, 1500) }

        catch (e : Exception) {Log.e("MainActivity-onResume", e.msg())}

    }

    override fun onDestroy() {

        if(!snackList.isNullOrEmpty()) { snackList.clear() }

        _binding = null

        super.onDestroy()
    }


    private fun variableSettings() {

        snackList = arrayListOf()

        firestore = WorkUtil.firestore()
        auth = FirebaseAuth.getInstance()

        requireActivity().window.hideStatusBar()

        // Aktivite açıldığında tamamlanmamış işlem olup olmadığı kontrol ediliyor

        when(loginViewModel.getNowOperation().isNullOrEmpty()) {

            true -> {

                loginViewModel.setUiEnable(true)

                checkRemember()

            }

            false -> {

                when(loginViewModel.getNowOperation()) {
                    "userCheckBannedListServer" -> userCheckBannedListServer()
                    "userFirestoreControl" -> userFirestoreControl()
                    "userAuthRegister" -> userAuthRegister()
                    "userFirestoreRegister" -> userFirestoreRegister()
                    "userLogin" -> userLogin()
                    "loginCheckFirestore" -> loginCheckFirestore()
                    "profileErrorBlockControl" -> profileErrorBlockControl()
                    "deleteAuthentication" -> deleteAuthentication()
                    "loginGoFeedIntent" -> loginGoAppIntent()
                }

            }
        }



    }

    private fun checkRemember() {

//        if(auth.currentUser != null) {
//            when(requireActivity().getSharedPreferences(requireActivity().packageName.toString(), Context.MODE_PRIVATE).getBoolean("rememberMe",false)){
//                true -> {
////                    startActivity(Intent(this@LoginFragment, AppActivity::class.java))
//                }
//                false -> auth.signOut()
//            }
//        }

    }




    @SuppressLint("LogConditional")
    private fun snackbarLogInfo(tag: String, message: Any, type: String, uiEnableWait : Boolean) {

        CoroutineScope(Dispatchers.Main).launch {

            try {

                when(type){
                    "error" -> Log.e(tag, if(message is String) message else getString(message as Int)) // Bir hata logu
                    "warning" -> Log.w(tag, if(message is String) message else getString(message as Int)) // Bir tehlike logu
                    "info" -> Log.i(tag, if(message is String) message else getString(message as Int)) // Bir bilgi logu
                }

                snackbar = Snackbar
                    .make(
                        requireActivity(), binding.root,
                        if(message is String) message else getString(message as Int),
                        Snackbar.LENGTH_SHORT)
                    .settings()
                    .widthSettings()
                    .setGestureInsetBottomIgnored(true)
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {

                            if (!loginViewModel.getIsLoginToIntent()) {
                                /**
                                 * Kullanıcı giriş yapmadığı sürece snackbar bitişi beklenecek,
                                 * Eğer giriş yapmışsa snackbar temizlemeye gerek yok.
                                 */

                                if(uiEnableWait) { loginViewModel.setUiEnable(true) } // Ui componentler aktif edilmek için snackbar bitişini bekledi.

                                if(!snackList.isNullOrEmpty()) {
                                    snackList.removeAt(0)
                                    snackbar.removeCallback(this)
                                    if (snackList.size >= 1) { snackList[0].show() }
                                }

                            }
                            else { loginGoAppIntent() } // Uygulamaya giriş yapıldı

                        }
                    })

                snackList.add(snackbar)
                if(snackList.size == 1) { snackbar.show() }

            }
            catch (e: Exception) { Log.e("MainAct-snackbar-Catch",e.msg()) }

        }
    }



    private fun checkInput(afterStart : () -> Unit) {

        val tagMainCheckInput = "Main-checkInput()"

        loginViewModel.setUiEnable(false)

        val isInputError = MutableLiveData<Boolean>()

        isInputError.observe(viewLifecycleOwner) {
            when (it) {
                true -> loginViewModel.setUiEnable(true)
                false -> afterStart()
            }
            isInputError.removeObservers(this@LoginFragment)
        }

        val arrayUserInfoUi: ArrayList<TextInputEditText> = arrayListOf()
        arrayUserInfoUi.add(binding.txtUserMail)
        arrayUserInfoUi.add(binding.txtUserPassword)

        for (ui: TextInputEditText in arrayUserInfoUi) {

            if (ui.length() < 3) {

                snackbarLogInfo(tagMainCheckInput, R.string.info_empty, "error", true)
                isInputError.value = true
                break

            } // Textboxlarda 3 karakterden az varsa otomatik hata verecek

            else if (ui.id == R.id.txtUserMail) {

                val stMail = ui.text.toString().indexOf("@")

                when (stMail < 0) {
                    true -> {
                        // Eğer @ işareti yoksa otomatik hata verecek
                        snackbarLogInfo(tagMainCheckInput, R.string.email_error, "error", true)
                        isInputError.value = true
                        break
                    }
                    false -> loginViewModel.setTxtUserMailValue(ui.text.toString()) // Eğer e-mail sorunsuz ise txtEmail değişkenine ver
                }

            } // Mail kutusu kontrolü

            else if (ui.id == R.id.txtUserPassword) {

                when (ui.length() < 6) {
                    true -> {
                        // Firebase şifre için minimum 6 karakter istiyor, eğer 6 karakter mevcut değilse hata verecek
                        snackbarLogInfo(tagMainCheckInput,
                            R.string.password_error, "error", true)
                        isInputError.value = true
                        break
                    }
                    false -> {
                        // Eğer şifre sorunsuz ise txtpassword değişkenine ver
                        loginViewModel.setTxtUserPasswordValue(ui.text.toString())
                        isInputError.value = false
                    }
                }

            } // Şifre kutusu kontrolü
        }

    }



    private fun listener() {

        loginViewModel.getUpdateScreen().observe(viewLifecycleOwner) { binding.viewmodel = loginViewModel }

        binding.apply {

            txtUserMail.setOnEditorActionListener { _, action, _ ->

                if(action == EditorInfo.IME_ACTION_NEXT) txtUserPassword.requestFocus()

                return@setOnEditorActionListener true
            }

            txtUserPassword.setOnEditorActionListener { _, action, _ ->

                if(action == EditorInfo.IME_ACTION_GO) checkInput { userLogin() }

                return@setOnEditorActionListener true
            }

            btnLogin.setOnClickListener { checkInput { userLogin() } }

            btnRegister.setOnClickListener { checkInput { userAuthRegister() } }

            txtResetPassword.setOnClickListener { resetPassDialog() }

        }

    }


    private fun resetPassDialog() {

        val tag = "Main-ResetPasswordClick"

        BottomSheetDialog(requireActivity(), R.style.BottomSheetDialogTheme).apply {
            val bindingPass = BottomDialogResetPasswordBinding.inflate(layoutInflater)

            setContentView(bindingPass.root)

            show()

            bindingPass.btnResetPassOk.setOnClickListener {

                this.dismiss()

                when(bindingPass.txtUserMailReset.length() < 3) {

                    true -> snackbarLogInfo(tag, R.string.info_empty, "error", true)

                    else -> {

                        CoroutineScope(Dispatchers.IO).launch {

                            auth.setLanguageCode(systemLanguage())

                            auth
                                .sendPasswordResetEmail(bindingPass.txtUserMailReset.text.toString())
                                .addOnSuccessListener {
                                    snackbarLogInfo(tag, R.string.check_mailbox, "info", true)
                                }
                                .addOnFailureListener {
                                    snackbarLogInfo(tag,
                                        R.string.check_mailbox_error, "error", false)
                                    snackbarLogInfo(tag, it.msg(), "error", true)
                                }

                        }

                    }
                }

            }

        }

    }


    private fun userAuthRegister() {

        loginViewModel.setNowOperation("userAuthRegister")

        CoroutineScope(Dispatchers.IO).launch {

            val tag = "Main-userAuthRegister()"
            // Firebase Authentication kısmına kayıt deneniyor
            auth
                .createUserWithEmailAndPassword(loginViewModel.getLoginUiState().value.userMailText, loginViewModel.getTxtUserPasswordValue())
                .addOnCompleteListener { authRegTask ->

                    when(authRegTask.isSuccessful) {

                        true -> {
                            Log.i(tag, "Authentication kaydı başarılı")
                            userCheckBannedListServer() // Block kontrolü yap
                        }

                        else -> {
                            Log.e(tag, "Authentication kaydı başarısız")
                            snackbarLogInfo(tag, R.string.register_fail,"error",false)
                            authRegTask.exception?.msg()?.let { snackbarLogInfo(tag, it,"error",true) }
                        }

                    }
                    loginViewModel.setNowOperation(null)

                }

        }

    }

    private fun userCheckBannedListServer() {

        loginViewModel.setNowOperation("userCheckBannedListServer")

        CoroutineScope(Dispatchers.IO).launch {

            val tag = "Main-ChkBanList-Server"

            firestore
                .collection("Block")
                .whereEqualTo("blockMail", loginViewModel.getLoginUiState().value.userMailText)
                .get(Source.SERVER)
                .addOnCompleteListener { checkListTask ->

                    when(checkListTask.isSuccessful) {

                        true -> {

                            when(checkListTask.result.isEmpty) {

                                true -> Log.i(tag, "Mail adresi yasaklı değil").also { userFirestoreControl() }

                                false -> {
                                    snackbarLogInfo(tag,
                                        R.string.register_banneduser_exists, "error",true)
                                    deleteAuthentication()
                                }

                            }

                        }

                        else -> {
                            snackbarLogInfo(tag, R.string.register_fail,"error",false)
                            checkListTask.exception?.msg()?.let { snackbarLogInfo(tag, it,"error",true) }
                            deleteAuthentication()
                        }

                    }

                    loginViewModel.setNowOperation(null)

                }

        }

    }

    private fun userFirestoreControl() {

        loginViewModel.setNowOperation("userFirestoreControl")

        // Firestore Users koleksiyonu kontrol ediliyor.

        CoroutineScope(Dispatchers.IO).launch {

            val tag = "MainUserFirestoreContrl"

            firestore
                .collection("Users")
                .whereEqualTo("userMail", loginViewModel.getLoginUiState().value!!.userMailText)
                .get(Source.SERVER)
                .addOnCompleteListener {

                    when(it.isSuccessful) {

                        true -> when(it.result.isEmpty) {
                            true -> userFirestoreRegister() // Profil yok, oluşturmayı dene.
                            else -> snackbarLogInfo(
                                tag,
                                when(it.result.documents[0].toUserModel().userIsActive) {
                                    true -> R.string.user_available
                                    else -> R.string.user_deleted
                                },
                                "info",
                                true).also { deleteAuthentication() }
                        }

                        else -> {
                            // Users tablosunda query ile email aranıyor (başarısız)
                            snackbarLogInfo(tag, R.string.register_fail,"error",true)
                            deleteAuthentication()
                            it.exception?.msg()?.let { msg -> snackbarLogInfo(tag, msg,"error",true) }
                        }

                    }

                    loginViewModel.setNowOperation(null)
                }

        }

    }

    private fun userFirestoreRegister() {

        loginViewModel.setNowOperation("userFirestoreRegister")

        val tag = "Main-FirestoreRegister"

        if(auth.currentUser != null) {

            // FireStore Users koleksiyonuna profil oluşturuluyor
            CoroutineScope(Dispatchers.IO).launch {

                loginViewModel.setUserUuid(auth.currentUser!!.uid) // Kullanıcıya uuid verildi

                val userName = "user" + Random.nextInt(10000,99999).toString() // Random username/realname verildi

                loginViewModel.setUserModel(
                    UserModel(
                        loginViewModel.getUserUuid(),
                        loginViewModel.getLoginUiState().value.userMailText,
                        userName,
                        userName,
                        "-",
                        "default",
                        "-",
                        "-",
                        "-",
                        getString(R.string.first_admin_message_tr),
                        getString(R.string.first_admin_message_en),
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        userAddPost = true,
                        userAddComment = true,
                        userAddReport = true,
                        userAddContact = true,
                        userEditProfile = true,
                        userIsActive = true,
                        userEmailConfirm = false,
                        userAdminMessageDate = null,
                        userRegisterDate = null
                    ) // Model oluşturuldu
                )

                firestore
                    .collection("Users")
                    .document(loginViewModel.getUserUuid())
                    .set(loginViewModel.getUserModel()!!)
                    .addOnCompleteListener { registerTask ->

                        when(registerTask.isSuccessful) {
                            true -> {
                                snackbarLogInfo(tag, R.string.profile_created,"info",false)
                                snackbarLogInfo(tag, R.string.login_account,"info",true)
                            }
                            else -> {
                                snackbarLogInfo(tag, R.string.register_fail,"error",true)
                                registerTask.exception?.msg()?.let { snackbarLogInfo(tag, it,"error",true) }
                                deleteAuthentication() // Profil oluşturulamadı auth silindi
                            }
                        }
                        loginViewModel.setNowOperation(null)

                    }

            }

        }
        else {
            snackbarLogInfo(tag, R.string.register_fail,"error",true)
            loginViewModel.setNowOperation(null)
        }

    }




    @SuppressLint("LogConditional")
    private fun userLogin() {

        loginViewModel.setNowOperation("userLogin")

        // Firebase Authentication logini deneniyor
        CoroutineScope(Dispatchers.IO).launch {

            val tag = "Main-UserLogin()"
            auth.signInWithEmailAndPassword(loginViewModel.getLoginUiState().value!!.userMailText, loginViewModel.getTxtUserPasswordValue())
                .addOnCompleteListener { loginTask ->

                    when(loginTask.isSuccessful) {
                        true -> {
                            Log.i(tag, getString(R.string.login_success))
                            loginCheckFirestore() // Kullanıcının firestore'daki bilgileri kontrol ediliyor
                        }
                        else -> {
                            snackbarLogInfo(tag, R.string.login_fail,"error",false)
                            loginTask.exception?.msg()?.let { snackbarLogInfo(tag, it,"error",true) }
                        }
                    }
                    loginViewModel.setNowOperation(null)

                }

        }

    }

    private fun loginCheckFirestore() {

        loginViewModel.setNowOperation("loginCheckFirestore")

        CoroutineScope(Dispatchers.IO).launch {

            val tag = "MainLoginCheckFirestore"

            firestore
                .collection("Users")
                .whereEqualTo("userMail", loginViewModel.getLoginUiState().value.userMailText)
                .limit(1)
                .get(Source.SERVER)
                .addOnSuccessListener {
                    // Users tablosunda query ile email aranıyor (başarılı)
                    if (it.isEmpty) {
                        // E-mail mevcut değil (auth kaydı var ama users kaydı yok)
                        snackbarLogInfo(tag, R.string.profile_error,"warning",false)

                        /**
                         * Block listesi kontrol edilecek.
                         * Eğer block var ise kullanıcı bloklandı yazacak
                         * Eğer block yok ise yeni profil oluşturulacak
                         */

                        profileErrorBlockControl()

                    }
                    else {

                        when(it.documents[0].toUserModel().userIsActive) {

                            true -> {
                                // Kullanıcının Users tablosundaki UUID kaydı bulundu
                                loginViewModel.setUserFirestoreId(it.documents[0].toUserModel().userId)

                                loginViewModel.setIsLoginToIntent(true) // Uygulama giriş emri verildi snackbar bitişi feed gidilecek

                                snackbarLogInfo(tag, R.string.login_success,"info",false)
                            }

                            else -> {
                                // Kullanıcının aktiflik durumu değiştirilmiş.
                                snackbarLogInfo(tag, R.string.user_deleted,"warning",true)

                                deleteAuthentication() // Profil silinmiş/silinmek isteniyor. -> Authentication siliniyor
                            }

                        }

                    }

                    loginViewModel.setNowOperation(null)

                }
                .addOnFailureListener {
                    // Users tablosunda query ile email aranıyor (başarısız)
                    snackbarLogInfo(tag, R.string.login_fail,"error",false)
                    snackbarLogInfo(tag, it.msg(),"error",true)
                    loginViewModel.setNowOperation(null)
                }

        }

    }

    private fun profileErrorBlockControl() {

        loginViewModel.setNowOperation("profileErrorBlockControl")

        CoroutineScope(Dispatchers.IO).launch {

            val tag = "MainErrorCheckBlockList"

            firestore
                .collection("Block")
                .whereEqualTo("blockMail", loginViewModel.getLoginUiState().value!!.userMailText)
                .limit(1)
                .get(Source.SERVER)
                .addOnCompleteListener { checkBlock ->

                    when(checkBlock.isSuccessful) {

                        true -> {

                            when(checkBlock.result.isEmpty) {

                                true -> {
                                    snackbarLogInfo(tag, R.string.profile_creating, "info",false)
                                    userFirestoreRegister()
                                }

                                false -> {
                                    snackbarLogInfo(tag,
                                        R.string.register_banneduser_exists, "error",true)
                                    deleteAuthentication()
                                }

                            }

                        }

                        else -> {
                            snackbarLogInfo(tag, R.string.login_fail,"error",false)
                            checkBlock.exception?.msg()?.let { snackbarLogInfo(tag, it,"error",true) }
                        }

                    }

                    loginViewModel.setNowOperation(null)

                }

        }

    }



    private fun deleteAuthentication() {

        loginViewModel.setNowOperation("deleteAuthentication")

        val tag = "Main-deleteAuth"

        if(auth.currentUser != null) {

            auth.currentUser!!
                .delete()
                .addOnCompleteListener { deleteAuthTask ->

                    when (deleteAuthTask.isSuccessful) {
                        true -> {
                            Log.i(tag, "Kullanıcı authentication silindi")
                            snackbarLogInfo(tag, R.string.create_new_user, "info", true)
                        }
                        else -> {
                            Log.e(tag, "Kullanıcı authentication silinemedi")
                            deleteAuthTask.exception?.msg()?.let { Log.e(tag, it) }
                            loginViewModel.setUiEnable(true)
                        }
                    }

                    loginViewModel.setNowOperation(null)

                }

        }
        else {
            Log.e(tag, "Kullanıcı authentication silinemedi")
            loginViewModel.setUiEnable(true)
            loginViewModel.setNowOperation(null)
        }


    }




    private fun loginGoAppIntent() {

        loginViewModel.setNowOperation("loginGoFeedIntent")

        CoroutineScope(Dispatchers.Main).launch {
            requireActivity().getSharedPreferences(requireActivity().packageName.toString(), Context.MODE_PRIVATE).edit {
                putBoolean("rememberMe", binding.chkRemember.isChecked)
                putString("userId", loginViewModel.getUserFirestoreId())
                commit()
            }

//            startActivity(Intent(this@LoginFragment, AppActivity::class.java))
//            loginViewModel.setNowOperation("null")
//            finish()

        }

    }


}