package com.basesoftware.fikirzadem.presentation.ui.feed

import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.presentation.adapter.FeedRecyclerAdapter
import com.basesoftware.fikirzadem.databinding.FragmentFeedBinding
import com.basesoftware.fikirzadem.util.ExtensionUtil.dataAvailable
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.ExtensionUtil.gone
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.toPostModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil.systemLanguage
import com.basesoftware.fikirzadem.presentation.viewmodel.FeedViewModel
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import java.util.*


class FeedFragment : Fragment() {

    private var _binding : FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore : FirebaseFirestore

    private lateinit var textToSpeech : TextToSpeech

    private val sharedViewModel : SharedViewModel by activityViewModels { SharedViewModel.provideFactory(requireActivity().application, requireActivity()) }

    private val feedViewModel : FeedViewModel by viewModels()


    override fun onCreateView(inf: LayoutInflater, cont: ViewGroup?, instance: Bundle?) : View {

        _binding = FragmentFeedBinding.inflate(layoutInflater, cont, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        listener()
//
//        initialize()

    }

    override fun onDestroyView() {

        feedViewModel.setFragmentResume(false)

        textToSpeech.apply {
            stop()
            shutdown()
        }

        _binding = null

        super.onDestroyView()

    }

    private fun checkContext(context: Context.() -> Unit) { if (isAdded) { context(requireContext()) } }



    @SuppressLint("LogConditional")
    private fun logSnackbar(message: Int, type : String) {

        CoroutineScope(Dispatchers.Main).launch {

            checkContext {

                when(type){
                    "error" -> Log.e("FeedFrag-DownloadData", getString(message)) // Bir hata logu
                    "warning" -> Log.w("FeedFrag-DownloadData", getString(message)) // Bir tehlike logu
                    "info" -> Log.i("FeedFrag-DownloadData", getString(message)) // Bir bilgi logu
                }

                Snackbar
                    .make(requireActivity(), binding.recyclerFeed, getString(message), Snackbar.LENGTH_SHORT)
                    .settings()
                    .widthSettings()
                    .show()

            }

        }

    }

    private fun newDataButtonAnimation() {

        CoroutineScope(Dispatchers.Main).launch {

            AnimatorInflater.loadAnimator(requireActivity(), R.animator.newpostanimator).apply {
                setTarget(binding.btnFeedNewPostExist)
                start()
            }

        }
    }




    private fun initialize() {

        binding.apply {

            feed = feedViewModel

        }

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

            binding.recyclerFeed.apply {
                setHasFixedSize(true)

                layoutManager = when(sharedViewModel.getStaggeredLayout()) {
                    true -> StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                    else -> LinearLayoutManager(requireContext())
                }

                adapter = FeedRecyclerAdapter(
                    sharedViewModel,
                    textToSpeech,
                    feedViewModel.getPostList(),
                    feedViewModel.getFragmentResume(),
                    "feed"
                )

                adapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            }

        }

        when(feedViewModel.getPostList().isNullOrEmpty()) {

            true -> downloadData()

            else -> {

                (binding.recyclerFeed.adapter as FeedRecyclerAdapter).setData(feedViewModel.getPostList()) // Restore için

                CoroutineScope(Dispatchers.IO).launch {
                    firestore
                        .collection("Posts")
                        .orderBy("postDate", Query.Direction.ASCENDING)
                        .startAfter(feedViewModel.getPostList()[0].postDate)
                        .limit(1)
                        .get(Source.SERVER)
                        .addOnSuccessListener {
                            if(it.dataAvailable()) CoroutineScope(Dispatchers.Main).launch { newDataButtonAnimation() }
                        }
                }

            }
        }

    }


    private fun listener() {

        checkContext {

            binding.apply {

                val refresh = Runnable {

                    // Eğer şuan bir indirme işlemi yoksa
                    if(!feedViewModel.getDataDownloading()) {

                        when(feedViewModel.getPostList().isNullOrEmpty()) {
                            true -> feedViewModel.setIsRefreshing(false) // Refresh yapılmayacak ilk data indirmesi gibi data çekilecek
                            else -> feedViewModel.setIsRefreshing(true) // Refresh yapılacak
                        }

                        downloadData() // İndirmeyi başlat
                    }

                }

                swipeRefreshFeed.apply {
                    setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(),R.color.lite)) // Progressbar arka plan rengi
                    setColorSchemeResources(R.color.white) // Progressbar iç rengi
                    setOnRefreshListener { refresh.run() }
                }

                btnFeedNewPostExist.setOnClickListener { refresh.run() }

                recyclerFeed.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)

                        if(newState == 1 && btnFeedNewPostExist.visibility == View.VISIBLE) btnFeedNewPostExist.gone()

