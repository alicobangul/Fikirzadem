package com.basesoftware.fikirzadem.presentation.adapter

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.RowReportDesignBinding
import com.basesoftware.fikirzadem.presentation.viewmodel.AdminViewModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.message
import com.basesoftware.fikirzadem.util.ExtensionUtil.questionSnackbar
import com.basesoftware.fikirzadem.util.ExtensionUtil.safePosition
import com.basesoftware.fikirzadem.util.ExtensionUtil.toReportModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReportRecyclerAdapter(
    private var adminViewModel : AdminViewModel,
    private val view : View) : RecyclerView.Adapter<ReportRecyclerAdapter.ReportRecyclerHolder>() {

    private val firestore = WorkUtil.firestore()

    private lateinit var recycler : RecyclerView

    class ReportRecyclerHolder (val binding : RowReportDesignBinding) : RecyclerView.ViewHolder (binding.root)


    override fun getItemCount(): Int { return adminViewModel.getReportList().size }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportRecyclerHolder {

        val binding = RowReportDesignBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        val holder = ReportRecyclerHolder(binding)

        holder.binding.apply {

            imgReporterId.setOnClickListener { adminViewModel.setReviewUser(txtReporterId.text.toString()) }

            imgContentId.setOnClickListener {

                when(adminViewModel.getReportList()[holder.bindingAdapterPosition.safePosition()].reportType) {

                    "user" -> adminViewModel.setReviewUser(txtContentId.text.toString())
                    else -> adminViewModel.setReviewPost(txtContentId.text.toString())
                }

            }

            imgContentExtraId.setOnClickListener {
                adminViewModel.setReviewComment(
                    mutableMapOf("postId" to txtContentId.text.toString(), "actionUserId" to txtContentExtraId.text.toString())
                )
            }

            btnTrueReportMessage.setOnLongClickListener {

                Snackbar
                    .make(root.context, view, "Doğru rapor bilgisi ilet ?", Snackbar.LENGTH_LONG)
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setBackgroundTint(WorkUtil.snackbarColor(root.context))
                    .setTextColor(Color.LTGRAY)
                    .setGestureInsetBottomIgnored(true)
                    .questionSnackbar()
                    .setAction("EVET") {

                        val reporter = holder.binding.txtReporterId.text.toString()

                        val updateData = mutableMapOf <String, Any?> ()

                        updateData["userAdminMessageTr"] = "Doğru rapor için teşekkürler :)"

                        updateData["userAdminMessageEn"] = "Your message has reached us :)"

                        updateData["userAdminMessageDate"] = FieldValue.serverTimestamp()

                        CoroutineScope(Dispatchers.IO).launch {

                            firestore
                                .collection("Users")
                                .document(reporter)
                                .update(updateData)
                                .addOnCompleteListener { feedBack ->
                                    when(feedBack.isSuccessful) {
                                        true -> logsnackbar(holder, "Doğru rapor bilgisi iletildi", "info")
                                        else -> logsnackbar(holder, "Doğru rapor bilgisi iletilemedi", "error")
                                    }
                                }

                        }

                    }
                    .setActionTextColor(Color.GREEN)
                    .widthSettings()
                    .show()

                return@setOnLongClickListener false
            }

            btnWrongReportMessage.setOnLongClickListener {

                Snackbar
                    .make(root.context, view, "Yanlış rapor bilgisi ilet ?", Snackbar.LENGTH_LONG)
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setBackgroundTint(WorkUtil.snackbarColor(root.context))
                    .setTextColor(Color.LTGRAY)
                    .setGestureInsetBottomIgnored(true)
                    .questionSnackbar()
                    .setAction("EVET") {

                        val reporter = holder.binding.txtReporterId.text.toString()

                        val updateData = mutableMapOf <String, Any?> ()

                        updateData["userAdminMessageTr"] = "Yanlış raporlama gerçekleştirdin"

                        updateData["userAdminMessageEn"] = "You sent the wrong report"

                        updateData["userAdminMessageDate"] = FieldValue.serverTimestamp()

                        updateData["userSpamReport"] = FieldValue.increment(1)

                        CoroutineScope(Dispatchers.IO).launch {

                            firestore
                                .collection("Users")
                                .document(reporter)
                                .update(updateData)
                                .addOnCompleteListener { feedBack ->
                                    when(feedBack.isSuccessful) {
                                        true -> logsnackbar(holder, "Yanlış rapor bilgisi iletildi", "info")
                                        else -> logsnackbar(holder, "Yanlış rapor bilgisi iletilemedi", "error")
                                    }
                                }

                        }

                    }
                    .setActionTextColor(Color.RED)
                    .widthSettings()
                    .show()

                return@setOnLongClickListener false
            }

            btnLateReportMessage.setOnLongClickListener {

                Snackbar
                    .make(root.context, view, "Daha önce incelendiğini ilet ?", Snackbar.LENGTH_LONG)
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setBackgroundTint(WorkUtil.snackbarColor(root.context))
                    .setTextColor(Color.LTGRAY)
                    .setGestureInsetBottomIgnored(true)
                    .questionSnackbar()
                    .setAction("EVET") {

                        val reporter = holder.binding.txtReporterId.text.toString()

                        val updateData = mutableMapOf <String, Any?> ()

                        updateData["userAdminMessageTr"] = "Gönderdiğin rapor daha önce incelendi"

                        updateData["userAdminMessageEn"] = "The report you sent has already been reviewed"

                        updateData["userAdminMessageDate"] = FieldValue.serverTimestamp()

                        CoroutineScope(Dispatchers.IO).launch {

                            firestore
                                .collection("Users")
                                .document(reporter)
                                .update(updateData)
                                .addOnCompleteListener { feedBack ->
                                    when(feedBack.isSuccessful) {
                                        true -> logsnackbar(holder, "Daha önce incelendiği iletildi", "info")
                                        else -> logsnackbar(holder, "Daha önce incelendiği iletilemedi", "error")
                                    }
                                }

                        }

                    }
                    .setActionTextColor(Color.GREEN)
                    .widthSettings()
                    .show()

                return@setOnLongClickListener false
            }

            btnReportDelete.setOnLongClickListener {

                Snackbar
                    .make(root.context, view, "Rapor silinecek ?", Snackbar.LENGTH_LONG)
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setBackgroundTint(WorkUtil.snackbarColor(root.context))
                    .setTextColor(Color.LTGRAY)
                    .setGestureInsetBottomIgnored(true)
                    .questionSnackbar()
                    .setAction("EVET") {

                        adminViewModel.getReportList().apply { remove(get(holder.bindingAdapterPosition.safePosition())) }

                        deleteReport(holder)

                    }
                    .setActionTextColor(Color.RED)
                    .widthSettings()
                    .show()


                return@setOnLongClickListener true
            }

            imgContentDetail.setOnClickListener {

                try {

                    val dialog = Dialog(recycler.context)

                    dialog.setContentView(R.layout.profile_picture_dialog)

                    Glide
                        .with(recycler.context)
                        .setDefaultRequestOptions(WorkUtil.glideDefault(recycler.context))
                        .load(txtReportContentDetail.text.toString())
                        .into(dialog.findViewById(R.id.imgProfilePictureOrg))

                    dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


                    dialog.findViewById<ConstraintLayout>(R.id.constraintPpDialog).isSoundEffectsEnabled = false

                    dialog.findViewById<ConstraintLayout>(R.id.constraintPpDialog)
                        .setOnClickListener { dialog.dismiss() }

                    dialog.show()

                }
                catch (e : Exception) { Log.e("ReportRecycler-OpenPp","Resim dialog kutusu gösterilemedi") }

            }

        }

        return holder
    }

    override fun onBindViewHolder(holder: ReportRecyclerHolder, position: Int) {

        holder.binding.report = adminViewModel.getReportList()[holder.bindingAdapterPosition.safePosition()]

    }


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        recycler = recyclerView
    }


    private fun logsnackbar(holder: ReportRecyclerHolder, msg : String, type : String) {

        CoroutineScope(Dispatchers.Main).launch {

            if (type.matches(Regex("info"))) Log.i("ReportAdapter", msg)
            else Log.e("ReportAdapter", msg)

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

    private fun deleteReport(holder: ReportRecyclerHolder) {

        CoroutineScope(Dispatchers.IO).launch {

            firestore
                .collection("Reports")
                .document(holder.binding.report!!.reportId)
                .delete()
                .addOnFailureListener {
                    logsnackbar(holder, "Rapor silinemedi", "error")
                    logsnackbar(holder, it.msg(), "error")
                }
                .addOnSuccessListener {

                    try {

                        this@ReportRecyclerAdapter.apply {

                            notifyItemRemoved(holder.bindingAdapterPosition.safePosition())

                            holder.bindingAdapter?.itemCount?.let { count ->
                                notifyItemRangeChanged(holder.bindingAdapterPosition.safePosition(), count)
                            }

                        }

                    }
                    catch (e : Exception) {
                        logsnackbar(holder, "Report recycler delete report error", "error")
                        logsnackbar(holder, e.msg(), "error")
                    }

                    getNewReport()

                    logsnackbar(holder, "Rapor silindi", "info")

                }

        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getNewReport() {

        CoroutineScope(Dispatchers.IO).launch {

            firestore
                .collection("Reports")
                .orderBy("reportDate", Query.Direction.ASCENDING)
                .limit(1)
                .get(Source.SERVER)
                .addOnSuccessListener { query ->

                    if (!query.documents.isNullOrEmpty()) {

                        query.documents[0]?.let {
                            adminViewModel.setReport(it.toReportModel())
                            this@ReportRecyclerAdapter.notifyDataSetChanged()
                        }

                    }

                }

        }

    }

}