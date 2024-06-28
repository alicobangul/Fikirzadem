package com.basesoftware.fikirzadem.presentation.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import androidx.core.content.edit
import androidx.fragment.app.activityViewModels
import androidx.room.Room
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.FragmentSettingsBinding
import com.basesoftware.fikirzadem.data.local.FikirzademDatabase
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.questionSnackbar
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding : FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel : SharedViewModel by activityViewModels { SharedViewModel.provideFactory(requireActivity().application, requireActivity()) }

    private lateinit var sharedPreferences : SharedPreferences

    override fun onCreateView(inf: LayoutInflater, cont: ViewGroup?, instance: Bundle?): View {

        _binding = FragmentSettingsBinding.inflate(layoutInflater, cont, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()

        listener()

    }

    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()

    }

    private fun checkContext(context: Context.() -> Unit) { if (isAdded) { context(requireContext()) } }



    private fun initialize() {

        binding.shared = sharedViewModel

        checkContext { requireContext().apply { sharedPreferences = getSharedPreferences(packageName.toString(),0) } }

    }

    private fun listener() {

        val radioListener = CompoundButton.OnCheckedChangeListener { buttonView, _ ->
            if((buttonView as RadioButton).isChecked) {
                when(buttonView.id) {
                    R.id.radioStaggered -> binding.radioLinear.isChecked = false
                    R.id.radioLinear -> binding.radioStaggered.isChecked = false
                }
            }
        }

        binding.apply {

            radioStaggered.setOnCheckedChangeListener(radioListener)

            radioLinear.setOnCheckedChangeListener(radioListener)

            btnFeedType.setOnClickListener {

                sharedPreferences.edit().putBoolean("_staggeredLayout", radioStaggered.isChecked).commit()

                sharedViewModel.setStaggeredLayout(radioStaggered.isChecked)

                snackbar("SettingsFrag-FeedType", R.string.save_changed, "info")

            }

            btnSettingsSave.setOnClickListener {

                sharedPreferences.edit {

                    putBoolean("_sideMenuAlignment", switchMenuAlignment.isChecked)
                    sharedViewModel.setSideMenuAlignment(switchMenuAlignment.isChecked)

                    putBoolean("_notification", switchNotification.isChecked)
                    sharedViewModel.setNotification(switchNotification.isChecked)

                    putBoolean("_animation", switchAnimation.isChecked)
                    sharedViewModel.setAnimation(switchAnimation.isChecked)

                    putBoolean("_rightMenu", switchRightMenu.isChecked)
                    sharedViewModel.setRightMenu(switchRightMenu.isChecked)

                    putBoolean("_bottomMenu", switchBottomMenu.isChecked)
                    when(switchBottomMenu.isChecked) {
                        true -> {
                            sharedViewModel.setBottomMenu(switchBottomMenu.isChecked) // Önce alt menü görünürlüğü ayarlandı
                            sharedViewModel.setScrollDown(!switchBottomMenu.isChecked) // Alt menü gösterildi
                        }
                        else -> {
                            sharedViewModel.setScrollDown(!switchBottomMenu.isChecked) // Alt menü gizlendi
                            sharedViewModel.setBottomMenu(switchBottomMenu.isChecked) // Alt menü görünürlüğü ayarlandı
                        }
                    }

                    commit()

                }

                snackbar("SettingsFrag-Settings", R.string.save_changed,"info")

            }

            btnRoomDelete.setOnClickListener {

                checkContext {

                    Snackbar
                        .make(requireActivity(), root, getString(R.string.favorite_post_delete_in_room), Snackbar.LENGTH_LONG)
                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(WorkUtil.snackbarColor(requireContext()))
                        .setTextColor(Color.LTGRAY)
                        .setGestureInsetBottomIgnored(true)
                        .questionSnackbar()
                        .setAction(getString(R.string.yes)) {

                            CoroutineScope(Dispatchers.IO).launch {

                                val tag = "SettingsFragment"
                                try {
                                    checkContext {

                                        val db = Room
                                            .databaseBuilder(requireContext(), FikirzademDatabase::class.java,"FikirzademDB")
                                            .build()

                                        db.fikirzademDao().deleteAllSavedPost()
                                        db.close()

                                        Log.i(tag, "Veritabanındaki bütün favori postlar silindi")

                                        snackbar(tag, R.string.favorite_post_delete_success, "info")

                                    }
                                }
                                catch (e: Exception) {
                                    Log.e(tag, "Veritabanındaki bütün favori postlar silinemedi")
                                    snackbar(tag, R.string.error, "error")
                                    Log.e(tag, e.msg())
                                }
                            }

                        }
                        .setActionTextColor(Color.RED)
                        .widthSettings()
                        .show()

                }

            }

        }

    }

    @SuppressLint("LogConditional")
    private fun snackbar(tag : String, msg : Int, type: String) {

        CoroutineScope(Dispatchers.Main).launch {

            try {

                checkContext {

                    when(type) {
                        "error" -> Log.e(tag, getString(msg))
                        "warning" -> Log.w(tag, getString(msg))
                        "info" -> Log.i(tag, getString(msg))
                    }

                    Snackbar
                        .make(requireActivity(), binding.root, getString(msg), Snackbar.LENGTH_SHORT)
                        .settings()
                        .widthSettings()
                        .show()

                }

            }
            catch (e : Exception){ Log.e("Settings-Snackbar", e.msg()) }

        }

    }

}