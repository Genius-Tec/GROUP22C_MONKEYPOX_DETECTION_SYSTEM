package com.example.yoporth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class ReportsAdapter(private var reports: List<DetectionLocation>) :
    RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewDate: TextView = view.findViewById(R.id.textViewDate)
        val imageViewDetection: ImageView = view.findViewById(R.id.imageViewDetection)
        val imageViewMapSnippet: ImageView = view.findViewById(R.id.imageViewMapSnippet)
        val textViewLocation: TextView = view.findViewById(R.id.textViewLocation)
        val textViewLabel: TextView = view.findViewById(R.id.textViewLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        holder.textViewDate.text = report.formattedDate
        holder.textViewLocation.text = "Location: ${String.format("%.4f", report.latitude)}, ${String.format("%.4f", report.longitude)}"
        holder.textViewLabel.text = "Type: ${report.label}"

        // Load detection image
        if (report.imagePath != null) {
            val imgFile = File(report.imagePath)
            if (imgFile.exists()) {
                Glide.with(holder.itemView.context)
                    .load(imgFile)
                    .into(holder.imageViewDetection)
            }
        }

        // For the map snippet, we can use Google Static Maps API or just a placeholder 
        // since setting up dynamic maps in a RecyclerView is complex.
        val staticMapUrl = "https://maps.googleapis.com/maps/api/staticmap?center=${report.latitude},${report.longitude}&zoom=15&size=400x400&markers=color:red%7C${report.latitude},${report.longitude}&key=AIzaSyDmAB83modWvvMrC9zEA9YPfhQ-fBf65Gs"
        
        Glide.with(holder.itemView.context)
            .load(staticMapUrl)
            .placeholder(android.R.drawable.ic_menu_mapmode)
            .into(holder.imageViewMapSnippet)
    }

    override fun getItemCount() = reports.size

    fun updateData(newReports: List<DetectionLocation>) {
        reports = newReports
        notifyDataSetChanged()
    }
}