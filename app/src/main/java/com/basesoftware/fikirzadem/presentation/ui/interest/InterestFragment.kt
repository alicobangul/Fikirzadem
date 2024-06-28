package com.basesoftware.fikirzadem.presentation.ui.interest

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.presentation.adapter.FeedRecyclerAdapter
import com.basesoftware.fikirzadem.databinding.FragmentInterestBinding
import com.basesoftware.fikirzadem.presentation.viewmodel.InterestViewModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.dataAvailable
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.toPostModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.WorkUtil.systemLanguage
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import java.util.*


class InterestFragment : Fragment() {

    private var _binding : FragmentInterestBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore : FirebaseFirestore

    private lateinit var textToSpeech : TextToSpeech

    private lateinit var popupCategory : PopupMenu

    private val sharedViewModel : SharedViewModel by activityViewModels { SharedViewModel.provideFactory(requireActivity().application, requireActivity()) }

    private val interestViewModel : InterestViewModel by viewModels()



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        _binding = FragmentInterestBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()

        firstDownload()

        listener()

    }

    override fun onDestroyView() {

        interestViewModel.setFragmentResume(false)

        textToSpeech.apply {
            stop()
            shutdown()
        }

        _binding = null

        super.onDestroyView()

    }

    private fun checkContext(context: Context.() -> Unit) { if (isAdded) { context(requireContext()) } }



    @SuppressLint("DiscouragedPrivateApi")
    private fun initialize() {

        binding.interest = interestViewModel

        firestore = WorkUtil.firestore()

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

            popupCategory = PopupMenu(requireContext(), binding.txtInterestCategoryPopup, Gravity.END)
            popupCategory.inflate(
                when(systemLanguage().matches(Regex("tr"))) {
                    true -> R.menu.newpost_category_menu_tr // Telefon dili türkçe ise türkçe kategori sıralaması
                    else -> R.menu.newpost_category_menu_en // Telefon dili türkçe değil ise ingilizce kategori sıralaması
                }
            )

            try {
                val declared = PopupMenu::class.java.getDeclaredField("mPopup")
                declared.isAccessible = true

                // Category popup ayarı
                val mpopup = declared.get(popupCategory)
                mpopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(mpopup,true)

            }
            catch (e : Exception){ Log.e("Interest-Popup","Interest kategori ikon gösterimi başarısız") }

            binding.recyclerInterest.apply {
                setHasFixedSize(true)
                layoutManager = when(sharedViewModel.getStaggeredLayout()) {
                    true -> StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                    else -> LinearLayoutManager(requireContext())
                }
                adapter = FeedRecyclerAdapter(
                    sharedViewModel,
                    textToSpeech,
                    interestViewModel.getInterestPostList(),
                    interestViewModel.getFragmentResume(),
                    "interest"
                )

                adapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            }

        }

    }

    private fun firstDownload() {

        checkContext {

            when(arguments != null && requireArguments().getInt("categoryId",-1) != -1) {
                true -> {
                    // FeedRecyclerAdaptör'den kategori textview'a basılarak gelindi

                    val argumentId = requireArguments().getInt("categoryId",-1)

                    /**
                     * <-- Uygulamada post kategorileri türkçe menüdeki [R.array.arrayCategoryText] sıralama {count} baz alınarak id atanıyor -->
                     */
                    val count = when(systemLanguage().matches(Regex("tr"))) {
                        true -> argumentId // Uygulama dili Türkçe ise Türkçe menü ekleniyor gelen argüman id'sini baz alıyor
                        else -> {
                            /**
                             * <-- İngilizce menü ise -->
                             * Feed Adaptörde post kategori id alınıyor & [R.array.arrayCategoryText]'te pozisyon bulunarak kategori text'i alınıyor
                             * Kategori text'i ingilizce menü sıralamasında [R.array.arrayCategoryTextEn] aranarak index {kategori id'si} alınıyor.
                             */
                            val trMenuItemText = requireActivity().resources.getStringArray(R.array.arrayCategoryText)[argumentId]
                            requireActivity().resources.getStringArray(R.array.arrayCategoryTextEn).indexOf(trMenuItemText)

                        }
                    }

                    popupCategory.menu[count].let { item ->

                        item.isChecked = true
                        interestViewModel.setCategory(item.itemId)

                        binding.txtInterestCategoryPopup.apply {
                            text = item.title.toString()
                            setCompoundDrawablesWithIntrinsicBounds(item.icon,null,null,null)
                        }

                    }

                }
                else -> {
                    // Bu fragment'a sol menüden girildi seçilmiş bir kategori yok dile uygun menüden ilk item gösterilecek
                    popupCategory.menu.findItem(interestViewModel.getCategory()).let { item ->

                        item.isChecked = true

                        binding.txtInterestCategoryPopup.apply {
                            text = item.title.toString()
                            setCompoundDrawablesWithIntrinsicBounds(item.icon,null,null,null)
                        }

                    }
                }
            }

            when(interestViewModel.getInterestPostList().isNullOrEmpty()) {

                true -> downloadInterestPost()

                else -> (binding.recyclerInterest.adapter as FeedRecyclerAdapter).setData(interestViewModel.getInterestPostList())

            }

        }

    }

    private fun listener() {

        popupCategory.setOnMenuItemClickListener {

            interestViewModel.apply {
                setCategory(it.itemId)
                clearInterestPostList()
            }

            (binding.recyclerInterest.adapter as FeedRecyclerAdapter).setData(interestViewModel.getInterestPostList())

            binding.txtInterestCategoryPopup.apply {
                text = it.title.toString()
                setCompoundDrawablesWithIntrinsicBounds(it.icon,null,null,null)
            }

            downloadInterestPost()

            return@setOnMenuItemClickListener true
        }

        binding.swipeRefreshInterest.apply {
            setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(),R.color.lite)) // Progressbar arka plan rengi
            setColorSchemeResources(R.color.white) // Progressbar iç rengi
            setOnRefreshListener {

                // Eğer şuan bir indirme işlemi yoksa
                if(!interestViewModel.getDataDownloading()) {

                    when(interestViewModel.getInterestPostList().isNullOrEmpty()) {
                        true -> downloadInterestPost()  // Refresh yapılmayacak ilk data indirmesi gibi data çekilecek
                        else -> downloadInterestPost(isRefresh = true) // Refresh yapılacak
                    }

                }

            }
        }

        binding.txtInterestCategoryPopup.setOnClickListener { popupCategory.show() }

        binding.recyclerInterest.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                sharedViewModel.getScrollDownLive().value?.let {

                    if(
                        it &&
                        !interestViewModel.getInterestPostList().isNullOrEmpty() &&
                        !recyclerView.canScrollVertically(1) && newState == 2
                    ) {
                        downloadInterestPost(isScroll = true)
                    }

                }

            }

        })

    }



    @SuppressLint("LogConditional")
    private fun logSnackbar(tag : String, message: Int, type : String) {

        CoroutineScope(Dispatchers.Main).launch {

            checkContext {

                when(type) {
                    "error" -> Log.e(tag, getString(message)) // Bir hata logu
                    "warning" -> Log.w(tag, getString(message)) // Bir tehlike logu
                    "info" -> Log.i(tag, getString(message)) // Bir bilgi logu
                }

                Snackbar
                    .make(requireActivity(), binding.root, getString(message), Snackbar.LENGTH_SHORT)
                    .settings()
                    .widthSettings()
                    .show()

            }

        }

    }




    private fun downloadInterestPost(isRefresh: Boolean = false, isScroll: Boolean = false) {

        checkContext {

            progress(true)

            val postRef = firestore
                .collection("Posts")
                .whereEqualTo("postCategoryId", requireActivity()
                    .resources
                    .getStringArray(R.array.arrayCategoryText)
                    .indexOf(binding.txtInterestCategoryPopup.text.toString()))

            val query = when {

                isRefresh -> postRef
                    .orderBy("postDate", Query.Direction.ASCENDING)
                    .startAfter(interestViewModel.getInterestPostList().first().postDate)
                    .limit(5)

                isScroll -> postRef
                    .orderBy("postDate", Query.Direction.DESCENDING)
                    .startAfter(interestViewModel.getInterestPostList().last().postDate)
                    .limit(5)

                else -> postRef.orderBy("postDate", Query.Direction.DESCENDING).limit(10)

            }

            CoroutineScope(Dispatchers.IO).launch {

                query
                    .get(Source.SERVER)
                    .addOnSuccessListener { snapshot ->

                        when(snapshot.dataAvailable()) {

                            true -> {
                                CoroutineScope(Dispatchers.Default).launch {

                                    for (document : DocumentSnapshot in snapshot.documents) {

                                        when(isRefresh) {
                                            true -> interestViewModel.setNewInterestPost(document.toPostModel())
                                            else -> interestViewModel.addInterestPostList(document.toPostModel())
                                        }

                                        if(document == snapshot.documents.last()) {
                                            withContext(Dispatchers.Main) {
                                                progress(false)

                                                (binding.recyclerInterest.adapter as FeedRecyclerAdapter).setData(
                                                    interestViewModel.getInterestPostList()
                                                )

                                                Log.i("InterestFrag-NewQuery", "İçerik yüklendi")
                                            }
                                        }

                                    }

                                }
                            }

                            else -> progress(false).also {
                                logSnackbar(
                                    "InterestFrag-NewQuery",
                                    when(interestViewModel.getInterestPostList().size > 0) {
                                        true -> R.string.old_post_fail
                                        else -> R.string.post_not_exists },
                                    "warning"
                                )
                            }

                        }

                    }
                    .addOnFailureListener {
                        progress(false)
                        logSnackbar("InterestFrag-NewQuery", R.string.post_profile_feed_download_fail, "error")
                        Log.e("InterestFrag-NewQuery", it.msg())
                    }

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
            interestViewModel.apply {

                setDataDownloading(downloading)

                when(downloading) {

                    true -> setLoadingInterest(false)

                    else -> {

                        binding.swipeRefreshInterest.isRefreshing = false

                        if(getInterestPostList().isNullOrEmpty()) setLoadingInterest(true)

                    }
                }

                binding.apply {

                    interest = interestViewModel

                    if(hasPendingBindings()) executePendingBindings()

                }

            }
        }

    }

}