package com.basesoftware.fikirzadem.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basesoftware.fikirzadem.model.ContactModel
import com.basesoftware.fikirzadem.model.ReportModel

class AdminViewModel(private val state: SavedStateHandle) : ViewModel() {

    private var _contactList : MutableLiveData<ArrayList<ContactModel>> = state.getLiveData("_contactList", arrayListOf())

    private var _reportList : MutableLiveData<ArrayList<ReportModel>> = state.getLiveData("_reportList", arrayListOf())

    fun getContactList() : ArrayList<ContactModel> = _contactList.value!!
    fun setContact(value : ContactModel) = state.getLiveData<ArrayList<ContactModel>>("_contactList", arrayListOf()).value?.add(value)

    fun getReportList() : ArrayList<ReportModel> = _reportList.value!!
    fun setReport(value : ReportModel) = state.getLiveData<ArrayList<ReportModel>>("_reportList", arrayListOf()).value?.add(value)


    fun getReviewUser() : MutableLiveData<String> = state.getLiveData("_reviewUser")
    fun setReviewUser(user : String) { getReviewUser().value = user }

    fun getReviewPost() : MutableLiveData<String> = state.getLiveData("_reviewPost")
    fun setReviewPost(post : String) { getReviewPost().value = post }

    fun getReviewComment() : MutableLiveData<MutableMap<String, String>> = state.getLiveData("_reviewComment")
    fun setReviewComment(comment : MutableMap<String, String>) { getReviewComment().value = comment }

}