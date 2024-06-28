package com.basesoftware.fikirzadem.presentation.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.fikirzadem.databinding.RowContactDesignBinding
import com.basesoftware.fikirzadem.presentation.viewmodel.AdminViewModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.message
import com.basesoftware.fikirzadem.util.ExtensionUtil.questionSnackbar
import com.basesoftware.fikirzadem.util.ExtensionUtil.safePosition
import com.basesoftware.fikirzadem.util.ExtensionUtil.toContactModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactUsRecyclerAdapter(
    private val adminViewModel: AdminViewModel,
    private val view : View) : RecyclerView.Adapter<ContactUsRecyclerAdapter.ContactRecyclerHolder>() {

    private val firestore = WorkUtil.firestore()

    class ContactRecyclerHolder (var binding: RowContactDesignBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactRecyclerHolder {
        val binding = RowContactDesignBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        val holder = ContactRecyclerHolder(binding)

        holder.binding.apply {

            imgContactUserId.setOnClickListener { adminViewModel.setReviewUser(txtContactUserId.text.toString()) }

            btnMessageFeedBack.setOnLongClickListener {

                Snackbar
                    .make(root.context, view, "Mesaj ulaştı bildirimi gönder ?", Snackbar.LENGTH_LONG)
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setBackgroundTint(WorkUtil.snackbarColor(root.context))
                    .setTextColor(Color.LTGRAY)
                    .setGestureInsetBottomIgnored(true)
                    .questionSnackbar()
                    .setAction("EVET") {

                        val contactUser = holder.binding.txtContactUserId.text.toString()

                        val updateData = mutableMapOf <String, Any?> ()

                        updateData["userAdminMessageTr"] = "Mesajın tarafımıza ulaştı :)"

                        updateData["userAdminMessageEn"] = "Your message has reached us :)"

                        updateData["userAdminMessageDate"] = FieldValue.serverTimestamp()

                        CoroutineScope(Dispatchers.IO).launch {

                            firestore
                                .collection("Users")
                                .document(contactUser)
                                .update(updateData)
                                .addOnCompleteListener { feedBack ->
                                    when(feedBack.isSuccessful) {
                                        true -> logsnackbar(holder, "Admin mesajı iletildi", "info")
                                        else -> logsnackbar(holder, "Admin mesajı iletilemedi", "error")
                                    }
                                }

                        }

                    }
                    .setActionTextColor(Color.GREEN)
                    .widthSettings()
                    .show()



                return@setOnLongClickListener false
            }

            btnMessageSpamFeedBack.setOnLongClickListener {

                Snackbar
                    .make(root.context, view, "Spam uyarısı bildirimi gönder ?", Snackbar.LENGTH_LONG)
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setBackgroundTint(WorkUtil.snackbarColor(root.context))
                    .setTextColor(Color.LTGRAY)
                    .setGestureInsetBottomIgnored(true)
                    .questionSnackbar()
                    .setAction("EVET") {

                        val contactUser = holder.binding.txtContactUserId.text.toString()

                        val updateData = mutableMapOf <String, Any?> ()

                        updateData["userAdminMessageTr"] = "Lütfen Bize Ulaşın kısmını spam için kullanmayın"

                        updateData["userAdminMessageEn"] = "Please do not use the Contact Us section for spam"

                        updateData["userAdminMessageDate"] = FieldValue.serverTimestamp()

                        updateData["userSpamContact"] = FieldValue.increment(1)

                        CoroutineScope(Dispatchers.IO).launch {

                            firestore
                                .collection("Users")
                                .document(contactUser)
                                .update(updateData)
                                .addOnCompleteListener { feedBack ->
                                    when(feedBack.isSuccessful) {
                                        true -> logsnackbar(holder, "Admin mesajı iletildi", "info")
                                        else -> logsnackbar(holder, "Admin mesajı iletilemedi", "error")
                                    }
                                }

                        }

                    }
                    .setActionTextColor(Color.RED)
                    .widthSettings()
                    .show()



                return@setOnLongClickListener false
            }

            btnMessageDelete.setOnLongClickListener {

                Snackbar
                    .make(root.context, view, "Contact silinecek ?", Snackbar.LENGTH_LONG)
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setBackgroundTint(WorkUtil.snackbarColor(root.context))
                    .setTextColor(Color.LTGRAY)
                    .setGestureInsetBottomIgnored(true)
                    .questionSnackbar()
                    .setAction("EVET") {

                        adminViewModel.getContactList().apply { remove(get(holder.bindingAdapterPosition.safePosition())) }

                        deleteContact(holder)

                    }
                    .setActionTextColor(Color.RED)
                    .widthSettings()
                    .show()

                return@setOnLongClickListener true

            }

        }

        return holder
    }

    override fun getItemCount() : Int { return adminViewModel.getContactList().size }

    override fun onBindViewHolder(holder: ContactRecyclerHolder, position: Int) {

        holder.binding.contact = adminViewModel.getContactList()[holder.bindingAdapterPosition.safePosition()]

    }

    private fun logsnackbar(holder: ContactRecyclerHolder, msg : String, type : String) {

        CoroutineScope(Dispatchers.Main).launch {

            if (type.matches(Regex("info"))) Log.i("ContactAdapter", msg)
            else Log.e("ContactAdapter", msg)

            Snackbar
                .make(holder.binding.root.context, view, msg, Snackbar.LENGTH_SHORT)
                .setDuration(1150)
                .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                .setBackgroundTint(WorkUtil.snackbarColor(holder.binding.root.context))
                .setTextColor(
                    if (type.matches(Regex("info"))) Color.GREEN
                    else Color.RED
                )
                .setGestureInsetBottomIgnored(true)
                .widthSettings()
                .show()

        }

    }

    private fun deleteContact(holder : ContactRecyclerHolder) {

        CoroutineScope(Dispatchers.IO).launch {

            firestore
                .collection("Contact")
                .document(holder.binding.txtContactId.text.toString())
                .delete()
                .addOnFailureListener {
                    logsnackbar(holder, "Contact silinemedi", "error")
                    logsnackbar(holder, it.msg(), "error")
                }
                .addOnSuccessListener {

                    try {

                        this@ContactUsRecyclerAdapter.apply {

                            notifyItemRemoved(holder.bindingAdapterPosition.safePosition())

                            holder.bindingAdapter?.itemCount?.let { count ->
                                notifyItemRangeChanged(holder.bindingAdapterPosition.safePosition(), count)
                            }

                        }

                    }
                    catch (e : Exception) {
                        logsnackbar(holder, "Contact recycler delete contact error", "error")
                        logsnackbar(holder, e.msg(), "error")
                    }

                    getNewContact()

                    logsnackbar(holder, "Contact silindi", "info")

                }

        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getNewContact() {

        CoroutineScope(Dispatchers.IO).launch {

            firestore
                .collection("Contact")
                .orderBy("contactDate", Query.Direction.ASCENDING)
                .limit(1)
                .get(Source.SERVER)
                .addOnSuccessListener { query ->

                    if (!query.documents.isNullOrEmpty()) {

                        query.documents[0]?.let {
                            adminViewModel.setContact(it.toContactModel())
                            this@ContactUsRecyclerAdapter.notifyDataSetChanged()
                        }

                    }

                }

        }

    }

}