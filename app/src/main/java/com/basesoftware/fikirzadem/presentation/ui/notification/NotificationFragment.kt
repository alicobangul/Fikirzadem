package com.basesoftware.fikirzadem.presentation.ui.notification

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.presentation.adapter.NotificationRecyclerAdapter
import com.basesoftware.fikirzadem.databinding.FragmentNotificationBinding
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.WorkUtil.calculateDate
import com.basesoftware.fikirzadem.util.WorkUtil.changeFragment
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationFragment : Fragment() {

    private var _binding : FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore

    private val sharedViewModel : SharedViewModel by activityViewModels { SharedViewModel.provideFactory(requireActivity().application, requireActivity()) }


    override fun onCreateView(inf: LayoutInflater, cont: ViewGroup?, instance: Bundle?): View {

        _binding = FragmentNotificationBinding.inflate(layoutInflater, cont, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()

        checkCacheSystemMessage()

        writeAdminMessage()

    }

    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()

    }

    private fun checkContext(context: Context.() -> Unit) { if (isAdded) { context(requireContext()) } }



    private fun initialize() {

        firestore = WorkUtil.firestore()

        checkContext {

            if (sharedViewModel.getNotificationList().isNotEmpty()) {

                binding.recyclerNotification.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = NotificationRecyclerAdapter(sharedViewModel.getNotificationList())
                }

            }

        }


        binding.txtSystemMessageTime.setOnLongClickListener {
            changeFragment(findNavController(), null, R.id.adminFragment)
            return@setOnLongClickListener true
        }

    }



    private fun checkCacheSystemMessage() {

        CoroutineScope(Dispatchers.IO).launch {

            val tag = "NotificationFrag-Cache"
            val language = WorkUtil.systemLanguage() // tr or en

            firestore
                .collection("Notification")
                .document("Notification")
                .get(Source.CACHE)
                .addOnCompleteListener {

                    when(it.isSuccessful) {
                        true -> {

                            checkContext {

                                Log.i(tag, "Önbellekte admin mesajı mevcut")

                                CoroutineScope(Dispatchers.Main).launch {

                                    binding.txtSystemMessage.text = it.result.getString("message_$language") ?: "-"
                                    binding.txtSystemMessageTime.text =
                                        it.result.getTimestamp("messageDate")?.let { time ->
                                            calculateDate(
                                                requireContext(),
                                                time.seconds,
                                                "NotifFrag-Cache"
                                            )
                                        }
                                }

                            }

                            checkServerSystemMessage(it.result.getTimestamp("messageDate") ?: Timestamp.now())
                        }
                        else -> {
                            Log.e(tag, "Önbellekte admin mesajı yok")
                            it.exception?.msg()?.let { msg -> Log.e(tag, msg) }
                            checkServerSystemMessage(null)
                        }
                    }
                }

        }

    }

    private fun checkServerSystemMessage(time : Timestamp?) {

        CoroutineScope(Dispatchers.IO).launch {

            val tag = "NotificationFrag-Server"
            val language = WorkUtil.systemLanguage() // tr or en

            if(time == null) {

                firestore
                    .collection("Notification")
                    .document("Notification")
                    .get(Source.SERVER)
                    .addOnCompleteListener {

                        when (it.isSuccessful) {
                            true -> {

                                checkContext {

                                    Log.i(tag, "Firebaseden ilk admin mesajı indirildi")

                                    CoroutineScope(Dispatchers.Main).launch {
                                        binding.txtSystemMessage.text = it.result.getString("message_$language")
                                        binding.txtSystemMessageTime.text =
                                            it.result.getTimestamp("messageDate")?.let { time ->
                                                calculateDate(
                                                    requireContext(),
                                                    time.seconds,
                                                    "NotifFrag-Server"
                                                )
                                            }
                                    }

                                }

                            }
                            else -> {

                                Log.e(tag, "Firebasede ve önbellekte admin mesajı bulunamadı")
                                it.exception?.msg()?.let { msg -> Log.e(tag, msg) }

                            }
                        }

                    }

            }

            else {

                firestore
                    .collection("Notification")
                    .whereGreaterThan("messageDate", time)
                    .limit(1)
                    .get(Source.SERVER)
                    .addOnCompleteListener {

                        when (it.isSuccessful) {
                            true -> {

                                checkContext {

                                    if(it.result.documents.isNotEmpty()) {

                                        Log.i(tag, "Firebaseden yeni admin mesajı indirildi")

                                        CoroutineScope(Dispatchers.Main).launch {
                                            binding.txtSystemMessage.text = it.result.documents[0].getString("message_$language")
                                            binding.txtSystemMessageTime.text =
                                                it.result.documents[0].getTimestamp("messageDate")?.let { time ->
                                                    calculateDate(
                                                        requireContext(),
                                                        time.seconds,
                                                        "NotifFrag-Server"
                                                    )
                                                }
                                        }

                                    }

                                    else { Log.i(tag, "Firebasede yeni admin mesajı yok, önbellekteki kullanılıyor") }

                                }

                            }
                            else -> {

                                Log.e(tag, "Firebasede yeni admin mesajı yok, önbellekteki kullanılıyor")
                                it.exception?.msg()?.let { msg -> Log.e(tag, msg) }

                            }
                        }

                    }

            }

        }

    }


    private fun writeAdminMessage() {

        CoroutineScope(Dispatchers.Main).launch {

            sharedViewModel.getUser()?.let { user ->

                binding.txtAdminMessage.text = when (WorkUtil.systemLanguage().matches(Regex("tr"))) {
                    true -> user.userAdminMessageTr
                    else -> user.userAdminMessageEn
                }
                binding.txtAdminMessageTime.text = calculateDate(
                    requireContext(),
                    user.userAdminMessageDate!!.seconds,
                    "NotifFrag-Server"
                )

            }

        }

    }

}