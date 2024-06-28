package com.basesoftware.fikirzadem.presentation.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.RowFavoriteDesignBinding
import com.basesoftware.fikirzadem.data.local.dto.SavedPostModel
import com.basesoftware.fikirzadem.data.local.FikirzademDatabase
import com.basesoftware.fikirzadem.util.WorkUtil
import com.basesoftware.fikirzadem.util.ExtensionUtil.gone
import com.basesoftware.fikirzadem.util.ExtensionUtil.itemAnimation
import com.basesoftware.fikirzadem.util.ExtensionUtil.msg
import com.basesoftware.fikirzadem.util.ExtensionUtil.message
import com.basesoftware.fikirzadem.util.ExtensionUtil.questionSnackbar
import com.basesoftware.fikirzadem.util.ExtensionUtil.safePosition
import com.basesoftware.fikirzadem.util.ExtensionUtil.widthSettings
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class FavoriteRecyclerAdapter constructor(
    private val view : View,
    private val textToSpeech : TextToSpeech,
    private val shared : SharedViewModel,
    private val fragmentResume : MutableLiveData<Boolean>) :
    PagingDataAdapter<SavedPostModel, FavoriteRecyclerAdapter.FavoriteListViewHolder>(DIFF_CALLBACK) {

    private lateinit var recycler: RecyclerView

    private var selectedHolder by Delegates.notNull<FavoriteListViewHolder>()
    private var selectedModel by Delegates.notNull<SavedPostModel>()
    private var selectedPosition by Delegates.notNull<Int>()

    private lateinit var bottomSheetDialog : BottomSheetDialog
    private lateinit var bottomSheetView : View

    companion object {

        init { Log.i("FavoriteListAdapter", "DiffUtil oluşturuldu") }

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SavedPostModel>() {

            override fun areItemsTheSame(oldItem: SavedPostModel, newItem: SavedPostModel) = oldItem.saveDate == newItem.saveDate

            override fun areContentsTheSame(oldItem: SavedPostModel, newItem: SavedPostModel) = oldItem.saveDate == newItem.saveDate

        }

    }

    class FavoriteListViewHolder (val binding: RowFavoriteDesignBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        recycler = recyclerView

        bottomSheetDialog = BottomSheetDialog(recycler.context, R.style.BottomSheetDialogTheme)

        fragmentResume.observe(recycler.context as FragmentActivity) {
            if(!it) { if(bottomSheetDialog.isShowing) bottomSheetDialog.dismiss() }
        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteListViewHolder {
        val binding = RowFavoriteDesignBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        val holder = FavoriteListViewHolder(binding)

        holder.itemView.setOnLongClickListener(bottomSheetListener(holder))
        holder.binding.txtFavoriteContent.setOnLongClickListener(bottomSheetListener(holder))

        return holder
    }

    override fun onBindViewHolder(holder: FavoriteListViewHolder, position: Int) {

        holder.binding.post = getItem(position)

        holder.itemView.itemAnimation(shared.getAnimation())

    }

    private fun closeBottomSheet() { if(bottomSheetDialog.isShowing) bottomSheetDialog.dismiss() }

    private fun bottomSheetListener(holder: FavoriteListViewHolder) : View.OnLongClickListener {
        return View.OnLongClickListener {

            selectedHolder = holder
            selectedModel = getItem(holder.bindingAdapterPosition.safePosition())!!
            selectedPosition = holder.bindingAdapterPosition.safePosition()

            getItem(holder.bindingAdapterPosition.safePosition())?.let {  bottomSheet() }

            return@OnLongClickListener false
        }
    }

    @SuppressLint("InflateParams")
    private fun bottomSheet() {

        bottomSheetView = LayoutInflater
            .from(recycler.context.applicationContext)
            .inflate(R.layout.bottom_dialog_post_option,null)

        bottomSheetView.apply {

            findViewById<TextView>(R.id.txtSheetRefresh).gone()

            findViewById<TextView>(R.id.txtSheetFavorite).gone()

            findViewById<TextView>(R.id.txtSheetReport).gone()

            findViewById<TextView>(R.id.txtSheetListen).setOnClickListener {

                try {

                    textToSpeech.apply {

                        setSpeechRate(0.8f)

                        setPitch(0.9f)

                        speak(selectedModel.postContent, TextToSpeech.QUEUE_ADD, null, null)

                    }

                    Log.i("FavoriteAdptrPostListen", "İçerik okutma başarılı")
                }
                catch (e: Exception) { Log.e("FavoriteAdptrPostListen", e.msg()) }

                closeBottomSheet()

            }

            findViewById<TextView>(R.id.txtSheetDelete).setOnClickListener {

                Snackbar
                    .make(recycler.context, view, recycler.context.getString(R.string.post_delete_question), Snackbar.LENGTH_LONG)
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setBackgroundTint(WorkUtil.snackbarColor(recycler.context))
                    .setTextColor(Color.LTGRAY)
                    .setGestureInsetBottomIgnored(true)
                    .questionSnackbar()
                    .setAction(recycler.context.getString(R.string.yes)) {

                        CoroutineScope(Dispatchers.IO).launch {

                            val tag = "FavoriteListAdapter"

                            try {

                                val db = Room.databaseBuilder(recycler.context, FikirzademDatabase::class.java,"FikirzademDB").build()

                                selectedModel.postId?.let { id -> db.fikirzademDao().deleteSavedPostWithId(shared.getMyUserId(), id) }

                                Log.i(tag, "Favori post veritabanından silindi")

                                db.close()

                                withContext(Dispatchers.Main) {

                                    Snackbar
                                        .make(
                                            recycler.context,
                                            view,
                                            recycler.context.getString(R.string.post_delete_in_favorite_success),
                                            Snackbar.LENGTH_SHORT
                                        )
                                        .setDuration(1150)
                                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                        .setBackgroundTint(WorkUtil.snackbarColor(recycler.context))
                                        .setTextColor(Color.GREEN)
                                        .setGestureInsetBottomIgnored(true)
                                        .widthSettings()
                                        .show()

                                    selectedPosition.let { select ->
                                        this@FavoriteRecyclerAdapter.notifyItemRemoved(select)
                                        this@FavoriteRecyclerAdapter.refresh()
                                        selectedHolder.bindingAdapter?.itemCount?.let { count ->
                                            this@FavoriteRecyclerAdapter.notifyItemRangeChanged(select, count)
                                        }


                                    }
                                }

                            }
                            catch (e: Exception) {

                                withContext(Dispatchers.Main) {

                                    Snackbar
                                        .make(
                                            recycler.context,
                                            view,
                                            recycler.context.getString(R.string.post_delete_in_favorite_fail),
                                            Snackbar.LENGTH_SHORT
                                        )
                                        .setDuration(1150)
                                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                        .setBackgroundTint(WorkUtil.snackbarColor(recycler.context))
                                        .setTextColor(Color.RED)
                                        .setGestureInsetBottomIgnored(true)
                                        .widthSettings()
                                        .show()

                                    Log.e(tag, "Favori post veritabanından silinemedi")
                                    Log.e(tag, e.msg())
                                }

                            }
                        }

                    }
                    .setActionTextColor(Color.GREEN)
                    .widthSettings()
                    .show()



                closeBottomSheet()

            }

            findViewById<TextView>(R.id.txtSheetShare).setOnClickListener {

                closeBottomSheet()

                selectedPosition.let {
                    WorkUtil.openShareMenu(recycler.context, getItem(it)?.postContent ?: "-")
                }

            }

        }

        bottomSheetDialog.apply {

            setContentView(bottomSheetView) // View eklendi

            show() // BottomSheet açıldı

        }

    }

}