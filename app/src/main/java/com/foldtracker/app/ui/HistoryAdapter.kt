package com.foldtracker.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.foldtracker.app.R
import com.foldtracker.app.data.DateUtils
import com.foldtracker.app.data.UnfoldEvent

class HistoryAdapter(private var items: List<UnfoldEvent>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.item_date)
        val time: TextView = view.findViewById(R.id.item_time)
        val duration: TextView = view.findViewById(R.id.item_duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = items[position]
        holder.date.text = DateUtils.formatDate(event.timestamp)
        holder.time.text = DateUtils.formatTime(event.timestamp)
        holder.duration.text = if (event.durationMillis != null) {
            DateUtils.formatDuration(event.durationMillis)
        } else {
            holder.itemView.context.getString(R.string.history_open_only)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<UnfoldEvent>) {
        items = newItems
        notifyDataSetChanged()
    }
}
