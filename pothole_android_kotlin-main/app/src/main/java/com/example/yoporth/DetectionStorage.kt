package com.example.yoporth

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DetectionLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val label: String,
    val imagePath: String? = null,
    val sessionId: String? = null,
    val boxCount: Int = 1,
    val formattedDate: String = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
) {
    val latLng: LatLng get() = LatLng(latitude, longitude)
}

data class DetectionSession(
    val id: String,
    val date: String,
    val time: String,
    val distance: String,
    val totalDetections: Int,
    val locationName: String,
    val timestamp: Long,
    val startTimestamp: Long = 0L
)

object DetectionStorage {
    private const val PREFS_NAME = "monkeypox_detections"
    private const val KEY_DETECTIONS = "detections_list"
    private const val KEY_SESSIONS = "sessions_list"
    private val gson = Gson()
    private val detections = mutableListOf<DetectionLocation>()
    private val sessions = mutableListOf<DetectionSession>()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val detectionsJson = prefs.getString(KEY_DETECTIONS, null)
        if (detectionsJson != null) {
            val type = object : TypeToken<List<DetectionLocation>>() {}.type
            val savedDetections: List<DetectionLocation> = gson.fromJson(detectionsJson, type)
            detections.clear()
            detections.addAll(savedDetections)
        }

        val sessionsJson = prefs.getString(KEY_SESSIONS, null)
        if (sessionsJson != null) {
            val type = object : TypeToken<List<DetectionSession>>() {}.type
            val savedSessions: List<DetectionSession> = gson.fromJson(sessionsJson, type)
            sessions.clear()
            sessions.addAll(savedSessions)
        }
    }

    fun addDetection(context: Context, detection: DetectionLocation) {
        detections.add(detection)
        saveDetections(context)
    }

    fun addSession(context: Context, session: DetectionSession) {
        sessions.add(0, session) // Add to top
        saveSessions(context)
    }

    fun deleteSession(context: Context, sessionId: String) {
        sessions.removeAll { it.id == sessionId }
        detections.removeAll { it.sessionId == sessionId }
        saveSessions(context)
        saveDetections(context)
    }

    fun getDetections(): List<DetectionLocation> {
        return detections
    }

    fun getDetectionsForSession(sessionId: String): List<DetectionLocation> {
        return detections.filter { it.sessionId == sessionId }
    }

    fun getSessions(): List<DetectionSession> {
        return sessions
    }

    fun clearDetections(context: Context) {
        detections.clear()
        saveDetections(context)
    }

    private fun saveDetections(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(detections)
        prefs.edit().putString(KEY_DETECTIONS, json).apply()
    }

    private fun saveSessions(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(sessions)
        prefs.edit().putString(KEY_SESSIONS, json).apply()
    }
}