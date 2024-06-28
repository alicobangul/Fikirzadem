package com.basesoftware.fikirzadem.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.fikirzadem.databinding.RowNotificationDesignBinding
import com.basesoftware.fikirzadem.model.recycler.NotificationRecyclerModel
import com.basesoftware.fikirzadem.util.ExtensionUtil.safePosition

class NotificationRecyclerAdapter(var list : ArrayList<NotificationRecyclerModel>) : RecyclerView.Adapter<NotificationRecyclerAdapter.RecyclerHolder>() {

    class RecyclerHolder(val binding : RowNotificationDesignBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        val binding = RowNotificationDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecyclerHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerHolder, position: Int) {

        holder.binding.notification = list[(list.size - 1) - holder.bindingAdapterPosition.safePosition()]

    }

    override fun getItemCount(): Int = list.size

}