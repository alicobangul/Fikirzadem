package com.basesoftware.fikirzadem.presentation.ui.favorite

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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.room.Room
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.presentation.adapter.FavoriteRecyclerAdapter
import com.basesoftware.fikirzadem.databinding.FragmentFavoriteBinding
import com.basesoftware.fikirzadem.presentation.viewmodel.FavoriteViewModel
import com.basesoftware.fikirzadem.data.local.FikirzademDatabase
import com.basesoftware.fikirzadem.util.ExtensionUtil.gone
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.ExtensionUtil.hide
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil.systemLanguage
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class FavoriteFragment : Fragment() {

    private var _binding : FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore : FirebaseFirestore

    private lateinit var textToSpeech : TextToSpeech

    private val sharedViewModel : SharedViewModel by activityViewModels { SharedViewModel.provideFactory(requireActivity().application, requireActivity()) }

    private val favoriteViewModel : FavoriteViewModel by viewModels()

    private lateinit var favoriteAdapter : FavoriteRecyclerAdapter

    private lateinit var db : FikirzademDatabase


    override fun onCreateView(inf: LayoutInflater, cont: ViewGroup?, instance: Bundle?): View {
        _binding = FragmentFavoriteBinding.inflate(layoutInflater, cont, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()

    }

    override fun onDestroyView() {

        favoriteViewModel.setFragmentResume(false)

        textToSpeech.apply {
            stop()
            shutdown()
        }

        _binding = null

        super.onDestroyView()

    }



    private fun initialize() {

        firestore = WorkUtil.firestore()

        favoriteViewModel.setUser(sharedViewModel.getMyUserId())

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

        }

        checkDatabase()

    }


    @SuppressLint("LogConditional")
    private fun checkDatabase() {

        CoroutineScope(Dispatchers.IO).launch {

            val tag = "Favorite-CheckFavorite"

            try {

                checkContext {

                    db = Room.databaseBuilder(requireContext(), FikirzademDatabase::class.java,"FikirzademDB").build()
                    val favoriteSize = db.fikirzademDao().getSavedPostSize(sharedViewModel.getMyUserId()) ?: 0
                    db.close()

                    CoroutineScope(Dispatchers.Main).launch {
                        when(favoriteSize == 0) {

                            true -> {
                                CoroutineScope(Dispatchers.Main).launch {
                                    checkContext {

                                        Log.i(tag, getString(R.string.post_not_exists))

                                        Snackbar
                                            .make(requireActivity(), binding.root,getString(R.string.post_not_exists), Snackbar.LENGTH_SHORT)
                                            .settings()
                                            .widthSettings()
                                            .show()

                                    }
                                }

                            }

                            else -> CoroutineScope(Dispatchers.Main).launch { connectPagingAdapter() }

                        }

                        binding.progressFavorite.hide()
                    }

                    Log.i(tag, "Veritabanı okuma başarılı")

                }

            }
            catch (e: Exception) {
                Log.e(tag, "Veritabanı okuma başarısız")
                Log.e(tag, e.msg())
            }
        }

    }


    private fun connectPagingAdapter() {

        CoroutineScope(Dispatchers.Main).launch { binding.progressFavoriteLoading.gone() }

        checkContext {
            favoriteAdapter = FavoriteRecyclerAdapter(binding.root, textToSpeech, sharedViewModel, favoriteViewModel.getFragmentResume())
            binding.recyclerFavorite.apply {
                setHasFixedSize(true)
                layoutManager = when(sharedViewModel.getStaggeredLayout()) {
                    true -> StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                    else -> LinearLayoutManager(requireContext())
                }
                adapter = favoriteAdapter
            }
        }
        lifecycleScope.launch(Dispatchers.IO) { favoriteViewModel.getDbData().collect { favoriteAdapter.submitData(it) } }

    }



    private fun checkContext(context: Context.() -> Unit) { if (isAdded && context != null) { context(requireContext()) } }


}