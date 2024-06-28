package com.basesoftware.fikirzadem.presentation.ui.contactus

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.FragmentContactUsBinding
import com.basesoftware.fikirzadem.model.ContactModel
import com.basesoftware.fikirzadem.presentation.viewmodel.ContactUsViewModel
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.WorkUtil.changeFragment
import com.basesoftware.fikirzadem.util.WorkUtil.goMyProfile
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.toContactModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ContactUsFragment : Fragment() {

    private var _binding: FragmentContactUsBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore

    private val sharedViewModel : SharedViewModel by activityViewModels { SharedViewModel.provideFactory(requireActivity().application, requireActivity()) }

    private val contactViewModel: ContactUsViewModel by viewModels()


    override fun onCreateView(inf: LayoutInflater, cont: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentContactUsBinding.inflate(layoutInflater, cont, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()

        listener()

    }


    override fun onConfigurationChanged(newConfig: Configuration) {

        super.onConfigurationChanged(newConfig)

    }

    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()
    }


    private fun initialize() {

        firestore = WorkUtil.firestore()

        when (contactViewModel.getLastOperation().isNullOrEmpty()) {
            true -> contactViewModel.setUiEnable(true)
            false -> {

                when (contactViewModel.getLastOperation()) {

                    "ContactUs-CheckMessage" -> checkMessage()

                    "ContactUs-sendMessage" -> sendMessage()

                    "ContactUs-checkDate" -> checkDate()

                }

            }
        }

    }

    @SuppressLint("LogConditional")
    private fun logSnackbar(tag: String, message: Int, type: String, goFeed: Boolean) {

        CoroutineScope(Dispatchers.Main).launch {

            try {
                checkContext {

                    when (type) {
                        "error" -> Log.e(tag, getString(message)) // Bir hata logu
                        "warning" -> Log.w(tag, getString(message)) // Bir tehlike logu
                        "info" -> Log.i(tag, getString(message)) // Bir bilgi logu
                    }

                    val snack = Snackbar
                        .make(requireActivity(), binding.root, getString(message), Snackbar.LENGTH_SHORT)
                        .settings()
                        .widthSettings()
                        .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {

                                checkContext {

                                    if (goFeed) changeFragment(findNavController(), null, R.id.feedFragment)
                                    else contactViewModel.setUiEnable(true)

                                }

                            }
                        })

                    snack.show()

                }

            }
            catch (e: Exception) { Log.e("Contact-Snackbar-Catch", e.msg()) }


        }

    }


    private fun listener() {

        contactViewModel.getUiEnable()
            .observe(viewLifecycleOwner, { binding.contactus = contactViewModel })

        sharedViewModel.getUserLive()
            .observe(viewLifecycleOwner, { binding.shared = sharedViewModel })

        binding.txtAddContactUs.addTextChangedListener {

            binding.txtContactUsLenght.text = (500 - binding.txtAddContactUs.text.length).toString()
            binding.contactUsLengthProgress.progress = 500 - binding.txtAddContactUs.text.length

        }

        binding.imgContactUsPp.setOnClickListener {
            contactViewModel.setUiEnable(false)
            goMyProfile(findNavController(), sharedViewModel.getMyUserId())
        }

        binding.imgContactUsGoBack.setOnClickListener {
            contactViewModel.setUiEnable(false)
            changeFragment(findNavController(), null, R.id.feedFragment)
        }

        binding.btnSendContactUs.setOnClickListener {

            contactViewModel.setUiEnable(false)

            val tag = "ContactUs-SendClick"

            try {

                requireView().clearFocus() // Fokus temizlendi

                val hideKeyboard =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                hideKeyboard?.hideSoftInputFromWindow(it.windowToken, 0)

            }
            catch (e: Exception) {
                Log.e(tag, e.msg())
            }

            binding.txtAddContactUs.apply {

                when (length() < 30) {

                    true -> logSnackbar(tag, R.string.minchar_limit, "error", goFeed = false)

                    else -> {

                        sharedViewModel.getUser()?.let { user ->

                            when(!user.userAddContact) {

                                true -> logSnackbar(tag, R.string.notification_new_contact_block, "error", goFeed = true)

                                else -> when (text.toString().trim().isEmpty()) {

                                    true -> logSnackbar(tag, R.string.spam_error, "error", goFeed = false)

                                    else -> checkMessage()

                                }

                            }

                        }

                    }

                }

            }

        }

    }

    private fun checkMessage() {

        val tag = "ContactUs-CheckMessage"

        contactViewModel.setLastOperation(tag)

        CoroutineScope(Dispatchers.IO).launch {

            firestore
                .collection("Contact")
                .whereEqualTo("contactUserId", sharedViewModel.getMyUserId())
                .orderBy("contactDate", Query.Direction.DESCENDING)
                .limit(1)
                .get(Source.SERVER)
                .addOnCompleteListener {

                    when (it.isSuccessful) {

                        true -> {
                            when (it.result.documents.isNotEmpty()) {
                                true -> {
                                    Log.i("xdxd", "Mesaj var zaman kontrolü")
                                    contactViewModel
                                        .setLastMessageDate(
                                            it.result.documents[0].toContactModel().contactDate!!
                                        )
                                    checkDate()
                                }
                                false -> sendMessage()
                            }
                        }

                        false -> {
                            logSnackbar(
                                tag,
                                R.string.msg_error,
                                "error",
                                goFeed = false)
                            it.exception?.msg()?.let { msg -> Log.e(tag, msg) }
                        }

                    }

                    contactViewModel.setLastOperation(null)

                }

        }


    }


    private fun sendMessage() {

        val tag = "ContactUs-sendMessage"
        contactViewModel.setLastOperation(tag)

        CoroutineScope(Dispatchers.IO).launch {

            val contactId = UUID.randomUUID().toString()

            val contactModel = ContactModel(
                contactId,
                sharedViewModel.getMyUserId(),
                binding.txtAddContactUs.text.toString(),
                null
            )

            firestore
                .collection("Contact")
                .document(contactId)
                .set(contactModel)
                .addOnCompleteListener {

                    when (it.isSuccessful) {

                        true -> logSnackbar(
                            tag,
                            R.string.msg_complete,
                            "info",
                            goFeed = true
                        )

                        else -> {
                            logSnackbar(
                                tag,
                                R.string.msg_error,
                                "error",
                                goFeed = false
                            )
                            it.exception?.msg()?.let { msg -> Log.e(tag, msg) }
                        }

                    }

                    contactViewModel.setLastOperation(null)

                }
        }

    }


    private fun checkDate() {

        val tag = "ContactUs-checkDate"
        contactViewModel.setLastOperation(tag)

        CoroutineScope(Dispatchers.Default).launch {

            val nowDate = Date(Timestamp.now().seconds * 1000) // Timestamp saniyeye çevir
            val nowCalendar = Calendar.getInstance() // Calendar instance al
            nowCalendar.time = nowDate // Date'i Calendar'a ekle


            val msgDate =
                Date(contactViewModel.getLastMessageDate().seconds * 1000) // Timestamp saniyeye çevir
            val msgCalendar = Calendar.getInstance() // Calendar instance al
            msgCalendar.time = msgDate // Date'i Calendar'a ekle

            when (
                (nowCalendar.get(Calendar.YEAR) > msgCalendar.get(Calendar.YEAR)) ||
                        ((nowCalendar.get(Calendar.MONTH) + 1) > (msgCalendar.get(Calendar.MONTH) + 1)) ||
                        (nowCalendar.get(Calendar.DAY_OF_MONTH) > msgCalendar.get(Calendar.DAY_OF_MONTH))
            ) {
                true -> {
                    sendMessage()
                    contactViewModel.setLastOperation(null)
                }
                else -> {
                    logSnackbar(tag, R.string.msg_error_new, "error", goFeed = true)
                    contactViewModel.setLastOperation(null)
                }
            }

        }

    }


    private fun checkContext(context: Context.() -> Unit) { if (isAdded && context != null) { context(requireContext()) } }


}