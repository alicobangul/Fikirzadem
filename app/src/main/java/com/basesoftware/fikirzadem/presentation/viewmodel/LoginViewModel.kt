package com.basesoftware.fikirzadem.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.domain.repository.AuthRepository
import com.basesoftware.fikirzadem.domain.repository.UserSettingsRepository
import com.basesoftware.fikirzadem.domain.use_case.auth.AuthDeleteUseCase
import com.basesoftware.fikirzadem.domain.use_case.auth.SignInUseCase
import com.basesoftware.fikirzadem.domain.use_case.auth.SignUpUseCase
import com.basesoftware.fikirzadem.domain.use_case.block.SearchBlockedUserUseCase
import com.basesoftware.fikirzadem.domain.use_case.user.ActiveUserControlUseCase
import com.basesoftware.fikirzadem.domain.use_case.user.CreateUserUseCase
import com.basesoftware.fikirzadem.domain.util.AuthError
import com.basesoftware.fikirzadem.domain.util.AuthInputError
import com.basesoftware.fikirzadem.domain.util.BlockError
import com.basesoftware.fikirzadem.domain.validator.AuthInputValidator
import com.basesoftware.fikirzadem.domain.util.Result
import com.basesoftware.fikirzadem.domain.util.UserError
import com.basesoftware.fikirzadem.model.UserModel
import com.basesoftware.fikirzadem.presentation.ui.login.LoginState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val state : SavedStateHandle,
    val authInputValidator: AuthInputValidator,
    val signInUseCase: SignInUseCase,
    val signUpUseCase: SignUpUseCase,
    val authDeleteUseCase: AuthDeleteUseCase,
    val activeUserControlUseCase: ActiveUserControlUseCase,
    val searchBlockedUserUseCase: SearchBlockedUserUseCase,
    val createUserUseCase: CreateUserUseCase,
    val authRepository: AuthRepository,
    val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private var _fragmentChangeFlow = MutableStateFlow(false)
    val fragmentChangeFlow = _fragmentChangeFlow

    private var _errorFlow : MutableSharedFlow<Int> = MutableSharedFlow()
    val errorFlow = _errorFlow

    private var _emptyText = ""

    // <---------- SİLİNECEK FONKSİYONLAR ---------->
    fun getUpdateScreen() : MutableLiveData<Boolean> = state.getLiveData("updateScreen", false)

    fun setUiEnable(value : Boolean) { state["uiEnable"] = value }

    init {

        viewModelScope.launch(Dispatchers.IO) {

            // Aktif bir auth var ve beni hatırla seçeneği seçildi ise feed fragment'a git
            if(authRepository.isLoggedIn() && userSettingsRepository.getRememberMe()) _fragmentChangeFlow.emit(true)

            else {
                // Aktif bir işlem var mı diye kontrol et
            }


        }

    }

    private val _loginState: MutableStateFlow<LoginState> = MutableStateFlow(state["loginState"] ?: LoginState.Empty)
    val loginState : StateFlow<LoginState> get() = _loginState

    fun getLoginUiState() : StateFlow<LoginState.LoginUiState> = state.getStateFlow("loginUiState", LoginState.LoginUiState())

    fun setUserMailText(mailText : String) = state.set("loginUiState", getLoginUiState().value.apply { userMailText = mailText })

    fun setUserPasswordText(passwordText : String) = state.set("loginUiState", getLoginUiState().value.apply { userPasswordText = passwordText })

    fun rememberChecked(isChecked : Boolean) = state.set("loginUiState", getLoginUiState().value.apply { rememberMe = isChecked })

    fun userLogin() {

//        state["loginState"] = getLoginUiState().value.apply { operation = LoginOperation.LOGIN }

        viewModelScope.launch(Dispatchers.IO) {

            checkUserInput(getLoginUiState().value.userMailText, getLoginUiState().value.userPasswordText) { email, password ->
                signInUserAuth(email, password)
            }

        }

    }

    fun userRegister() {

        //        state["loginState"] = getLoginUiState().value.apply { operation = LoginOperation.LOGIN }

        viewModelScope.launch(Dispatchers.IO) {

            checkUserInput(getLoginUiState().value.userMailText, getLoginUiState().value.userPasswordText) { mail, password ->
                signUpUserAuth(mail,password)
            }

        }

    }

    fun resetPasswordClick() {
        Log.i("API-TEST", "reset tıklandı")
    }


    suspend inline fun checkUserInput(email: String, password: String, successProcess: (String, String) -> Unit) {

        when(val inputResult = authInputValidator.validateEmailAndPassword(getLoginUiState().value.userMailText, getLoginUiState().value.userPasswordText)) {

            // Validate kısmında sorun yok giriş yapmayı dene
            is Result.Success -> successProcess(email, password)

            is Result.Error<*, *> -> {

                when((inputResult as Result.Error<Unit, AuthInputError>).error) {
                    AuthInputError.EMAIL_TYPE -> errorFlow.emit(R.string.email_error)
                    AuthInputError.SHORT_PASSWORD -> errorFlow.emit(R.string.password_error)
                }

            }

        }

    }



    suspend fun signInUserAuth(email : String, password : String) {

        when(val loginResult = signInUseCase.invoke(email, password)) {

            // Auth girişi başarılı firestore ile belgeyi ara
            is Result.Success -> checkUserForActive(email)

            is Result.Error -> {

                when((loginResult as Result.Error<*,*>).error) {
                    AuthError.AUTH_NOTFOUND -> _errorFlow.emit(R.string.user_notfound)
                    AuthError.WRONG_PASSWORD -> _errorFlow.emit(R.string.wrong_password)
                    else -> _errorFlow.emit(R.string.unknown_error)
                }

            }

        }

    }

    suspend fun checkUserForActive(mail : String) {

        when(val activeResult = activeUserControlUseCase.findUser(mail, ActiveUserControlUseCase.SearchField.EMAIL)) {

            is Result.Success<Boolean, *> -> {

                when((activeResult as Result.Success<*, *>).data as Boolean) {

                    // Kullanıcı aktif o halde feed sayfasına git
                    true -> _fragmentChangeFlow.emit(true)
                    else -> {
                        _errorFlow.emit(R.string.user_deleted)
                        deleteAuth()
                    }

                }

            }

            is Result.Error -> {
                when((activeResult as Result.Error<*,*>).error) {
                    UserError.USER_NOTFOUND -> {
                        _errorFlow.emit(R.string.profile_error)
                        searchUserInBlockList(mail)
                    }
                    else -> _errorFlow.emit(R.string.unknown_error)
                }
            }

        }

    }

    suspend fun searchUserInBlockList(mail : String) {

        when(val blockResult = searchBlockedUserUseCase.findBlockedUser(mail)) {

            // Kullanıcı blok listesinde var, yasaklanmış
            is Result.Success -> {
                when((blockResult as Result.Success<*,*>).data as Boolean) {
                    // Kullanıcı bloklanmış
                    true -> {
                        _errorFlow.emit(R.string.register_banneduser_exists)
                        deleteAuth()
                    }

                    else -> createUserDocument(mail)
                }
            }

            is Result.Error -> _errorFlow.emit(R.string.unknown_error)

        }

    }

    suspend fun deleteAuth() {

        when(val authDeleteResult = authDeleteUseCase.invoke()) {

            is Result.Success -> {
                // Kullanıcı silme işlemi başarılı
                Log.i("API-TAG", "Hesap silme başarılı")
            }

            is Result.Error -> {
                when((authDeleteResult as Result.Error<*,*>).error) {

                    AuthError.CURRENTUSER_NOTFOUND -> {
                        // Şuan bir kullanıcı yok
                        Log.i("API-TAG", "Aktif bir kullanıcı yok hesap silinemedi")
                    }

                    AuthError.AUTH_NOTFOUND -> {
                        // Silinecek ama kullanıcı bulunamadı
                        Log.i("API-TAG", "Auth bulunamadı")
                    }
                    else -> {
                        // Bilinmeyen bir hata var
                        Log.i("API-TAG", "Bilinemeyen bir hata var.")
                    }
                }
            }

        }

    }


    suspend fun signUpUserAuth(mail : String, password : String) {

        when(val registerResult = signUpUseCase.invoke(mail, password)) {

            // Auth kaydı başarılı
            is Result.Success -> {
                // Blok listesine bakacak sonra firestore belge oluşturacak
                searchUserInBlockList(mail)
            }

            is Result.Error -> {

                when((registerResult as Result.Error<*,*>).error) {
                    AuthError.AUTH_COLLISION -> _errorFlow.emit(R.string.user_available)
                    else -> _errorFlow.emit(R.string.unknown_error)
                }

            }

        }

    }

    suspend fun createUserDocument(mail : String) {

        when(val createUserResult = createUserUseCase.invoke(mail)) {

            // Kayıt başarılı
            is Result.Success -> { _fragmentChangeFlow.emit(true) }

            is Result.Error -> {

                _errorFlow.emit(R.string.unknown_error)

            }

        }

    }


    // <---------- VARIABLES ---------->

    fun getTxtUserMailValue() : String = state["_txtUserMailValue"] ?: _emptyText
    fun setTxtUserMailValue(value : String) = state.set("_txtUserMailValue", value)

    fun getTxtUserPasswordValue() : String = state["_txtUserPasswordValue"] ?: _emptyText
    fun setTxtUserPasswordValue(value : String) = state.set("_txtUserPasswordValue", value)

    fun getUserUuid() : String = state["_userUuid"] ?: _emptyText
    fun setUserUuid(value : String) = state.set("_userUuid", value)

    fun getIsLoginToIntent() : Boolean = state["_isLoginToIntent"] ?: false
    fun setIsLoginToIntent(value : Boolean) = state.set("_isLoginToIntent", value)

    fun getUserModel() : UserModel? = state.get<UserModel>("_userModel")
    fun setUserModel(model : UserModel) = state.set("_userModel", model)

    fun getUserFirestoreId() : String = state["_userFirestoreId"] ?: "null"
    fun setUserFirestoreId(value : String) = state.set("_userFirestoreId", value)


    // <------------------------------ OPERATION ------------------------------>

    fun getNowOperation() : String? = state["_nowOperation"]
    fun setNowOperation(value : String?) = state.set("_nowOperation", value)

}