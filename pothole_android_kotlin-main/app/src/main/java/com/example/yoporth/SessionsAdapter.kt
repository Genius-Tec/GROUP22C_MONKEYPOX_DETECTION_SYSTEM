package com.example.yoporth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SessionsAdapter(
    private var sessions: List<DetectionSession>,
    private val onItemClick: (DetectionSession) -> Unit
) : RecyclerView.Adapter<SessionsAdapter.SessionViewHolder>() {

    class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewLocation: TextView = view.findViewById(R.id.textViewSessionLocation)
        val textViewDateTime: TextView = view.findViewById(R.id.textViewSessionDateTime)
        val textViewDistance: TextView = view.findViewById(R.id.textViewSessionDistance)
        val textViewDetections: TextView = view.findViewById(R.id.textViewSessionPotholes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        holder.textViewLocation.text = session.locationName
        holder.textViewDateTime.text = "${session.date}\n${session.time}"
        holder.textViewDistance.text = session.distance
        holder.textViewDetections.text = session.totalDetections.toString()
        
        holder.itemView.setOnClickListener {
            onItemClick(session)
        }
    }

    override fun getItemCount() = sessions.size

    fun updateData(newSessions: List<DetectionSession>) {
        sessions = newSessions
        notifyDataSetChanged()
    }
}