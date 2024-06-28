package com.basesoftware.fikirzadem.presentation.adapter

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.RowSocialDesignBinding
import com.basesoftware.fikirzadem.model.SocialModel
import com.basesoftware.fikirzadem.model.UserModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.dataAvailable
import com.basesoftware.fikirzadem.util.WorkUtil.changeFragment
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.message
import com.basesoftware.fikirzadem.util.ExtensionUtil.safePosition
import com.basesoftware.fikirzadem.util.ExtensionUtil.settings
import com.basesoftware.fikirzadem.util.ExtensionUtil.toUserModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList

class SocialRecyclerAdapter constructor(
    private var sharedViewModel : SharedViewModel,
    private var dialog : BottomSheetDialog,
    private val navController : NavController,
    private val argumentUserId : String
    ) : RecyclerView.Adapter<SocialRecyclerAdapter.RecyclerHolder>() {

    private var socialList : ArrayList<SocialModel> = arrayListOf()
    private var socialUserList : ArrayList<String> = arrayListOf()
    private var deleteFollowerList : ArrayList<String> = arrayListOf()
    private val firestore = WorkUtil.firestore()


    class SocialUtil(private val oldList : ArrayList<SocialModel>, private val newList : ArrayList<SocialModel>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(old: Int, new: Int): Boolean = oldList[old].userId.matches(Regex(newList[new].userId))

        override fun areContentsTheSame(old: Int, new: Int): Boolean = oldList[old].userId.matches(Regex(newList[new].userId))

    }

    class RecyclerHolder(val binding : RowSocialDesignBinding) : RecyclerView.ViewHolder(binding.root)



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        val binding = RowSocialDesignBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        val holder = RecyclerHolder(binding)

        val listener  = View.OnClickListener {

            holder.binding.deletedUser?.let {

                if(!it) {
                    if(dialog.isShowing) { dialog.dismiss() }
                    val argument = Bundle()
                    argument.putString("userId",socialList[holder.bindingAdapterPosition.safePosition()].userId)
                    changeFragment(navController, argument, R.id.profileFragment)
                }

            }

        }

        holder.binding.shared = sharedViewModel
        holder.itemView.setOnClickListener(listener)
        holder.binding.btnSocialProfile.setOnClickListener(listener)

        return holder
    }

    override fun getItemCount(): Int { return socialList.size }

    override fun onBindViewHolder(holder: RecyclerHolder, position: Int) {

        when (socialList[position].userId == sharedViewModel.getMyUserId()) {

            true -> {
                writeProfile(holder, sharedViewModel.getUser()!!)

                Log.i("SocialAdaptrBindHolder", "Kullanıcı viewmodeldan alındı")

            }

            false -> when(socialUserList.contains(socialList[position].userId)) {

                true -> checkCache(holder, socialList[position])

                else -> downloadUserDataFromServer(holder, socialList[position])

            }

        }

    }


    fun setData(newSocialList : ArrayList<SocialModel>) {

        val utilObj = SocialUtil(socialList, newSocialList)
        val utilResult = DiffUtil.calculateDiff(utilObj)
        socialList.clear()
        socialList.addAll(newSocialList)
        utilResult.dispatchUpdatesTo(this@SocialRecyclerAdapter)

    }


    private fun deletedProfile(holder: RecyclerHolder) { CoroutineScope(Dispatchers.Main).launch { holder.binding.deletedUser = true } }

    private fun writeProfile(holder: RecyclerHolder, userModel : UserModel) {
        CoroutineScope(Dispatchers.Main).launch {
            holder.binding.apply {

                deletedUser = false

                user = userModel

            }
        }
    }


    private fun checkCache(holder: RecyclerHolder, socialUser: SocialModel) {

        CoroutineScope(Dispatchers.IO).launch {

            val tagCheckCache = "SocialAdapterCheckCache"

            firestore
                .collection("Users")
                .document(socialUser.userId)
                .get(Source.CACHE)
                .addOnCompleteListener {
                    when(it.dataAvailable()) {
                        true -> {

                            Log.e(tagCheckCache, "Kullanıcı önbellekten alınıyor")
                            writeProfile(
                                holder,
                                it.result.toUserModel()
                            )

                        }
                        else -> {
                            // Kullanıcı önbellekte bulunamadı firestore gidiyor
                            Log.i(tagCheckCache, "Kullanıcı önbellekten alınamadı, Firebase gidiliyor")
                            socialUserList.remove(socialUser.userId)
                            downloadUserDataFromServer(holder, socialUser)
                        }
                    }
                }

        }

    }

    @SuppressLint("LogConditional")
    private fun downloadUserDataFromServer(holder: RecyclerHolder, socialUser: SocialModel) {

        CoroutineScope(Dispatchers.IO).launch {

            val tagGoFirebase = "SocialAdapterGoFirebase"

            firestore
                .collection("Users")
                .document(socialUser.userId)
                .get(Source.SERVER)
                .addOnSuccessListener { user ->

                    when(user.exists()) {
                        true -> {

                            // Kullanıcı firebasede mevcut
                            Log.i(tagGoFirebase, "Kullanıcı önbelleğe kaydedildi")

                            socialUserList.add(socialUser.userId)

                            writeProfile(holder, user.toUserModel())

                        }
                        false -> {

                            if(deleteFollowerList.indexOf(socialUser.userId) == -1) {

                                /**
                                 * Kullanıcı sosyal tablodan silindi
                                 * {UserFollower} veya {UserFollowing} tablosu
                                 */
                                firestore
                                    .collection("Users")
                                    .document(argumentUserId)
                                    .collection("UserFollower")
                                    .document(socialUser.userId)
                                    .delete()
                                    .addOnCompleteListener {
                                        deleteFollowerList.add(socialUser.userId)
                                        socialUserList.remove(socialUser.userId)
                                        when(it.isSuccessful) {
                                            true -> Log.i(tagGoFirebase, "Kullanıcı silinmiş, profildeki sosyal tablodan da silindi")
                                            else -> {
                                                Log.e(tagGoFirebase,"Kullanıcı silinmiş ama profildeki sosyal tablodan silinemedi")
                                                it.exception?.msg()?.let { msg -> Log.e(tagGoFirebase, msg) }
                                            }
                                        }
                                    }

                            }

                            // Kullanıcı firebasede mevcut değil
                            Log.i(tagGoFirebase, holder.itemView.context.getString(R.string.user_deleted))

                            deletedProfile(holder)
                        }
                    }


                }
                .addOnFailureListener {

                    CoroutineScope(Dispatchers.Main).launch {
                        Log.e(tagGoFirebase, holder.itemView.context.getString(R.string.error))
                        Snackbar
                            .make(dialog.window!!.decorView, holder.itemView.context.getString(R.string.error), Snackbar.LENGTH_SHORT)
                            .settings().widthSettings().setGestureInsetBottomIgnored(true)
                            .setAnchorView(dialog.window!!.decorView.findViewById(R.id.viewSocialSnackbar))
                            .show()
                        Log.e(tagGoFirebase, "Kullanıcı firebaseden alınamadı")
                        Log.e(tagGoFirebase, it.msg())
                        deletedProfile(holder)
                    }

                }

        }

    }

}