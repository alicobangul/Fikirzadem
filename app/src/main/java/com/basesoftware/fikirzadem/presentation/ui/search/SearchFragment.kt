package com.basesoftware.fikirzadem.presentation.ui.search

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.RadioGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.presentation.adapter.SearchRecyclerAdapter
import com.basesoftware.fikirzadem.databinding.FragmentSearchBinding
import com.basesoftware.fikirzadem.presentation.viewmodel.SearchViewModel
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.WorkUtil.goMyProfile
import com.basesoftware.fikirzadem.util.ExtensionUtil.gone
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.show
import com.basesoftware.fikirzadem.util.ExtensionUtil.toSearchModelCache
import com.basesoftware.fikirzadem.util.ExtensionUtil.toSearchModelServer
import com.basesoftware.fikirzadem.util.ExtensionUtil.toUserModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore

    private lateinit var searchTypeDialog : BottomSheetDialog
    private lateinit var searchTypeSheetView : View

    private val sharedViewModel : SharedViewModel by activityViewModels { SharedViewModel.provideFactory(requireActivity().application, requireActivity()) }
    private val searchViewModel : SearchViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, instance: Bundle?): View {

        _binding = FragmentSearchBinding.inflate(layoutInflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeSearch()

        settingsBottomDialog()

        inputSettings()

        searchListener()

    }

    override fun onDestroyView() {

        if(searchTypeDialog.isShowing) searchTypeDialog.dismiss()

        _binding = null

        super.onDestroyView()
    }

    private fun checkContext(context: Context.() -> Unit) { if (isAdded) { context(requireContext()) } }



    private fun initializeSearch() {

        binding.apply {

            firestore = WorkUtil.firestore()

            searchTypeDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)

            progressSearchDownload.gone()

            checkContext {

                recyclerSearch.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = SearchRecyclerAdapter(sharedViewModel)

                    adapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

                }

                if(searchViewModel.getSearchList().isNotEmpty()) {
                    searchViewModel.setNewQuery(false)
                    (recyclerSearch.adapter as SearchRecyclerAdapter).setData(searchViewModel.getSearchList())
                }

            }

        }

    }

    private fun searchListener() {

        sharedViewModel.getUserLive().observe(viewLifecycleOwner, { binding.shared = sharedViewModel })

        binding.imgSearchSettings.setOnClickListener {
            val tag = "SearchFragSettings"
            try {
                searchTypeDialog.show()

                Log.i(tag, "SearchFrag - SettingsDialog gösterildi")
            }
            catch (e: Exception) {
                Log.e(tag, "SearchFrag - SettingsDialog gösterilemedi")
                Log.e(tag, e.msg())
            }

        }

        binding.imgSearchMyImg.setOnClickListener { goMyProfile(findNavController(), sharedViewModel.getMyUserId()) }

        binding.recyclerSearch.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if(
                    newState == 2 &&
                    !recyclerView.canScrollVertically(1) &&
                    sharedViewModel.getScrollDownLive().value == true
                ) {

                    if(binding.progressSearchDownload.visibility == View.GONE) {

                        searchViewModel.setScrolling(true).also { searchCache() }

                    }
                }
            }

        })

    }

    @SuppressLint("LogConditional")
    private fun logSnackbar(message: Int, type : String) {

        CoroutineScope(Dispatchers.Main).launch {

            checkContext {

                when(type) {
                    "error" -> Log.e("SearchFrag-SearchCache", getString(message)) // Bir hata logu
                    "warning" -> Log.w("SearchFrag-SearchCache", getString(message)) // Bir tehlike logu
                    "info" -> Log.i("SearchFrag-SearchCache", getString(message)) // Bir bilgi logu
                }

                Snackbar
                    .make(requireContext(),binding.root, getString(message), Snackbar.LENGTH_SHORT)
                    .settings()
                    .widthSettings()
                    .show()

            }

        }

    }



    @SuppressLint("InflateParams")
    private fun settingsBottomDialog() {

        val tag = "SearchFragSettingsSheet"
        try {

            checkContext {

                searchTypeSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_dialog_search, null)

                searchTypeDialog.setContentView(searchTypeSheetView)

                searchTypeSheetView.findViewById<Button>(R.id.btnSearchSheetOk).setOnClickListener {

                    searchViewModel.setSearchType(
                        when(searchTypeSheetView.findViewById<RadioGroup>(R.id.radioGroupSearchSheet).checkedRadioButtonId){
                            R.id.radioSearchUserName -> "userName"
                            R.id.radioSearchRealName -> "userRealName"
                            else -> "userMail"
                        }
                    )

                    searchTypeDialog.dismiss()

                }

                Log.i(tag, "SearchFrag - SettingsDialog gösterildi")

            }

        }
        catch (e: Exception) {
            Log.e(tag, "SearchFrag - SettingsDialog gösterilemedi")
            Log.e(tag, e.msg())
        }

    }

    private fun inputSettings() {

        binding.txtSearchText.setOnEditorActionListener { _, action, _ ->

            if(action == EditorInfo.IME_ACTION_SEARCH) {

                try {
                    checkContext {
                        (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken,0)
                    }
                }
                catch (e : Exception) { Log.w("Search-hideKeyboard", e.msg()) }

                when(binding.txtSearchText.text.isNullOrEmpty()) {

                    true -> {
                        checkContext {
                            Log.e("SearchFragInputSettings", getString(R.string.info_empty))
                            Snackbar
                                .make(this@SearchFragment.requireView(), getString(R.string.info_empty), Snackbar.LENGTH_SHORT)
                                .settings().widthSettings().show()
                        }
                    }
                    false -> {
                        searchViewModel.setNewQuery(true).also { startSearch() }

                    }
                }

            }

            return@setOnEditorActionListener true
        }

    }


    private fun startSearch() {

        binding.apply {

            if(searchViewModel.getNewQuery()) {

                txtSearchText.apply {
                    setText(text.toString().replace("\\s+".toRegex(), " ").trim())
                    setSelection(text.toString().replace("\\s+".toRegex(), " ").trim().length)
                }

                searchViewModel.apply {
                    setScrolling(false)
                    searchListClear()
                    setSearchText(txtSearchText.text.toString())
                }

                searchFirebase()

            }

        }

    }

    private fun searchCache() {

        binding.progressSearchDownload.show()

        val tagSearchCache = "SearchFrag-SearchCache"

        CoroutineScope(Dispatchers.IO).launch {

            // Önbellekte aranan field baz alınarak sözcük araması yap

            val searchEnd = when(searchViewModel.getSearchType().matches(Regex("userMail"))){
                true -> searchViewModel.getSearchText()
                false -> searchViewModel.getSearchText()+"z"
            }

            var query = when(searchViewModel.getSearchType().matches(Regex("userMail"))){
                /**
                 *  Eğer aranan email ise, birebir aynı e-mail yazılmalıdır. {whereEqualTo}
                 *  Eğer aranan e-mail değil ise, arama sözcüklerini içeren bütün sonuçları getir {whereGreaterThan}
                 *  Amaç e-mail hesapların belli edilmemesi, kullanıcıların güvenliğinin sağlanması
                 */
                true -> firestore
                    .collection("Users")
                    .whereEqualTo(searchViewModel.getSearchType(), searchViewModel.getSearchText())

                false -> firestore
                    .collection("Users")
                    .orderBy(searchViewModel.getSearchType())
                    .whereGreaterThan(searchViewModel.getSearchType(), searchViewModel.getSearchText())

            }

            if(!searchViewModel.getSearchType().matches(Regex("userMail"))) {

                query = when (!searchViewModel.getScrolling()) {

                    /**
                     *  Eğer scroll yapılmıyorsa arama şu şekilde
                     *  Serverdan alınan data varsa data sayısına göre sözcük öbeğine harf eklenerek önbellekte arama yapılıyor
                     *  Böylece serverdan ve önbellekten aynı data alınmıyor
                     */
                    true -> query.startAt(when(searchViewModel.getSearchList().size) {
                        1 -> searchViewModel.getSearchText()
                        2 -> searchViewModel.getSearchText()+"b"
                        3 -> searchViewModel.getSearchText()+"c"
                        4 -> searchViewModel.getSearchText()+"d"
                        else -> searchViewModel.getSearchText()+"e"
                    }).endAt(searchEnd) // Scroll yapılmıyor

                    false -> query.startAfter(searchViewModel.getLastSearchUser()).endAt(searchEnd) // Scroll yapılıyor

                }

            }

            query
                .limit(50)
                .get(Source.CACHE)
                .addOnSuccessListener {

                    when(!it.documents.isNullOrEmpty()) {
                        true -> {
                            CoroutineScope(Dispatchers.Default).launch {

                                for (document: DocumentSnapshot in it.documents) {

                                    if(document.toUserModel().userIsActive) {
                                        // Eğer kullanıcı aktif ise {admin tarafından silinme emri verilmedi ise}

                                            checkContext {

                                                searchViewModel.apply {
                                                    setSearchUser(document.toSearchModelCache())
                                                    setLastSearchUser(document.getString(getSearchType()) ?: "x")
                                                }

                                            }

                                    }

                                    if(document == it.documents.last()) {

                                        withContext(Dispatchers.Main) {

                                            checkContext {

                                                (binding.recyclerSearch.adapter as SearchRecyclerAdapter).setData(searchViewModel.getSearchList())
                                                binding.progressSearchDownload.gone()
                                            }

                                        }

                                    }

                                }

                            }
                        }
                        else -> {

                            CoroutineScope(Dispatchers.Main).launch {
                                /**
                                 * Önbellekte data yok
                                 * Daha önce eleman alınabildi ise "Başka kullanıcı yok", liste boş ise "Kullanıcı yok"
                                 */
                                when(searchViewModel.getSearchList().isEmpty()){
                                    true -> logSnackbar(R.string.user_not_exists,"info")
                                    else -> if(searchViewModel.getScrolling()) logSnackbar(R.string.more_user_fail, "info")
                                }

                                binding.progressSearchDownload.gone()
                            }
                        }
                    }

                }
                .addOnFailureListener {
                    Log.e(tagSearchCache, "Önbellekte kullanıcı arama başarısız")
                    if(searchViewModel.getSearchList().isEmpty()) {
                        // Hiç data getirilemedi ise
                        binding.progressSearchDownload.gone()
                        logSnackbar(R.string.user_not_exists,"error")
                    }
                }

        }


    }

    private fun searchFirebase() {

        val tagSearchFirebase = "SearchFragSearcFirebase"

        CoroutineScope(Dispatchers.IO).launch {

            val searchEnd = when(searchViewModel.getSearchType().matches(Regex("userMail"))){
                true -> searchViewModel.getSearchText()
                false -> searchViewModel.getSearchText()+"z"
            }

            val query = when(searchViewModel.getSearchType().matches(Regex("userMail"))){
                /**
                 *  Eğer aranan email ise, birebir aynı e-mail yazılmalıdır. {whereEqualTo}
                 *  Eğer aranan e-mail değil ise, arama sözcüklerini içeren bütün sonuçları getir {whereGreaterThan}
                 *  Amaç e-mail adresinin belli edilmemesi, kullanıcıların güvenliğinin sağlanması
                 */
                true -> firestore
                    .collection("Users")
                    .whereEqualTo(searchViewModel.getSearchType(), searchViewModel.getSearchText())
                false -> firestore
                    .collection("Users")
                    .orderBy(searchViewModel.getSearchType())
                    .whereGreaterThanOrEqualTo(searchViewModel.getSearchType(), searchViewModel.getSearchText())
                    .startAt(searchViewModel.getSearchText())
                    .endAt(searchEnd)
            }

            // Kayıt tarihi en eskiden başlayarak 5 kullanıcı getir
            query
                .limit(5)
                .get(Source.SERVER)
                .addOnSuccessListener {

                    CoroutineScope(Dispatchers.Default).launch {
                        if(it.documents.isNotEmpty()) {
                            for (document : DocumentSnapshot in it.documents) {

                                // Eğer kullanıcı aktif ise {admin tarafından silinmedi ise} listeye ekle
                                if(document.toUserModel().userIsActive) searchViewModel.setSearchUser(document.toSearchModelServer())

                                if(document == it.documents.last()) {

                                    withContext(Dispatchers.Main) {

                                        checkContext {

                                            (binding.recyclerSearch.adapter as SearchRecyclerAdapter).setData(searchViewModel.getSearchList())

                                        }

                                    }

                                }

                            }
                        }

                        else { Log.i(tagSearchFirebase, "Kullanıcı search edildi ama firebasede bulunamadı") }
                    }

                    searchViewModel.setScrolling(false).also { searchCache() }

                }
                .addOnFailureListener {
                    Log.i(tagSearchFirebase, "Kullanıcı search edildi ama firebasede bulunamadı")
                    searchViewModel.setScrolling(false).also { searchCache() }
                }

        }

    }


}