                        sharedViewModel.getScrollDownLive().value?.let {

                            if(it &&
                                !feedViewModel.getDataDownloading() && !feedViewModel.getLoadingFeed() &&
                                newState == 2 && !recyclerView.canScrollVertically(1)) {

                                feedViewModel.setIsRefreshing(false)

                                downloadData()

                            }

                        }

                    }
                })
            }

        }

    }


    private fun downloadData() {

        /**
         *
         * ----------> ILK DATA INDIRME {DESCENDING - Yakın tarihten en eski tarihe göre sırala} <----------
         *
         * ----------> REFRESH {ASCENDING - Eski tarihten yeni tarihe göre sırala} <----------
         * Sunucuda son posttan ilk posta doğru sıralama DESCENDING (c - b - a) -> [a en eski post]
         * Data indirme sonrası PostList eleman sıralaması (c - b - a) -> [c en yeni post 0.index]
         * 3 yeni post atıldı {d - e - f} -> Sunucuda yeni sıralama DESCENDING (f-e-d-c-b-a) -> [a en eski post]
         * Refresh zamanı ASCENDING sıralama yap -> (a-b-c-d-e-f)
         * PostList'teki 0.elemanı baz alarak {c} 5 yeni data al -> {d - e - f}
         * Her yeni elemanı PostList'te 0.index'e ekle -> [com.basesoftware.fikirzadem.viewmodel.FeedViewModel.setNewPost]
         * PostList durumu 0.indexten : {c-b-a} -> {d-c-b-a} -> {e-d-c-b-a} -> {f-e-d-c-b-a}
         *
         * ----------> SCROLL / PAGINATION {DESCENDING - Yakın tarihten en eski tarihe göre sırala} <----------
         * Data indirmeye PostList'teki son post'un tarihini baz alarak başla [startAfter -> o hariç]
         *
         */

        progress(true)

        val postCollection = firestore.collection("Posts")

        val query = when(feedViewModel.getPostList().isNullOrEmpty()) {

            // PostList boş - ilk data indirmesi
            true -> postCollection
                .orderBy("postDate", Query.Direction.DESCENDING)
                .limit(10)

            else -> when(feedViewModel.getIsRefreshing()) {

                // PostList dolu - Refresh yapılıyor (yeni datalar için)
                true -> postCollection
                    .orderBy("postDate", Query.Direction.ASCENDING)
                    .startAfter(feedViewModel.getPostList()[0].postDate)
                    .limit(5)

                // PostList dolu - Scroll yapılıyor (daha eski datalar için)
                else -> postCollection
                    .orderBy("postDate", Query.Direction.DESCENDING)
                    .startAfter(feedViewModel.getPostList().last().postDate)
                    .limit(10)
            }

        }

        CoroutineScope(Dispatchers.IO).launch {

            query
                .get(Source.SERVER)
                .addOnSuccessListener { snapshot ->

                    if(snapshot.dataAvailable()) {

                        CoroutineScope(Dispatchers.Default).launch {

                            for (document : DocumentSnapshot in snapshot.documents) {

                                when(feedViewModel.getIsRefreshing()) {
                                    true -> feedViewModel.setNewPost(document.toPostModel())
                                    else -> feedViewModel.setPost(document.toPostModel())
                                }

                                if(document == snapshot.documents.last()) {

                                    withContext(Dispatchers.Main) {
                                        checkContext {

                                            (binding.recyclerFeed.adapter as FeedRecyclerAdapter).setData(feedViewModel.getPostList())

                                            progress(false)

                                            if(feedViewModel.getIsRefreshing()) binding.recyclerFeed.smoothScrollToPosition(0)

                                            Log.i("FeedFrag-DownloadData", "İçerik yüklendi")

                                        }
                                    }

                                }

                            }

                        }

                    }

                    else {

                        logSnackbar(

                            when(feedViewModel.getPostList().isNullOrEmpty()) {

                                /**
                                 * PostList boş, refresh veya ilk data indirmesi yapıldı
                                 * {PostList boş iken scroll yapılamaz}
                                 */
                                true -> R.string.post_not_exists // PostList boş - refresh veya ilk indirme - data yok

                                else -> when(feedViewModel.getIsRefreshing()) {

                                    /**
                                     * PostList dolu, refresh veya scroll yapıldı
                                     * {PostList dolu iken ilk data indirmesi yapılamaz}
                                     */
                                    true -> R.string.new_post_fail // PostList dolu - refresh yapıldı - yeni data yok
                                    else -> R.string.old_post_fail // PostList dolu - scroll yapıldı - yeni data yok

                                } }, "warning")

                        progress(false)

                    }

                }
                .addOnFailureListener {

                    progress(false)
                    logSnackbar(R.string.post_profile_feed_download_fail, "error")
                    Log.e("FeedFrag-DownloadData", it.msg())

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
            feedViewModel.apply {

                setDataDownloading(downloading)

                when(downloading) {
                    true -> setLoadingFeed(false)
                    else -> {
                        binding.swipeRefreshFeed.isRefreshing = false
                        if(getPostList().isNullOrEmpty()) setLoadingFeed(true)
                    }
                }

                binding.apply {

                    feed = feedViewModel

                    btnFeedNewPostExist.gone()

                    if(hasPendingBindings()) executePendingBindings()

                }

            }
        }

    }

}