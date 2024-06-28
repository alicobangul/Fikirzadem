package com.basesoftware.fikirzadem.presentation.ui.newpost

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.FragmentNewPostBinding
import com.basesoftware.fikirzadem.model.PostModel
import com.basesoftware.fikirzadem.presentation.viewmodel.NewPostViewModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.gone
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.show
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.WorkUtil.changeFragment
import com.basesoftware.fikirzadem.util.WorkUtil.goMyProfile
import com.basesoftware.fikirzadem.util.WorkUtil.systemLanguage
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import java.util.*


class NewPostFragment : Fragment() {

    private var _binding : FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore : FirebaseFirestore

    private lateinit var postRulesDialog : BottomSheetDialog

    private lateinit var popupCategory : PopupMenu

    private lateinit var popupLike : PopupMenu

    private val sharedViewModel : SharedViewModel by activityViewModels { SharedViewModel.provideFactory(requireActivity().application, requireActivity()) }
    private val newPostViewModel : NewPostViewModel by viewModels()



    override fun onCreateView(inf: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {

        _binding = FragmentNewPostBinding.inflate(layoutInflater, group, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize() // Veriler initialize edildi

        popupSettings() // Kategori ve beğeni modu popup ayarları yapıldı

        addPostTextLengthProgress() // Progressbar listener eklendi

        listener() // Geri ve gönder buton listener

    }


    override fun onResume() {

        checkContext {

            if(newPostViewModel.getGoFeed()) changeFragment(findNavController(), null, R.id.feedFragment)

        }

        super.onResume()
    }

    override fun onDestroyView() {

        if(postRulesDialog.isShowing) postRulesDialog.dismiss()

        _binding = null

        super.onDestroyView()
    }

    private fun checkContext(context: Context.() -> Unit) { if (isAdded) { context(requireContext()) } }



    @SuppressLint("DiscouragedPrivateApi")
    private fun initialize() {

        checkContext {

            firestore = WorkUtil.firestore()

            postRulesDialog = BottomSheetDialog(requireActivity(), R.style.BottomSheetDialogTheme)

            binding.txtAddNewPost.apply {
                filters = arrayOf(InputFilter.LengthFilter(newPostViewModel.getMaxLength())) // Max. karakter sınırı işlendi
            }

            popupLike = PopupMenu(requireContext(),binding.txtLikeModePopup)
            popupLike.inflate(R.menu.newpost_likemode_menu)

            popupCategory = PopupMenu(requireContext(),binding.txtCategoryPopup, Gravity.END)
            popupCategory.inflate(
                when(systemLanguage().matches(Regex("tr"))) {
                    true -> R.menu.newpost_category_menu_tr // Telefon dili türkçe ise türkçe kategori sıralaması
                    else -> R.menu.newpost_category_menu_en // Telefon dili türkçe değil ise ingilizce kategori sıralaması
                }
            )
            

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
            catch (e : Exception){ Log.e("NewPost-Popup","NewPost likemode ikon gösterimi başarısız") }

        }

    }

    private fun popupSettings() {

        val tagPopup = "NewPostFragPopupSetting"
        try {

            popupLike.menu.findItem(newPostViewModel.getLikeMode()).let { item ->
                item.isChecked = true

                binding.txtLikeModePopup.apply {
                    this.text = item.title.toString()
                    this.setCompoundDrawablesWithIntrinsicBounds(item.icon,null,null,null)
                }

            }

            popupCategory.menu.findItem(newPostViewModel.getCategory()).let { item ->
                item.isChecked = true

                binding.txtCategoryPopup.apply {
                    this.text = item.title.toString()
                    this.setCompoundDrawablesWithIntrinsicBounds(item.icon,null,null,null)
                }

            }

            Log.i(tagPopup, "NewPostFrag - PopupSettings başarılı")
        }
        catch (e: Exception) {
            Log.e(tagPopup, "NewPostFrag - PopupSettings başarısız")
            Log.e(tagPopup, e.msg())
        }

    }

    private fun closeKeyboard() {

        val tag = "NewPost-CloseKeyboard"

        try
        {
            checkContext {
                val hideKeyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                hideKeyboard?.hideSoftInputFromWindow(binding.root.windowToken, 0)
            }
        }
        catch (e : Exception) { Log.e(tag, e.msg()) }

    }



    @SuppressLint("LogConditional")
    private fun logSnackbar(tag : String, message: Int, type : String, ui : Boolean, postComplete : Boolean) {

        CoroutineScope(Dispatchers.Main).launch {

            try {

                checkContext {

                    when(type){
                        "error" -> Log.e(tag, getString(message)) // Bir hata logu
                        "warning" -> Log.w(tag, getString(message)) // Bir tehlike logu
                        "info" -> Log.i(tag, getString(message)) // Bir bilgi logu
                    }

                    if(postComplete) newPostViewModel.setGoFeed(true)

                    val snack = Snackbar
                        .make(requireActivity(),binding.root, getString(message), Snackbar.LENGTH_SHORT)
                        .settings()
                        .widthSettings()
                        .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                checkContext {

                                    if(postComplete) {
                                        changeFragment(findNavController(),null,R.id.feedFragment)
                                    }
                                    // Ui componentler aktif edilmek için snackbar bitişini bekledi.
                                    else if(!postComplete && ui) { newPostViewModel.setUiEnable(true) }

                                }
                            }
                        })

                    snack.show()

                }

            }
            catch (e: Exception) { Log.e("NewPost-Snackbar-Catch",e.msg()) }


        }

    }



    private fun addPostTextLengthProgress(){

        binding.txtNewPostLenght.setTextColor(Color.LTGRAY)

        binding.txtAddNewPost.addTextChangedListener {
            binding.txtNewPostLenght.text = (newPostViewModel.getMaxLength() - binding.txtAddNewPost.text.length).toString()
            binding.msgLengthProgress.progress = newPostViewModel.getMaxLength() - binding.txtAddNewPost.text.length

            /**
             * Eğer isAddChar false ise (reklam ile ekstra karakter alma hakkı var ise)
             * Eğer şuanki text boyutu 350 oldu ise
             * Reklam almak için +650 Text görünümünü aç
             */
            if(!newPostViewModel.getIsAddChar()){
                when(binding.txtAddNewPost.text.length == 350) {
                    true -> binding.txtAddCharLimit.show()
                    false -> binding.txtAddCharLimit.gone()
                }
            }

        }

    }


    private fun listener() {

        popupLike.setOnMenuItemClickListener {

            newPostViewModel.setLikeMode(it.itemId)
            binding.txtLikeModePopup.apply {
                text = it.title.toString()
                setCompoundDrawablesWithIntrinsicBounds(it.icon,null,null,null)
            }

            return@setOnMenuItemClickListener true
        }

        popupCategory.setOnMenuItemClickListener {

            newPostViewModel.setCategory(it.itemId)
            binding.txtCategoryPopup.apply {
                text = it.title.toString()
                setCompoundDrawablesWithIntrinsicBounds(it.icon,null,null,null)
            }

            return@setOnMenuItemClickListener true
        }

        newPostViewModel.getUiEnable().observe(viewLifecycleOwner, { binding.newpost = newPostViewModel })

        sharedViewModel.getUserLive().observe(viewLifecycleOwner, { binding.shared = sharedViewModel })

        binding.imgNewPostPp.setOnClickListener { goMyProfile(findNavController(), sharedViewModel.getMyUserId()) }

        binding.txtCategoryPopup.setOnClickListener { popupCategory.show() }

        binding.txtLikeModePopup.setOnClickListener { popupLike.show() }

        binding.imgNewPostGoBack.setOnClickListener {

            changeFragment(findNavController(),null,R.id.feedFragment)

        }
        
        binding.txtAddCharLimit.setOnClickListener {

            checkContext {

                binding.txtAddCharLimit.gone() // Gizlendi

                newPostViewModel.apply {
                    setMaxLength(1000) // Viewmodel Max 1000 ayarlandı
                    setIsAddChar(true) // Viewmodel Tekrar bonus alınamaz
                }

                binding.msgLengthProgress.apply {
                    max = newPostViewModel.getMaxLength() // Progressbar max 1000 yapıldı
                    progress = newPostViewModel.getMaxLength() - binding.txtAddNewPost.length() // Progress güncellendi - 650
                }

                // Content maxLength 1000 yapıldı
                binding.txtAddNewPost.filters = arrayOf(InputFilter.LengthFilter(newPostViewModel.getMaxLength()))

                // Progress güncel değer yazıldı - 650
                binding.txtNewPostLenght.text = binding.msgLengthProgress.progress.toString()


            }


        }

        binding.txtAddNewPost.setOnFocusChangeListener { _, hasFocus -> if(!hasFocus) { closeKeyboard() } }

        binding.btnSendNewPost.setOnClickListener {

            val tag = "NewPost-SendClick"

            checkContext { requireView().clearFocus() }

            closeKeyboard()

            newPostViewModel.setUiEnable(false)

            when(binding.txtAddNewPost.length() < 30) {

                true -> logSnackbar(tag, R.string.minchar_limit, "error", ui = true, postComplete = false)

                else -> {

                    when(binding.txtAddNewPost.text.toString().trim().isEmpty()) {

                        true -> logSnackbar(tag, R.string.spam_error, "error", ui = true, postComplete = false)

                        else -> {

                            val text = binding.txtAddNewPost.text.toString().replace("\\s+".toRegex(), " ").trim()
                            /*
                            Metinde temizlenen boşluklar: (_ kısa çizgi boşluğu temsil ediyor)
                            - Baştaki/sondaki ->  _metin / metin_   -> metin
                            - Kelimeler/harfler arasındaki spam (birden fazla) boşluklar ->  metin____metin  -> metin_metin
                            */

                            binding.txtAddNewPost.apply {
                                setText(text)
                                setSelection(text.length)
                            }

                            // Text düzenlendikten sonra tekrar kontrol ediliyor
                            when(binding.txtAddNewPost.length() > 30) {

                                true -> {

                                    when(sharedViewModel.getUser()?.userAddPost != null && sharedViewModel.getUser()?.userAddPost!!) {

                                        true -> openPostRules() // Kullanıcının post atması yasaklanmamış kuralları aç

                                        false -> logSnackbar(tag, R.string.addpost_block,"info", ui = true,true)

                                    }

                                }

                                false -> logSnackbar(tag, R.string.minchar_limit,"error",ui = true,false)

                            }

                        }

                    }

                }

            }

        }


    }




    private fun openPostRules() {

        checkContext {

            postRulesDialog.apply {

                setContentView(R.layout.bottom_dialog_post_rules)

                findViewById<AppCompatButton>(R.id.btnRulesOk)?.setOnClickListener { if(isShowing) dismiss() }

                setOnDismissListener { postSend() }

                show()

            }

        }

    }


    private fun postSend() {

        CoroutineScope(Dispatchers.IO).launch {

            checkContext {

                val categoryId : Int = resources.getStringArray(R.array.arrayCategoryText).indexOf(binding.txtCategoryPopup.text.toString())
                val postContent : String = binding.txtAddNewPost.text.toString().replace("\\s+".toRegex(), " ").trim()
                val postLikePublic : Boolean = binding.txtLikeModePopup.text.toString().matches(Regex(getString(R.string.likemode_on)))
                val postUUID : String = UUID.randomUUID().toString()
                val userId : String = sharedViewModel.getMyUserId()

                val postModel = PostModel(
                    categoryId,
                    postContent,
                    postLikePublic,
                    postUUID,
                    userId,
                    null
                )
                // Sunucunun zaman damgasını otomatik eklemesi için  @ServerTimestamp annotation bölümünü null veriyoruz


                var addPostNetworkError = true
                firestore
                    .collection("Posts")
                    .document(postUUID)
                    .set(postModel)
                    .addOnSuccessListener {
                        addPostNetworkError = false
                        logSnackbar("NewPost-PostSend", R.string.addpost_complete, "info", ui = false, postComplete = true)
                    }
                    .addOnFailureListener {
                        addPostNetworkError = false
                        logSnackbar("NewPost-PostSend", R.string.addpost_error, "error", ui = true, postComplete = false)
                        Log.e("NewPost-PostSend",it.msg())
                    }

                launch {
                    delay(4000)
                    if (addPostNetworkError) {
                        logSnackbar(
                            "NewPost-PostSend",
                            R.string.check_internet_connection,
                            "warning",
                            ui = true,
                            postComplete = false
                        )
                    }
                }

            }

        }

    }




}