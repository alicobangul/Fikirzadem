package com.basesoftware.fikirzadem.presentation.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.RowSearchDesignBinding
import com.basesoftware.fikirzadem.model.recycler.SearchRecyclerModel
import com.basesoftware.fikirzadem.util.WorkUtil.changeFragment
import com.basesoftware.fikirzadem.util.ExtensionUtil.itemAnimation
import com.basesoftware.fikirzadem.util.ExtensionUtil.safePosition
import com.basesoftware.fikirzadem.presentation.viewmodel.SharedViewModel

class SearchRecyclerAdapter constructor(
    private val sharedViewModel : SharedViewModel
) : RecyclerView.Adapter<SearchRecyclerAdapter.RecyclerHolder>() {

    private var searchList : ArrayList<SearchRecyclerModel> = arrayListOf()

    private lateinit var recycler : RecyclerView

    class SearchUtil(private val oldList : ArrayList<SearchRecyclerModel>, private val newList : ArrayList<SearchRecyclerModel>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(old: Int, new: Int): Boolean = oldList[old].userId.matches(Regex(newList[new].userId))

        override fun areContentsTheSame(old: Int, new: Int): Boolean = oldList[old].userId.matches(Regex(newList[new].userId))

    }

    class RecyclerHolder(val binding : RowSearchDesignBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        recycler = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        val binding = RowSearchDesignBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        val holder = RecyclerHolder(binding)

        val listener  = View.OnClickListener {
            val argument = Bundle()
            argument.putString("userId", searchList[holder.bindingAdapterPosition.safePosition()].userId)
            changeFragment(Navigation.findNavController(recycler), argument, R.id.profileFragment)
        }

        holder.binding.apply {

            shared = sharedViewModel

            btnSearchProfile.setOnClickListener(listener)
            imgSearchProfilePicture.setOnClickListener(listener)

        }

        return holder
    }

    override fun getItemCount(): Int { return searchList.size }

    override fun onBindViewHolder(holder: RecyclerHolder, position: Int) {

        holder.apply {

            binding.apply {

                user = searchList[holder.bindingAdapterPosition.safePosition()]

                dataFromServer = searchList[holder.bindingAdapterPosition.safePosition()].sourceServer

            }

            itemView.itemAnimation(sharedViewModel.getAnimation())

        }

    }

    fun setData(newSearchList : ArrayList<SearchRecyclerModel>) {

        val utilObj = SearchUtil(searchList, newSearchList)
        val utilResult = DiffUtil.calculateDiff(utilObj)
        searchList.clear()
        searchList.addAll(newSearchList)
        utilResult.dispatchUpdatesTo(this@SearchRecyclerAdapter)

    }

}